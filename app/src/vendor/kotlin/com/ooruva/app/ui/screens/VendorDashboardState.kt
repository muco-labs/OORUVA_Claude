package com.ooruva.app.ui.screens

import com.ooruva.app.data.remote.BusinessDto

/**
 * What the vendor dashboard knows.
 *
 * Everything here is either read from the backend or absent. There is
 * deliberately no field for revenue, orders or customer count: OORUVA has no
 * transaction capability, so there is no honest source for those numbers and
 * the dashboard says so instead of showing a plausible zero that looks like a
 * bad trading day.
 */
data class VendorDashboardState(
    val business: BusinessDto? = null,
    val catalogueCount: Int = 0,
    val photoCount: Int = 0,
    val loading: Boolean = true,
    val error: String? = null,
) {
    val hasBusiness: Boolean get() = business != null

    /** Where the listing stands, in words a shopkeeper would use. */
    val statusLine: String
        get() = when (business?.status) {
            null -> "No listing yet"
            "draft" -> "Not submitted yet"
            "submitted" -> "With our team for review"
            "verified" -> "Live and visible to customers"
            "needs_changes" -> "Changes needed — see the notes"
            "rejected" -> "Not approved — see the notes"
            "suspended" -> "Suspended — contact support"
            else -> business.status
        }

    /**
     * Whether customers can currently find this business. Not the same question
     * as "is it verified": a suspended listing is also invisible, and a vendor
     * looking at their dashboard cares about the visibility, not the label.
     */
    val isDiscoverable: Boolean
        get() = business?.status == "verified"
}
