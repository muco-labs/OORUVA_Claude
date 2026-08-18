package com.ooruva.app.data.models

import kotlinx.serialization.Serializable

@Serializable
data class CommunityPost(
    val id: String,
    val vendorId: String,
    val userId: String,
    val userName: String,
    val userPhotoUrl: String = "",
    val caption: String,
    val photoUrl: String,
    val likes: Int = 0,
    val commentCount: Int = 0,
    val hasLiked: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

