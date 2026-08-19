package com.ooruva.app.ui.onboarding

import com.ooruva.app.data.remote.BusinessRequirementDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Onboarding gate logic.
 *
 * These are the rules that decide whether a vendor can move to the next step,
 * and how a regulatory requirement is described to them. Both are easy to break
 * with a well-meaning edit and neither is visible in a screenshot, which is
 * what makes them worth pinning down here.
 */
class OnboardingDraftTest {

    private val complete = OnboardingDraft(
        isExistingBusiness = true,
        name = "Chai Wali",
        ownerName = "R. Kumar",
        categoryId = "cat-1",
        businessTypeId = "type-1",
        description = "Tea and snacks",
        phone = "9876543210",
        address = "Main Street",
        district = "Erode",
        latitude = 11.34,
        longitude = 77.71,
        openingHours = "06:00-22:00",
        termsAccepted = true,
    )

    // == Advancing ===========================================================

    @Test
    fun `basics needs both a name and the new-or-existing answer`() {
        val draft = OnboardingDraft()
        assertFalse(draft.canAdvance(OnboardingStep.Basics))
        assertFalse(draft.copy(name = "Chai Wali").canAdvance(OnboardingStep.Basics))
        assertFalse(draft.copy(isExistingBusiness = true).canAdvance(OnboardingStep.Basics))
        assertTrue(
            draft.copy(name = "Chai Wali", isExistingBusiness = true)
                .canAdvance(OnboardingStep.Basics)
        )
    }

    @Test
    fun `a blank name does not pass as a name`() {
        assertFalse(
            OnboardingDraft(name = "   ", isExistingBusiness = false)
                .canAdvance(OnboardingStep.Basics)
        )
    }

    @Test
    fun `details needs a full ten-digit phone and an address`() {
        val base = complete.copy(phone = "", address = "")
        assertFalse(base.canAdvance(OnboardingStep.Details))
        assertFalse(base.copy(phone = "98765", address = "Main Street").canAdvance(OnboardingStep.Details))
        assertFalse(base.copy(phone = "9876543210").canAdvance(OnboardingStep.Details))
        assertTrue(base.copy(phone = "9876543210", address = "Main Street").canAdvance(OnboardingStep.Details))
    }

    /**
     * The skippable steps. A vendor standing in their shop with no GPS fix must
     * not be trapped at step five — a human reviews the listing and can chase
     * what is missing. If someone later makes these mandatory, this test should
     * fail and force the conversation.
     */
    @Test
    fun `location hours photos and catalogue are skippable`() {
        val empty = OnboardingDraft()
        assertTrue(empty.canAdvance(OnboardingStep.Location))
        assertTrue(empty.canAdvance(OnboardingStep.Hours))
        assertTrue(empty.canAdvance(OnboardingStep.Photos))
        assertTrue(empty.canAdvance(OnboardingStep.Catalogue))
        assertTrue(empty.canAdvance(OnboardingStep.Requirements))
    }

    @Test
    fun `terms cannot be skipped`() {
        assertFalse(complete.copy(termsAccepted = false).canAdvance(OnboardingStep.Terms))
        assertTrue(complete.canAdvance(OnboardingStep.Terms))
    }

    @Test
    fun `review refuses a draft with no business type`() {
        assertFalse(complete.copy(businessTypeId = null).canAdvance(OnboardingStep.Review))
        assertTrue(complete.canAdvance(OnboardingStep.Review))
    }

    // == Completeness ========================================================

    @Test
    fun `completeness is zero for an empty draft`() {
        assertEquals(0, OnboardingDraft().completeness())
    }

    @Test
    fun `a fully filled draft reaches 100`() {
        val everything = complete.copy(
            catalogue = listOf(item("Tea")),
            photoPaths = listOf("11110000-0000-0000-0000-000000000001/front.jpg"),
        )
        assertEquals(100, everything.completeness())
    }

    @Test
    fun `a typed draft with no catalogue or photos is high but not complete`() {
        // Those are two of the twelve checks, so it should land short of 100
        // rather than rounding up to it.
        val score = complete.completeness()
        assertTrue("expected 80..99, was $score", score in 80..99)
    }

    @Test
    fun `completeness never exceeds 100`() {
        val maxed = complete.copy(
            catalogue = List(50) { item("Item $it") },
            photoPaths = List(20) { "11110000-0000-0000-0000-000000000001/photo$it.jpg" },
        )
        assertTrue(maxed.completeness() <= 100)
    }

    // == Step resumption =====================================================

    @Test
    fun `an unknown or missing step key resumes at the beginning`() {
        assertEquals(OnboardingStep.Basics, OnboardingStep.fromKey(null))
        assertEquals(OnboardingStep.Basics, OnboardingStep.fromKey("not-a-step"))
        assertEquals(OnboardingStep.Requirements, OnboardingStep.fromKey("requirements"))
    }

    // == Requirement presentation ============================================

    /**
     * The important one. `requires_review` is the database saying nobody
     * qualified has decided whether this registration applies. It must reach
     * the vendor as a question, never as an instruction — OORUVA does not give
     * regulatory advice it has no basis for.
     */
    @Test
    fun `requires_review is presented as a question, not a requirement`() {
        val prompt = requirement("fssai", "requires_review").toPrompt()
        assertTrue(prompt is RequirementPrompt.AskVendor)
    }

    @Test
    fun `not_applicable is not shown at all`() {
        // A mobile repair shop should never be asked about a food licence.
        assertNull(requirement("fssai", "not_applicable").toPrompt())
    }

    @Test
    fun `required and optional map to their own prompts`() {
        assertTrue(requirement("gst", "required").toPrompt() is RequirementPrompt.Mandatory)
        assertTrue(requirement("udyam", "optional").toPrompt() is RequirementPrompt.Optional)
    }

    @Test
    fun `requirement labels are human, and unknown keys still read sensibly`() {
        assertEquals("FSSAI licence", requirementLabel("fssai"))
        assertEquals("Udyam registration", requirementLabel("udyam"))
        assertEquals("Trade licence", requirementLabel("trade_licence"))
        assertEquals("Shop establishment", requirementLabel("shop_establishment"))
    }

    private fun item(name: String) = com.ooruva.app.data.remote.CatalogueItemDto(
        vendorId = "v", name = name, price = 10.0
    )

    private fun requirement(key: String, applicability: String) = BusinessRequirementDto(
        id = "r-$key",
        businessTypeId = "type-1",
        requirementKey = key,
        applicability = applicability,
    )
}
