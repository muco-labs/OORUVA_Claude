package com.ooruva.app.data.models

import kotlinx.serialization.Serializable

@Serializable
data class Favorite(
    val id: String,
    val userId: String,
    val vendorId: String,
    val createdAt: Long = System.currentTimeMillis()
)
