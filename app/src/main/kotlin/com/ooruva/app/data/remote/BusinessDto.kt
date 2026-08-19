package com.ooruva.app.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire shapes for the model migration 04 introduced and 07 completed:
 * business_categories -> business_types -> businesses, with requirements
 * attached to the type rather than decided in app code.
 *
 * These replace [VendorProfileDto], which speaks to the older `vendor_profiles`
 * table. Both exist for now so the migration can happen one caller at a time.
 */

@Serializable
data class BusinessCategoryDto(
    val id: String,
    val slug: String,
    val name: String,
    val description: String? = null,
    val icon: String? = null,
    @SerialName("sort_order") val sortOrder: Int = 100,
    val active: Boolean = true,
)

@Serializable
data class BusinessTypeDto(
    val id: String,
    @SerialName("category_id") val categoryId: String,
    val slug: String,
    val name: String,
    val description: String? = null,
    @SerialName("sort_order") val sortOrder: Int = 100,
    val active: Boolean = true,
)

/**
 * What a business type has to produce before it can be verified.
 *
 * [applicability] is one of required | optional | not_applicable |
 * requires_review. The default in the database is requires_review, and that is
 * load-bearing: OORUVA does not tell a vendor which registrations they legally
 * need until a qualified person has recorded a basis. The app must present
 * requires_review as a question, never as an instruction.
 */
@Serializable
data class BusinessRequirementDto(
    val id: String,
    @SerialName("business_type_id") val businessTypeId: String,
    @SerialName("requirement_key") val requirementKey: String,
    val applicability: String = "requires_review",
    @SerialName("basis_note") val basisNote: String? = null,
) {
    val isSettled: Boolean get() = applicability != "requires_review"
    val isRequired: Boolean get() = applicability == "required"
    val isNotApplicable: Boolean get() = applicability == "not_applicable"
}

@Serializable
data class BusinessDto(
    val id: String? = null,
    @SerialName("vendor_id") val vendorId: String,
    @SerialName("business_type_id") val businessTypeId: String? = null,
    val name: String,
    @SerialName("owner_name") val ownerName: String? = null,
    val description: String? = null,
    val address: String? = null,
    val district: String? = null,
    @SerialName("location_lat") val locationLat: Double? = null,
    @SerialName("location_lng") val locationLng: Double? = null,
    val phone: String? = null,
    @SerialName("opening_hours") val openingHours: String? = null,
    @SerialName("main_photo_url") val mainPhotoUrl: String? = null,
    val status: String = "draft",
    @SerialName("verification_notes") val verificationNotes: String? = null,
    @SerialName("profile_completeness") val profileCompleteness: Int = 0,
    @SerialName("onboarding_step") val onboardingStep: String? = null,
    @SerialName("submitted_at") val submittedAt: String? = null,
    @SerialName("verified_at") val verifiedAt: String? = null,
)

/** Row shape returned by the nearby_businesses() SQL function. */
@Serializable
data class NearbyBusinessDto(
    val id: String,
    @SerialName("vendor_id") val vendorId: String,
    val name: String,
    @SerialName("business_type_id") val businessTypeId: String? = null,
    @SerialName("type_name") val typeName: String? = null,
    @SerialName("category_slug") val categorySlug: String? = null,
    val address: String? = null,
    val district: String? = null,
    @SerialName("location_lat") val locationLat: Double? = null,
    @SerialName("location_lng") val locationLng: Double? = null,
    @SerialName("opening_hours") val openingHours: String? = null,
    @SerialName("main_photo_url") val mainPhotoUrl: String? = null,
    val status: String,
    @SerialName("distance_km") val distanceKm: Double,
)

/**
 * A sellable thing. Not a dish: [kind] spans item, box, bundle, package and
 * service so a gift shop and an electrician fit the same table as a tea stall.
 */
@Serializable
data class CatalogueItemDto(
    val id: String? = null,
    @SerialName("vendor_id") val vendorId: String,
    @SerialName("business_id") val businessId: String? = null,
    val name: String,
    val price: Double,
    val description: String? = null,
    val kind: String = "item",
    /** Free text: "per kg", "per hour", "box of 12". Null for a plain price. */
    val unit: String? = null,
    val available: Boolean = true,
    @SerialName("sort_order") val sortOrder: Int = 100,
    @SerialName("photo_url") val photoUrl: String? = null,
)

@Serializable
data class BusinessDocumentDto(
    val id: String? = null,
    @SerialName("business_id") val businessId: String,
    @SerialName("document_type") val documentType: String,
    @SerialName("document_number") val documentNumber: String? = null,
    /** Private bucket path. Never a public URL — these are certificates. */
    @SerialName("storage_path") val storagePath: String? = null,
    val status: String = "submitted",
    @SerialName("admin_notes") val adminNotes: String? = null,
)

@Serializable
data class RewardRuleDto(
    @SerialName("activity_type") val activityType: String,
    val label: String,
    val points: Int,
    val active: Boolean = true,
    @SerialName("daily_cap") val dailyCap: Int? = null,
    val description: String? = null,
)

@Serializable
data class RewardTransactionDto(
    val id: String? = null,
    @SerialName("customer_id") val customerId: String,
    val direction: String,
    val points: Int,
    @SerialName("activity_type") val activityType: String,
    @SerialName("reference_id") val referenceId: String? = null,
    val status: String = "pending",
    val note: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
)
