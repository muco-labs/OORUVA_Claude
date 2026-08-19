package com.ooruva.app.data.repository

import com.ooruva.app.data.remote.BusinessCategoryDto
import com.ooruva.app.data.remote.BusinessRequirementDto
import com.ooruva.app.data.remote.BusinessTypeDto
import com.ooruva.app.data.remote.DataResult
import com.ooruva.app.data.remote.Supabase
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * The category taxonomy, read from the database rather than compiled in.
 *
 * Adding "Cloud Kitchen" or a whole new category is an INSERT by an admin, not
 * an app release. That is the point of the tables migration 04 introduced, and
 * this repository is what stops the app from quietly re-hardcoding them.
 *
 * Cached in memory for [CACHE_TTL_MS]. The taxonomy changes a few times a year
 * at most, and it is read on the discovery screen and on every step of vendor
 * onboarding — refetching it each time would be the single chattiest thing the
 * app does for the least benefit.
 */
object TaxonomyRepository {

    private const val CACHE_TTL_MS = 60 * 60 * 1000L

    private val lock = Mutex()

    private var categories: List<BusinessCategoryDto> = emptyList()
    private var categoriesFetchedAt = 0L

    private var types: List<BusinessTypeDto> = emptyList()
    private var typesFetchedAt = 0L

    private var requirements: List<BusinessRequirementDto> = emptyList()
    private var requirementsFetchedAt = 0L

    private fun fresh(at: Long) = at != 0L && System.currentTimeMillis() - at < CACHE_TTL_MS

    /**
     * Every active category, in the order an admin chose.
     *
     * When the backend is not configured this returns an empty list rather than
     * a hardcoded fallback. A fabricated taxonomy would let onboarding collect a
     * category that does not exist in the database, and the submission would
     * fail at the last step with nothing useful to say. Empty is honest, and the
     * caller can say "categories could not be loaded".
     */
    suspend fun categories(forceRefresh: Boolean = false): DataResult<List<BusinessCategoryDto>> =
        withContext(Dispatchers.IO) {
            val client = Supabase.client
                ?: return@withContext DataResult.Success(emptyList(), fromMock = true)

            lock.withLock {
                if (!forceRefresh && fresh(categoriesFetchedAt)) {
                    return@withContext DataResult.Success(categories)
                }
                runCatching {
                    client.from("business_categories")
                        .select { order("sort_order", Order.ASCENDING) }
                        .decodeList<BusinessCategoryDto>()
                }.fold(
                    onSuccess = {
                        categories = it
                        categoriesFetchedAt = System.currentTimeMillis()
                        DataResult.Success(it)
                    },
                    onFailure = {
                        // Serve a stale cache rather than emptying the screen: an
                        // hour-old category list is still correct enough to browse.
                        if (categories.isNotEmpty()) DataResult.Success(categories)
                        else DataResult.Failure(it.message ?: "Could not load categories")
                    }
                )
            }
        }

    /** Business types, optionally narrowed to one category. */
    suspend fun types(
        categoryId: String? = null,
        forceRefresh: Boolean = false,
    ): DataResult<List<BusinessTypeDto>> = withContext(Dispatchers.IO) {
        val client = Supabase.client
            ?: return@withContext DataResult.Success(emptyList(), fromMock = true)

        val all = lock.withLock {
            if (!forceRefresh && fresh(typesFetchedAt)) {
                DataResult.Success(types)
            } else {
                runCatching {
                    client.from("business_types")
                        .select { order("sort_order", Order.ASCENDING) }
                        .decodeList<BusinessTypeDto>()
                }.fold(
                    onSuccess = {
                        types = it
                        typesFetchedAt = System.currentTimeMillis()
                        DataResult.Success(it)
                    },
                    onFailure = {
                        if (types.isNotEmpty()) DataResult.Success(types)
                        else DataResult.Failure(it.message ?: "Could not load business types")
                    }
                )
            }
        }

        when (all) {
            is DataResult.Success ->
                if (categoryId == null) all
                else DataResult.Success(all.data.filter { it.categoryId == categoryId })
            else -> all
        }
    }

    /**
     * The requirements attached to one business type.
     *
     * Callers must respect `applicability`. A `requires_review` row means nobody
     * has decided yet — it is a prompt to ask the vendor, not a statement that
     * the registration is needed. Rendering it as "Required" would have OORUVA
     * giving regulatory advice it has no basis for.
     */
    suspend fun requirementsFor(businessTypeId: String): DataResult<List<BusinessRequirementDto>> =
        withContext(Dispatchers.IO) {
            val client = Supabase.client
                ?: return@withContext DataResult.Success(emptyList(), fromMock = true)

            val all = lock.withLock {
                if (fresh(requirementsFetchedAt)) {
                    DataResult.Success(requirements)
                } else {
                    runCatching {
                        client.from("business_requirements")
                            .select()
                            .decodeList<BusinessRequirementDto>()
                    }.fold(
                        onSuccess = {
                            requirements = it
                            requirementsFetchedAt = System.currentTimeMillis()
                            DataResult.Success(it)
                        },
                        onFailure = {
                            if (requirements.isNotEmpty()) DataResult.Success(requirements)
                            else DataResult.Failure(it.message ?: "Could not load requirements")
                        }
                    )
                }
            }

            when (all) {
                is DataResult.Success ->
                    DataResult.Success(all.data.filter { it.businessTypeId == businessTypeId })
                else -> all
            }
        }

    /** Drops the cache. Called after an admin edits the taxonomy. */
    suspend fun invalidate() = lock.withLock {
        categoriesFetchedAt = 0L
        typesFetchedAt = 0L
        requirementsFetchedAt = 0L
    }
}
