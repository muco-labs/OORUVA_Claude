package com.ooruva.app.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire shapes. Field names match the Postgres columns in supabase/01_schema.sql
 * exactly — snake_case on the wire, camelCase in Kotlin.
 */

@Serializable
data class UserDto(
    val id: String? = null,
    val phone: String,
    val role: String,
    @SerialName("auth_uid") val authUid: String? = null,
    val suspended: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class CustomerProfileDto(
    val id: String? = null,
    @SerialName("customer_id") val customerId: String,
    val name: String? = null,
    @SerialName("location_lat") val locationLat: Double? = null,
    @SerialName("location_lng") val locationLng: Double? = null,
    @SerialName("profile_photo_url") val profilePhotoUrl: String? = null,
)

@Serializable
data class VendorProfileDto(
    val id: String? = null,
    @SerialName("vendor_id") val vendorId: String,
    @SerialName("business_name") val businessName: String,
    @SerialName("owner_name") val ownerName: String? = null,
    @SerialName("business_category") val businessCategory: String,
    @SerialName("location_lat") val locationLat: Double,
    @SerialName("location_lng") val locationLng: Double,
    val address: String? = null,
    val phone: String? = null,
    @SerialName("opening_hours") val openingHours: String? = null,
    val description: String? = null,
    @SerialName("main_photo_url") val mainPhotoUrl: String? = null,
    @SerialName("verification_status") val verificationStatus: String = "pending",
    @SerialName("verification_notes") val verificationNotes: String? = null,
    @SerialName("views_count") val viewsCount: Int = 0,
)

/** Row shape returned by the vendors_within_km() SQL function. */
@Serializable
data class NearbyVendorDto(
    val id: String,
    @SerialName("vendor_id") val vendorId: String,
    @SerialName("business_name") val businessName: String,
    @SerialName("business_category") val businessCategory: String,
    @SerialName("location_lat") val locationLat: Double,
    @SerialName("location_lng") val locationLng: Double,
    val address: String? = null,
    @SerialName("opening_hours") val openingHours: String? = null,
    @SerialName("main_photo_url") val mainPhotoUrl: String? = null,
    @SerialName("verification_status") val verificationStatus: String,
    @SerialName("distance_km") val distanceKm: Double,
)

@Serializable
data class ProductDto(
    val id: String? = null,
    @SerialName("vendor_id") val vendorId: String,
    val name: String,
    val price: Double,
    val description: String? = null,
    @SerialName("photo_url") val photoUrl: String? = null,
)

@Serializable
data class VendorPhotoDto(
    val id: String? = null,
    @SerialName("vendor_id") val vendorId: String,
    @SerialName("photo_url") val photoUrl: String,
    val caption: String? = null,
    @SerialName("is_main") val isMain: Boolean = false,
)

@Serializable
data class FssaiDto(
    val id: String? = null,
    @SerialName("vendor_id") val vendorId: String,
    @SerialName("fssai_number") val fssaiNumber: String? = null,
    @SerialName("certificate_url") val certificateUrl: String? = null,
    val status: String = "pending",
    @SerialName("admin_notes") val adminNotes: String? = null,
)

@Serializable
data class ReviewDto(
    val id: String? = null,
    @SerialName("vendor_id") val vendorId: String,
    @SerialName("customer_id") val customerId: String,
    val rating: Int,
    val text: String? = null,
    @SerialName("photo_url") val photoUrl: String? = null,
    @SerialName("vendor_response") val vendorResponse: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class PostDto(
    val id: String? = null,
    @SerialName("customer_id") val customerId: String,
    @SerialName("vendor_id") val vendorId: String? = null,
    val caption: String? = null,
    @SerialName("photo_url") val photoUrl: String? = null,
    @SerialName("likes_count") val likesCount: Int = 0,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class PostCommentDto(
    val id: String? = null,
    @SerialName("post_id") val postId: String,
    @SerialName("customer_id") val customerId: String,
    val text: String,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class RewardDto(
    val id: String? = null,
    @SerialName("customer_id") val customerId: String,
    val points: Int,
    @SerialName("activity_type") val activityType: String,
    @SerialName("reference_id") val referenceId: String? = null,
    val status: String = "pending",
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class OfferDto(
    val id: String? = null,
    @SerialName("vendor_id") val vendorId: String,
    val title: String,
    val description: String? = null,
    @SerialName("discount_percentage") val discountPercentage: Double? = null,
    @SerialName("discount_amount") val discountAmount: Double? = null,
    @SerialName("points_required") val pointsRequired: Int = 0,
    @SerialName("validity_date") val validityDate: String? = null,
    @SerialName("redemptions_count") val redemptionsCount: Int = 0,
)

@Serializable
data class VerificationQueueDto(
    val id: String? = null,
    @SerialName("vendor_id") val vendorId: String,
    val status: String = "pending",
    @SerialName("admin_notes") val adminNotes: String? = null,
)
