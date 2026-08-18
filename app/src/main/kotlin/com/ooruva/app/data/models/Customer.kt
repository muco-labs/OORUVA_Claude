package com.ooruva.app.data.models

import kotlinx.serialization.Serializable

@Serializable
data class Customer(
    val customerId: String,
    val userId: String,
    val name: String = "",
    val location: String = "",
    val preferences: List<String> = emptyList(),
    val points: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
