package com.ooruva.app.data.models

import kotlinx.serialization.Serializable

@Serializable
data class Photo(
    val id: String,
    val vendorId: String,
    val url: String,
    val uploadedBy: String, // user_id
    val caption: String = "",
    val uploadedAt: Long = System.currentTimeMillis()
)

