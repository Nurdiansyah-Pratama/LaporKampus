package lat.pam.laporkampus.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import lat.pam.laporkampus.data.model.ReportCategories
import lat.pam.laporkampus.data.repository.FirebaseReportRepository
import lat.pam.laporkampus.data.repository.ReportRepository
import lat.pam.laporkampus.util.ReportFormValidator

/**
 * State UI untuk layar pembuatan laporan (Sprint 2).
 */
data class CreateReportUiState(
    val kategori: String = ReportCategories.ALL.first(),
    val deskripsi: String = "",
    val isAnonymous: Boolean = false,
    val isSensitive: Boolean = false,
    val photoUris: List<Uri> = emptyList(),
    val lat: Double? = -6.8915, // Koordinat awal default (Area Kampus Bandung)
    val lng: Double? = 107.6107,
    val namaLokasi: String = "",
    val isMapExpanded: Boolean = true,
    val cameraPermissionDenied: Boolean = false,
    val locationPermissionDenied: Boolean = false,
    val categoryError: String? = null,
    val descriptionError: String? = null,
    val locationError: String? = null,
    val isSubmitting: Boolean = false,
    val submitErrorMessage: String? = null,
    val isSubmitSuccess: Boolean = false
)

class CreateReportViewModel(
    private val reportRepository: ReportRepository = FirebaseReportRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateReportUiState())
    val uiState: StateFlow<CreateReportUiState> = _uiState.asStateFlow()

    fun onKategoriChanged(newKategori: String) {
        val autoSensitive = newKategori.contains("Sensitif", ignoreCase = true)
        _uiState.update {
            it.copy(
                kategori = newKategori,
                isSensitive = if (autoSensitive) true else it.isSensitive,
                categoryError = null
            )
        }
    }

    fun onDeskripsiChanged(newDeskripsi: String) {
        _uiState.update {
            it.copy(deskripsi = newDeskripsi, descriptionError = null)
        }
    }

    fun onAnonymousChanged(anonymous: Boolean) {
        _uiState.update { it.copy(isAnonymous = anonymous) }
    }

    fun onSensitiveChanged(sensitive: Boolean) {
        _uiState.update { it.copy(isSensitive = sensitive) }
    }

    fun onNamaLokasiChanged(newNamaLokasi: String) {
        _uiState.update {
            it.copy(namaLokasi = newNamaLokasi, locationError = null)
        }
    }

    fun onMapCoordinatesSelected(lat: Double, lng: Double) {
        _uiState.update {
            it.copy(
                lat = lat,
                lng = lng,
                locationError = null
            )
        }
    }

    fun toggleMapExpanded() {
        _uiState.update { it.copy(isMapExpanded = !it.isMapExpanded) }
    }

    fun addPhotoUri(uri: Uri) {
        _uiState.update { current ->
            if (current.photoUris.size >= 3 || current.photoUris.contains(uri)) {
                current
            } else {
                current.copy(photoUris = current.photoUris + uri)
            }
        }
    }

    fun removePhotoUri(uri: Uri) {
        _uiState.update { current ->
            current.copy(photoUris = current.photoUris.filterNot { it == uri })
        }
    }

    fun onCameraPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(cameraPermissionDenied = !granted) }
    }

    fun onLocationPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(locationPermissionDenied = !granted) }
    }

    fun consumeSubmitSuccess() {
        _uiState.update { CreateReportUiState() }
    }

    fun submitReport(reporterId: String, reporterName: String) {
        val state = _uiState.value
        val validCategory = ReportFormValidator.isValidCategory(state.kategori)
        val validDesc = ReportFormValidator.isValidDescription(state.deskripsi)
        val validLocation = ReportFormValidator.isValidLocation(
            namaLokasi = state.namaLokasi,
            lat = state.lat,
            lng = state.lng
        )

        if (!validCategory || !validDesc || !validLocation) {
            _uiState.update {
                it.copy(
                    categoryError = if (!validCategory) "Pilih kategori laporan terlebih dahulu" else null,
                    descriptionError = if (!validDesc) "Deskripsi laporan minimal 10 karakter" else null,
                    locationError = if (!validLocation) "Nama lokasi atau titik koordinat harus diisi" else null,
                    submitErrorMessage = null
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSubmitting = true,
                    categoryError = null,
                    descriptionError = null,
                    locationError = null,
                    submitErrorMessage = null
                )
            }

            val result = reportRepository.createReport(
                reporterId = reporterId,
                reporterName = reporterName,
                kategori = state.kategori,
                deskripsi = state.deskripsi,
                isAnonymous = state.isAnonymous,
                isSensitive = state.isSensitive,
                photoUris = state.photoUris,
                lat = state.lat,
                lng = state.lng,
                namaLokasi = state.namaLokasi
            )

            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            isSubmitSuccess = true,
                            submitErrorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            submitErrorMessage = error.localizedMessage
                                ?: "Gagal mengirim laporan. Periksa koneksi internet Anda."
                        )
                    }
                }
            )
        }
    }
}
