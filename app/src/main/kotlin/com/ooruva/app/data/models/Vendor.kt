package com.ooruva.app.data.models

import kotlinx.serialization.Serializable

@Serializable
data class Vendor(
    val id: String,
    val name: String,
    val category: String, // FOOD, SHOP, SERVICE, SALON, GYM, CLINIC
    val description: String = "",
    val latitude: Double,
    val longitude: Double,
    val address: String = "",
    val phone: String = "",
    val hours: String = "10:00-22:00", // "HH:MM-HH:MM"
    val isClosed: Boolean = false,
    val rating: Float = 4.0f,
    val reviewCount: Int = 0,
    val photoUrl: String = "", // Primary photo
    val createdAt: Long = System.currentTimeMillis()
)

