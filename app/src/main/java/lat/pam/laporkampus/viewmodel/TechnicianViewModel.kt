package lat.pam.laporkampus.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import lat.pam.laporkampus.data.model.Report
import lat.pam.laporkampus.data.repository.FirebaseReportRepository
import lat.pam.laporkampus.data.repository.ReportRepository

data class TechnicianUiState(
    val isLoading: Boolean = true,
    val tasks: List<Report> = emptyList(),
    val isSubmittingCompletion: Boolean = false,
    val errorMessage: String? = null,
    val completionSuccessMessage: String? = null
)

class TechnicianViewModel(
    private val reportRepository: ReportRepository = FirebaseReportRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(TechnicianUiState())
    val uiState: StateFlow<TechnicianUiState> = _uiState.asStateFlow()

    private var tasksJob: Job? = null
    private var activeTechId: String? = null

    fun startObservingTasks(technicianId: String, technicianName: String) {
        if (activeTechId == technicianId && tasksJob?.isActive == true) return
        activeTechId = technicianId
        tasksJob?.cancel()

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        tasksJob = viewModelScope.launch {
            reportRepository.observeTechnicianTasks(technicianId, technicianName)
                .catch { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.localizedMessage
                                ?: "Gagal memuat daftar tugas teknisi."
                        )
                    }
                }
                .collect { reports ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            tasks = reports,
                            errorMessage = null
                        )
                    }
                }
        }
    }

    fun consumeMessages() {
        _uiState.update {
            it.copy(errorMessage = null, completionSuccessMessage = null)
        }
    }

    fun completeTask(
        report: Report,
        proofPhotoUris: List<Uri>,
        catatan: String,
        technicianName: String
    ) {
        if (proofPhotoUris.isEmpty()) {
            _uiState.update {
                it.copy(errorMessage = "Wajib melampirkan minimal 1 foto bukti hasil perbaikan.")
            }
            return
        }
        if (catatan.trim().isEmpty()) {
            _uiState.update {
                it.copy(errorMessage = "Catatan penyelesaian pekerjaan wajib diisi.")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSubmittingCompletion = true,
                    errorMessage = null,
                    completionSuccessMessage = null
                )
            }
            val result = reportRepository.completeTaskByTechnician(
                report = report,
                completionPhotoUris = proofPhotoUris,
                catatan = catatan,
                technicianName = technicianName
            )
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isSubmittingCompletion = false,
                            completionSuccessMessage = "Tugas berhasil ditandai SELESAI beserta foto bukti perbaikan."
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isSubmittingCompletion = false,
                            errorMessage = error.localizedMessage
                                ?: "Gagal menyelesaikan tugas."
                        )
                    }
                }
            )
        }
    }
}
