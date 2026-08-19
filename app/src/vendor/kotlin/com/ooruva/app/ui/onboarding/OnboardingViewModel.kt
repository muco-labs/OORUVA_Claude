package com.ooruva.app.ui.onboarding

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ooruva.app.data.remote.BusinessDocumentDto
import com.ooruva.app.data.remote.BusinessDto
import com.ooruva.app.data.remote.CatalogueItemDto
import com.ooruva.app.data.remote.DataResult
import com.ooruva.app.data.repository.BusinessRepository
import com.ooruva.app.data.repository.StorageRepository
import com.ooruva.app.data.repository.TaxonomyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Drives vendor onboarding.
 *
 * A ViewModel rather than `remember`, which is the idiom elsewhere in this
 * codebase, because onboarding is the one flow where losing state to a rotation
 * or a backgrounded app costs the vendor real work.
 *
 * The draft is written to the backend after every step. That is more round
 * trips than saving once at the end, and it is the right trade: the alternative
 * is a vendor who fills in eleven screens on mobile data and loses all of it.
 */
class OnboardingViewModel(
    private val vendorUserId: String,
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    init {
        loadTaxonomy()
        resumeExistingDraft()
    }

    // == Loading ==============================================================

    private fun loadTaxonomy() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            when (val result = TaxonomyRepository.categories()) {
                is DataResult.Success -> _state.update {
                    it.copy(categories = result.data, loading = false)
                }
                is DataResult.Failure -> _state.update {
                    // Named plainly. "Something went wrong" here would leave the
                    // vendor tapping a category list that will never populate.
                    it.copy(
                        loading = false,
                        error = "Could not load business categories. Check your connection.",
                    )
                }
                DataResult.Loading -> Unit
            }
        }
    }

    /** Picks up an unfinished draft so the vendor resumes rather than restarts. */
    private fun resumeExistingDraft() {
        viewModelScope.launch {
            when (val mine = BusinessRepository.mine()) {
                is DataResult.Success -> {
                    val draftRow = mine.data.firstOrNull { it.status == "draft" } ?: return@launch
                    _state.update { current ->
                        current.copy(
                            step = OnboardingStep.fromKey(draftRow.onboardingStep),
                            draft = current.draft.copy(
                                businessId = draftRow.id,
                                name = draftRow.name,
                                ownerName = draftRow.ownerName.orEmpty(),
                                businessTypeId = draftRow.businessTypeId,
                                description = draftRow.description.orEmpty(),
                                phone = draftRow.phone.orEmpty(),
                                address = draftRow.address.orEmpty(),
                                district = draftRow.district.orEmpty(),
                                latitude = draftRow.locationLat,
                                longitude = draftRow.locationLng,
                                openingHours = draftRow.openingHours.orEmpty(),
                                // An existing row means they came back to it.
                                isExistingBusiness = current.draft.isExistingBusiness ?: false,
                            ),
                        )
                    }
                    draftRow.businessTypeId?.let { loadRequirements(it) }
                    draftRow.id?.let { loadCatalogue(it) }
                }
                // No draft to resume, or the backend is unreachable. Either way
                // starting fresh is the right behaviour, and the save step will
                // surface any connection problem with a message that fits.
                else -> Unit
            }
        }
    }

    private fun loadRequirements(businessTypeId: String) {
        viewModelScope.launch {
            when (val result = TaxonomyRepository.requirementsFor(businessTypeId)) {
                is DataResult.Success -> _state.update {
                    it.copy(prompts = result.data.mapNotNull { r -> r.toPrompt() })
                }
                else -> Unit
            }
        }
    }

    private fun loadCatalogue(businessId: String) {
        viewModelScope.launch {
            when (val result = BusinessRepository.catalogue(businessId)) {
                is DataResult.Success -> _state.update {
                    it.copy(draft = it.draft.copy(catalogue = result.data))
                }
                else -> Unit
            }
        }
    }

    // == Editing ==============================================================

    fun update(transform: (OnboardingDraft) -> OnboardingDraft) {
        _state.update { it.copy(draft = transform(it.draft), error = null) }
    }

    fun chooseCategory(categoryId: String) {
        _state.update {
            it.copy(
                // Changing category invalidates the chosen type: a tea stall
                // type has no meaning under Automotive. Clearing it here stops
                // a mismatched pair reaching the verification queue.
                draft = it.draft.copy(categoryId = categoryId, businessTypeId = null),
                types = emptyList(),
                prompts = emptyList(),
            )
        }
        viewModelScope.launch {
            when (val result = TaxonomyRepository.types(categoryId)) {
                is DataResult.Success -> _state.update { it.copy(types = result.data) }
                is DataResult.Failure -> _state.update {
                    it.copy(error = "Could not load business types. Check your connection.")
                }
                DataResult.Loading -> Unit
            }
        }
    }

    fun chooseType(typeId: String) {
        _state.update { it.copy(draft = it.draft.copy(businessTypeId = typeId)) }
        loadRequirements(typeId)
    }

    fun setDocument(entry: DocumentEntry) {
        _state.update {
            it.copy(draft = it.draft.copy(
                documents = it.draft.documents + (entry.requirementKey to entry)
            ))
        }
    }

    /**
     * Uploads a certificate to the private bucket.
     *
     * Needs a saved business first: storage writes are scoped to
     * `<business_id>/...` and a draft that has never reached the backend has no
     * id to scope to. Saving here rather than refusing means the vendor is not
     * told to "go back and press continue" for reasons that are ours, not
     * theirs.
     */
    fun uploadDocument(context: Context, requirementKey: String, uri: Uri) {
        val existing = _state.value.draft.documents[requirementKey]
            ?: DocumentEntry(requirementKey)

        viewModelScope.launch {
            setDocument(existing.copy(uploading = true, uploadError = null))

            val businessId = _state.value.draft.businessId
                ?: if (persist(_state.value.step)) _state.value.draft.businessId else null

            if (businessId == null) {
                setDocument(existing.copy(
                    uploading = false,
                    uploadError = "Could not reach OORUVA, so the file was not uploaded. Try again when you have signal.",
                ))
                return@launch
            }

            when (val result = StorageRepository.uploadDocument(context, businessId, requirementKey, uri)) {
                is DataResult.Success -> setDocument(
                    existing.copy(
                        storagePath = result.data,
                        uploading = false,
                        uploadError = null,
                        declaredNotApplicable = false,
                    )
                )
                is DataResult.Failure -> setDocument(
                    existing.copy(uploading = false, uploadError = result.message)
                )
                DataResult.Loading -> Unit
            }
        }
    }

    fun uploadPhoto(context: Context, uri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(saving = true, error = null) }

            val businessId = _state.value.draft.businessId
                ?: if (persist(_state.value.step)) _state.value.draft.businessId else null

            if (businessId == null) {
                _state.update {
                    it.copy(
                        saving = false,
                        error = "Could not reach OORUVA, so the photo was not uploaded. Try again when you have signal.",
                    )
                }
                return@launch
            }

            when (val result = StorageRepository.uploadPhoto(context, businessId, uri)) {
                is DataResult.Success -> _state.update {
                    it.copy(
                        saving = false,
                        draft = it.draft.copy(photoPaths = it.draft.photoPaths + result.data),
                    )
                }
                is DataResult.Failure -> _state.update {
                    it.copy(saving = false, error = result.message)
                }
                DataResult.Loading -> Unit
            }
        }
    }

    fun removePhoto(path: String) {
        // Removed from the draft only. The object is left in the bucket rather
        // than deleted here: a vendor who removes a photo and then loses the
        // app mid-step would otherwise have destroyed a file the listing still
        // references. Orphans are cleaned up server-side, where the full
        // picture is available.
        _state.update {
            it.copy(draft = it.draft.copy(photoPaths = it.draft.photoPaths - path))
        }
    }

    fun addCatalogueItem(item: CatalogueItemDto) {
        _state.update { it.copy(draft = it.draft.copy(catalogue = it.draft.catalogue + item)) }
    }

    fun removeCatalogueItem(index: Int) {
        _state.update {
            it.copy(draft = it.draft.copy(
                catalogue = it.draft.catalogue.filterIndexed { i, _ -> i != index }
            ))
        }
    }

    // == Navigation ===========================================================

    fun back() {
        _state.update { current ->
            val steps = OnboardingStep.entries
            val index = steps.indexOf(current.step)
            if (index <= 0) current else current.copy(step = steps[index - 1], error = null)
        }
    }

    /**
     * Saves the draft, then advances. Advancing only on a successful save would
     * strand a vendor with no signal; advancing without saving would lose their
     * work silently. So it advances either way and says which happened.
     */
    fun next() {
        val current = _state.value
        if (!current.draft.canAdvance(current.step)) return

        val steps = OnboardingStep.entries
        val index = steps.indexOf(current.step)
        val nextStep = steps.getOrNull(index + 1) ?: return

        viewModelScope.launch {
            _state.update { it.copy(saving = true) }
            val saved = persist(nextStep)
            _state.update {
                it.copy(step = nextStep, saving = false, offlineDraft = !saved)
            }
        }
    }

    private suspend fun persist(atStep: OnboardingStep): Boolean {
        val draft = _state.value.draft
        val row = BusinessDto(
            id = draft.businessId,
            vendorId = vendorUserId,
            businessTypeId = draft.businessTypeId,
            name = draft.name.ifBlank { "Untitled business" },
            ownerName = draft.ownerName.ifBlank { null },
            description = draft.description.ifBlank { null },
            address = draft.address.ifBlank { null },
            district = draft.district.ifBlank { null },
            locationLat = draft.latitude,
            locationLng = draft.longitude,
            phone = draft.phone.ifBlank { null },
            openingHours = draft.openingHours.ifBlank { null },
            mainPhotoUrl = draft.photoPaths.firstOrNull(),
            status = "draft",
            profileCompleteness = draft.completeness(),
            onboardingStep = atStep.key,
        )

        return when (val result = BusinessRepository.saveDraft(row)) {
            is DataResult.Success -> {
                _state.update { it.copy(draft = it.draft.copy(businessId = result.data.id)) }
                true
            }
            else -> false
        }
    }

    // == Submission ===========================================================

    /**
     * Final submit. Writes the catalogue and the declared documents, then moves
     * the row into the verification queue.
     *
     * It cannot mark the business verified — only an admin can, and the database
     * refuses it regardless of what this code asks for.
     */
    fun submit() {
        val draft = _state.value.draft
        if (!draft.canAdvance(OnboardingStep.Review)) return

        viewModelScope.launch {
            _state.update { it.copy(saving = true, error = null) }

            if (!persist(OnboardingStep.Review)) {
                _state.update {
                    it.copy(
                        saving = false,
                        error = "Could not reach OORUVA. Your details are saved on this device — try submitting again when you have signal.",
                    )
                }
                return@launch
            }

            val businessId = _state.value.draft.businessId
            if (businessId == null) {
                _state.update { it.copy(saving = false, error = "Could not save your business. Please try again.") }
                return@launch
            }

            draft.catalogue.forEach { item ->
                BusinessRepository.saveItem(
                    item.copy(vendorId = vendorUserId, businessId = businessId)
                )
            }

            // Only what the vendor actually claimed to hold. A row for a
            // registration they said does not apply would sit in the admin
            // queue looking like a missing upload.
            draft.documents.values
                .filter { !it.declaredNotApplicable && (it.number.isNotBlank() || it.hasFile) }
                .forEach { entry ->
                    BusinessRepository.saveDocument(
                        BusinessDocumentDto(
                            businessId = businessId,
                            documentType = entry.requirementKey,
                            documentNumber = entry.number.ifBlank { null },
                            storagePath = entry.storagePath,
                            status = "submitted",
                        )
                    )
                }

            when (BusinessRepository.submitForVerification(businessId)) {
                is DataResult.Success ->
                    _state.update { it.copy(saving = false, submitted = true) }
                else ->
                    _state.update {
                        it.copy(saving = false, error = "Could not submit for verification. Please try again.")
                    }
            }
        }
    }

    fun dismissError() = _state.update { it.copy(error = null) }
}
