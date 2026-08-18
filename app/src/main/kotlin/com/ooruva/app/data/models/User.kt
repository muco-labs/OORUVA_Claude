package com.ooruva.app.data.models

import kotlinx.serialization.Serializable



@Serializable
data class User(
    val id: String,
    val phone: String,
    val name: String = "",
    val profilePhotoUrl: String = "",
    val location: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

