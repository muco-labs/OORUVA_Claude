package com.ooruva.app.data.models

import kotlinx.serialization.Serializable

@Serializable
data class CheckIn(
    val id: String,
    val userId: String,
    val vendorId: String,
    val vendorName: String,
    val timestamp: Long = System.currentTimeMillis()
)

