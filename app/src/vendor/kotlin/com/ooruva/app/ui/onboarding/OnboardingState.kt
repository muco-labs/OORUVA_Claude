package com.ooruva.app.ui.onboarding

import com.ooruva.app.data.remote.BusinessCategoryDto
import com.ooruva.app.data.remote.BusinessRequirementDto
import com.ooruva.app.data.remote.BusinessTypeDto
import com.ooruva.app.data.remote.CatalogueItemDto

/**
 * The steps a vendor walks through, in order.
 *
 * Declared as data rather than a stack of screens so progress can be persisted:
 * [OnboardingStep.key] is written to businesses.onboarding_step after each save,
 * and a vendor who loses signal halfway resumes where they stopped instead of
 * starting over. Small shops sign up on patchy mobile data; losing twenty
 * minutes of typing is how a platform loses a vendor for good.
 */
enum class OnboardingStep(val key: String, val title: String, val blurb: String) {
    Basics(
        "basics",
        "Your business",
        "The name customers already know you by."
    ),
    Category(
        "category",
        "What kind of business?",
        "This decides what else we need to ask you."
    ),
    Type(
        "type",
        "Narrow it down",
        "Pick the closest match. You can change this later."
    ),
    Details(
        "details",
        "Contact and description",
        "How customers reach you, and what you are known for."
    ),
    Location(
        "location",
        "Where are you?",
        "Customers find you on the map with this."
    ),
    Hours(
        "hours",
        "When are you open?",
        "Roughly is fine. You can refine it any time."
    ),
    Catalogue(
        "catalogue",
        "What do you sell?",
        "Add a few to start. Items, boxes, packages or services."
    ),
    Photos(
        "photos",
        "Photos",
        "One good photo of the shopfront does more than ten of anything else."
    ),
    Requirements(
        "requirements",
        "Registrations",
        "Only what applies to your kind of business."
    ),
    Terms(
        "terms",
        "Terms",
        "The short version, and the full text if you want it."
    ),
    Review(
        "review",
        "Check and submit",
        "Nothing is public until our team has looked at it."
    );

    companion object {
        fun fromKey(key: String?): OnboardingStep =
            entries.firstOrNull { it.key == key } ?: Basics
    }
}

/**
 * How a requirement is presented to the vendor.
 *
 * Mirrors business_requirements.applicability rather than collapsing it to a
 * boolean. The distinction that matters is [AskVendor]: the database says
 * `requires_review`, meaning nobody qualified has decided whether this
 * registration applies. OORUVA asks; it does not assert. Rendering that as
 * "Required" would be regulatory advice the platform has no basis to give.
 */
sealed interface RequirementPrompt {
    val key: String
    val label: String

    data class Mandatory(override val key: String, override val label: String) : RequirementPrompt
    data class Optional(override val key: String, override val label: String) : RequirementPrompt
    data class AskVendor(override val key: String, override val label: String) : RequirementPrompt
}

/** Human label for a requirement key. Unknown keys fall back to the key itself. */
fun requirementLabel(key: String): String = when (key.lowercase()) {
    "fssai" -> "FSSAI licence"
    "udyam" -> "Udyam registration"
    "gst" -> "GST registration"
    "trade_licence", "trade_license" -> "Trade licence"
    else -> key.replace('_', ' ').replaceFirstChar { it.uppercase() }
}

fun BusinessRequirementDto.toPrompt(): RequirementPrompt? {
    val label = requirementLabel(requirementKey)
    return when (applicability) {
        "required" -> RequirementPrompt.Mandatory(requirementKey, label)
        "optional" -> RequirementPrompt.Optional(requirementKey, label)
        "requires_review" -> RequirementPrompt.AskVendor(requirementKey, label)
        // not_applicable: do not show it at all. Asking a mobile repair shop
        // about a food licence wastes their time and erodes trust in the rest
        // of the form.
        else -> null
    }
}

/** A registration the vendor holds, and the file proving it. */
data class DocumentEntry(
    val requirementKey: String,
    val number: String = "",
    /**
     * Path in the private `documents` bucket. Null until a file has actually
     * finished uploading — a local filename would let the review screen imply
     * a document was submitted when nothing left the phone.
     */
    val storagePath: String? = null,
    val uploading: Boolean = false,
    val uploadError: String? = null,
    val declaredNotApplicable: Boolean = false,
) {
    val hasFile: Boolean get() = storagePath != null
}

/**
 * Everything onboarding has collected so far.
 *
 * One flat object because it is saved as one row. Splitting it per step would
 * mean partial writes and a draft that can be internally inconsistent.
 */
data class OnboardingDraft(
    val businessId: String? = null,
    val isExistingBusiness: Boolean? = null,
    val name: String = "",
    val ownerName: String = "",
    val categoryId: String? = null,
    val businessTypeId: String? = null,
    val description: String = "",
    val phone: String = "",
    val address: String = "",
    val district: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val openingHours: String = "",
    val catalogue: List<CatalogueItemDto> = emptyList(),
    /** Storage paths in the public `photos` bucket, in upload order. */
    val photoPaths: List<String> = emptyList(),
    val documents: Map<String, DocumentEntry> = emptyMap(),
    val termsAccepted: Boolean = false,
) {
    /**
     * Whether the current step has enough to move on.
     *
     * Location and photos are deliberately skippable. A vendor standing in
     * their shop with no GPS fix should not be stuck at step five; the listing
     * is reviewed by a human who can chase what is missing. Blocking here
     * loses the vendor, and a listing with a gap beats no listing at all.
     */
    fun canAdvance(step: OnboardingStep): Boolean = when (step) {
        OnboardingStep.Basics -> name.isNotBlank() && isExistingBusiness != null
        OnboardingStep.Category -> categoryId != null
        OnboardingStep.Type -> businessTypeId != null
        OnboardingStep.Details -> phone.length == 10 && address.isNotBlank()
        OnboardingStep.Location -> true
        OnboardingStep.Hours -> true
        OnboardingStep.Catalogue -> true
        OnboardingStep.Photos -> true
        OnboardingStep.Requirements -> true
        OnboardingStep.Terms -> termsAccepted
        OnboardingStep.Review -> termsAccepted && name.isNotBlank() && businessTypeId != null
    }

    /**
     * A rough completeness score, shown to the vendor and stored on the row so
     * an admin can triage a queue by how much there is to review.
     */
    fun completeness(): Int {
        val checks = listOf(
            name.isNotBlank(),
            ownerName.isNotBlank(),
            categoryId != null,
            businessTypeId != null,
            description.isNotBlank(),
            phone.length == 10,
            address.isNotBlank(),
            latitude != null && longitude != null,
            openingHours.isNotBlank(),
            catalogue.isNotEmpty(),
            photoPaths.isNotEmpty(),
            termsAccepted,
        )
        return (checks.count { it } * 100) / checks.size
    }
}

/** What the screen renders while talking to the backend. */
data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.Basics,
    val draft: OnboardingDraft = OnboardingDraft(),
    val categories: List<BusinessCategoryDto> = emptyList(),
    val types: List<BusinessTypeDto> = emptyList(),
    val prompts: List<RequirementPrompt> = emptyList(),
    val loading: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null,
    val submitted: Boolean = false,
    /**
     * True when the backend is unreachable and the draft lives only on this
     * device. The screen says so plainly rather than implying the work is safe.
     */
    val offlineDraft: Boolean = false,
)
