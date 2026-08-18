package com.ooruva.app.data.models

import kotlinx.serialization.Serializable

@Serializable
data class VendorAnalytics(
    val vendorId: String,
    val date: String,
    val checkIns: Int = 0,
    val uniqueCustomers: Int = 0,
    val reviews: Int = 0,
    val revenue: Int = 0
)
