package com.ooruva.app.ui.screens

import com.ooruva.app.data.models.Vendor
import com.ooruva.app.data.remote.BusinessCategoryDto
import com.ooruva.app.data.remote.BusinessDto
import com.ooruva.app.data.remote.NearbyBusinessDto

/**
 * What the discovery screen is showing, and where it came from.
 *
 * [fromSampleData] is not cosmetic. When the backend is not configured the app
 * still renders so design work can continue, but a customer must never be shown
 * invented businesses as though they were real places they could walk to. The
 * flag drives a visible banner.
 */
data class DiscoveryState(
    val categories: List<BusinessCategoryDto> = emptyList(),
    val selectedCategorySlug: String? = null,
    val businesses: List<Vendor> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
    val fromSampleData: Boolean = false,
)

/**
 * Erode town centre. Used only as a search origin before a location fix
 * arrives, so the first screenful is not empty while the permission dialog is
 * still up. It is never presented as the customer's own location.
 */
const val DEFAULT_ORIGIN_LAT = 11.3410
const val DEFAULT_ORIGIN_LNG = 77.7172

fun NearbyBusinessDto.toVendor(): Vendor = Vendor(
    id = id,
    name = name,
    category = (categorySlug ?: "other").uppercase(),
    description = typeName.orEmpty(),
    latitude = locationLat ?: 0.0,
    longitude = locationLng ?: 0.0,
    address = address.orEmpty(),
    phone = "",
    hours = openingHours.orEmpty(),
    // Ratings are computed from the reviews table, which discovery does not
    // join. Left at zero rather than guessed; the detail screen loads the real
    // figure. A plausible-looking invented rating is worse than none.
    rating = 0f,
    reviewCount = 0,
    photoUrl = mainPhotoUrl.orEmpty(),
)

fun BusinessDto.toVendor(): Vendor = Vendor(
    id = id.orEmpty(),
    name = name,
    category = "OTHER",
    description = description.orEmpty(),
    latitude = locationLat ?: 0.0,
    longitude = locationLng ?: 0.0,
    address = address.orEmpty(),
    phone = phone.orEmpty(),
    hours = openingHours.orEmpty(),
    rating = 0f,
    reviewCount = 0,
    photoUrl = mainPhotoUrl.orEmpty(),
)
