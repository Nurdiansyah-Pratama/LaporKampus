package lat.pam.laporkampus.viewmodel

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
import lat.pam.laporkampus.data.model.ReportPriority
import lat.pam.laporkampus.data.model.ReportStatus
import lat.pam.laporkampus.data.repository.FirebaseReportRepository
import lat.pam.laporkampus.data.repository.ReportRepository
import lat.pam.laporkampus.util.StatusTransitionRules

data class SatgasUiState(
    val isLoading: Boolean = true,
    val allSensitiveReports: List<Report> = emptyList(),
    val statusCounts: Map<ReportStatus, Int> = ReportStatus.entries.associateWith { 0 },
    val selectedStatusFilter: ReportStatus? = null,
    val isUpdating: Boolean = false,
    val errorMessage: String? = null,
    val updateSuccessMessage: String? = null
) {
    val filteredReports: List<Report>
        get() = if (selectedStatusFilter == null) {
            allSensitiveReports
        } else {
            allSensitiveReports.filter { it.statusEnum == selectedStatusFilter }
        }
}

class SatgasViewModel(
    private val reportRepository: ReportRepository = FirebaseReportRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(SatgasUiState())
    val uiState: StateFlow<SatgasUiState> = _uiState.asStateFlow()

    private var reportsJob: Job? = null

    fun startObservingSensitiveReports() {
        if (reportsJob?.isActive == true) return
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        reportsJob = viewModelScope.launch {
            reportRepository.observeSensitiveReportsForSatgas()
                .catch { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.localizedMessage
                                ?: "Gagal memuat daftar laporan sensitif Satgas."
                        )
                    }
                }
                .collect { reports ->
                    val counts = StatusTransitionRules.countByStatus(reports)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            allSensitiveReports = reports,
                            statusCounts = counts,
                            errorMessage = null
                        )
                    }
                }
        }
    }

    fun onStatusFilterSelected(status: ReportStatus?) {
        _uiState.update { it.copy(selectedStatusFilter = status) }
    }

    fun consumeMessages() {
        _uiState.update { it.copy(errorMessage = null, updateSuccessMessage = null) }
    }

    fun updateSensitiveReport(
        report: Report,
        newStatus: ReportStatus,
        newPriority: ReportPriority,
        catatan: String,
        satgasName: String
    ) {
        if (catatan.trim().isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Catatan perubahan status wajib diisi.") }
            return
        }

        val currentStatus = report.statusEnum
        if (newStatus != currentStatus && !StatusTransitionRules.canTransition(currentStatus, newStatus)) {
            _uiState.update {
                it.copy(
                    errorMessage = "Transisi status dari ${currentStatus.label} ke ${newStatus.label} tidak diizinkan."
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(isUpdating = true, errorMessage = null, updateSuccessMessage = null)
            }
            val result = reportRepository.updateSensitiveReportBySatgas(
                report = report,
                newStatus = newStatus,
                newPriority = newPriority,
                catatan = catatan,
                satgasName = satgasName
            )
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isUpdating = false,
                            updateSuccessMessage = "Status penanganan laporan sensitif berhasil diperbarui oleh Satgas."
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isUpdating = false,
                            errorMessage = error.localizedMessage
                                ?: "Gagal memperbarui laporan sensitif."
                        )
                    }
                }
            )
        }
    }
}
