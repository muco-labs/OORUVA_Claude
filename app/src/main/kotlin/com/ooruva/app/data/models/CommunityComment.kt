package com.ooruva.app.data.models

import kotlinx.serialization.Serializable

@Serializable
data class CommunityComment(
    val id: String,
    val postId: String,
    val userId: String,
    val userName: String,
    val userPhotoUrl: String = "",
    val text: String,
    val createdAt: Long = System.currentTimeMillis()
)

