package com.ooruva.app.data.models

import kotlinx.serialization.Serializable

@Serializable
data class Review(
    val reviewId: String,
    val customerId: String,
    val customerName: String,
    val vendorId: String,
    val rating: Float,
    val text: String = "",
    val photos: List<String> = emptyList(),
    val helpfulCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
