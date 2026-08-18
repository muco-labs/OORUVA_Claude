package com.ooruva.app.data.repository

import com.ooruva.app.data.models.Vendor
import com.ooruva.app.data.remote.DataResult
import com.ooruva.app.data.remote.ProductDto
import com.ooruva.app.data.remote.ReviewDto
import com.ooruva.app.data.remote.Supabase
import com.ooruva.app.data.remote.VendorProfileDto
import com.ooruva.app.data.mock.getMockVendors
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Vendor reads and writes.
 *
 * Every method degrades to mock data when Supabase is not configured, and
 * reports which it used through [DataResult.Success.fromMock] so the UI can be
 * honest about it rather than silently showing fiction.
 */
object VendorRepository {

    suspend fun listVendors(
        category: String? = null,
        query: String? = null,
    ): DataResult<List<Vendor>> = withContext(Dispatchers.IO) {
        val client = Supabase.client
            ?: return@withContext DataResult.Success(mockFiltered(category, query), fromMock = true)

        runCatching {
            val rows = client.from("vendor_profiles")
                .select {
                    filter { eq("verification_status", "verified") }
                    order("business_name", Order.ASCENDING)
                }
                .decodeList<VendorProfileDto>()

            rows.map { it.toVendor() }
                .filter { category == null || category == "ALL" || it.category.equals(category, true) }
                .filter {
                    query.isNullOrBlank() ||
                        it.name.contains(query, true) ||
                        it.description.contains(query, true)
                }
        }.fold(
            onSuccess = { DataResult.Success(it) },
            onFailure = { DataResult.Failure(it.message ?: "Could not load vendors") }
        )
    }

    suspend fun vendorById(vendorId: String): DataResult<Vendor> = withContext(Dispatchers.IO) {
        val client = Supabase.client ?: return@withContext getMockVendors()
            .firstOrNull { it.id == vendorId }
            ?.let { DataResult.Success(it, fromMock = true) }
            ?: DataResult.Failure("Vendor not found")

        runCatching {
            client.from("vendor_profiles")
                .select { filter { eq("vendor_id", vendorId) } }
                .decodeSingle<VendorProfileDto>()
                .toVendor()
        }.fold(
            onSuccess = { DataResult.Success(it) },
            onFailure = { DataResult.Failure(it.message ?: "Could not load vendor") }
        )
    }

    suspend fun saveProfile(profile: VendorProfileDto): DataResult<Unit> =
        withContext(Dispatchers.IO) {
            val client = Supabase.client
                ?: return@withContext DataResult.Failure("Backend not configured")
            runCatching {
                client.from("vendor_profiles").upsert(profile)
                Unit
            }.fold(
                onSuccess = { DataResult.Success(it) },
                onFailure = { DataResult.Failure(it.message ?: "Could not save profile") }
            )
        }

    suspend fun products(vendorId: String): DataResult<List<ProductDto>> =
        withContext(Dispatchers.IO) {
            val client = Supabase.client
                ?: return@withContext DataResult.Success(emptyList(), fromMock = true)
            runCatching {
                client.from("products")
                    .select { filter { eq("vendor_id", vendorId) } }
                    .decodeList<ProductDto>()
            }.fold(
                onSuccess = { DataResult.Success(it) },
                onFailure = { DataResult.Failure(it.message ?: "Could not load products") }
            )
        }

    suspend fun addProduct(product: ProductDto): DataResult<Unit> = withContext(Dispatchers.IO) {
        val client = Supabase.client
            ?: return@withContext DataResult.Failure("Backend not configured")
        runCatching {
            client.from("products").insert(product)
            Unit
        }.fold(
            onSuccess = { DataResult.Success(it) },
            onFailure = { DataResult.Failure(it.message ?: "Could not add product") }
        )
    }

    suspend fun deleteProduct(productId: String): DataResult<Unit> = withContext(Dispatchers.IO) {
        val client = Supabase.client
            ?: return@withContext DataResult.Failure("Backend not configured")
        runCatching {
            client.from("products").delete { filter { eq("id", productId) } }
            Unit
        }.fold(
            onSuccess = { DataResult.Success(it) },
            onFailure = { DataResult.Failure(it.message ?: "Could not delete product") }
        )
    }

    suspend fun reviews(vendorId: String): DataResult<List<ReviewDto>> =
        withContext(Dispatchers.IO) {
            val client = Supabase.client
                ?: return@withContext DataResult.Success(emptyList(), fromMock = true)
            runCatching {
                client.from("reviews")
                    .select {
                        filter { eq("vendor_id", vendorId) }
                        order("created_at", Order.DESCENDING)
                    }
                    .decodeList<ReviewDto>()
            }.fold(
                onSuccess = { DataResult.Success(it) },
                onFailure = { DataResult.Failure(it.message ?: "Could not load reviews") }
            )
        }

    /** Vendor replying to a review. RLS lets the vendor touch only this column. */
    suspend fun respondToReview(reviewId: String, response: String): DataResult<Unit> =
        withContext(Dispatchers.IO) {
            val client = Supabase.client
                ?: return@withContext DataResult.Failure("Backend not configured")
            runCatching {
                client.from("reviews").update(
                    mapOf("vendor_response" to response)
                ) { filter { eq("id", reviewId) } }
                Unit
            }.fold(
                onSuccess = { DataResult.Success(it) },
                onFailure = { DataResult.Failure(it.message ?: "Could not send response") }
            )
        }

    private fun mockFiltered(category: String?, query: String?): List<Vendor> =
        getMockVendors()
            .filter { category == null || category == "ALL" || it.category.equals(category, true) }
            .filter {
                query.isNullOrBlank() ||
                    it.name.contains(query, true) ||
                    it.description.contains(query, true)
            }
}

/** Wire shape to the UI model the screens already speak. */
fun VendorProfileDto.toVendor(): Vendor = Vendor(
    id = vendorId,
    name = businessName,
    category = businessCategory.uppercase(),
    description = description.orEmpty(),
    latitude = locationLat,
    longitude = locationLng,
    address = address.orEmpty(),
    phone = phone.orEmpty(),
    hours = openingHours.orEmpty(),
    rating = 0f,
    reviewCount = 0,
    photoUrl = mainPhotoUrl.orEmpty()
)
