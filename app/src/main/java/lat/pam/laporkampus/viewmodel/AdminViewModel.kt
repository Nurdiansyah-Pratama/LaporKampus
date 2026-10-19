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
import lat.pam.laporkampus.data.model.UserProfile
import lat.pam.laporkampus.data.repository.FirebaseReportRepository
import lat.pam.laporkampus.data.repository.ReportRepository
import lat.pam.laporkampus.util.StatusTransitionRules

data class AdminUiState(
    val isLoading: Boolean = true,
    val allReports: List<Report> = emptyList(),
    val statusCounts: Map<ReportStatus, Int> = ReportStatus.entries.associateWith { 0 },
    val selectedStatusFilter: ReportStatus? = null,
    val technicians: List<UserProfile> = emptyList(),
    val isUpdating: Boolean = false,
    val errorMessage: String? = null,
    val updateSuccessMessage: String? = null
) {
    val filteredReports: List<Report>
        get() = if (selectedStatusFilter == null) {
            allReports
        } else {
            allReports.filter { it.statusEnum == selectedStatusFilter }
        }
}

class AdminViewModel(
    private val reportRepository: ReportRepository = FirebaseReportRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    private var reportsJob: Job? = null
    private var techniciansJob: Job? = null

    fun startObserving() {
        if (reportsJob?.isActive == true) return

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        reportsJob = viewModelScope.launch {
            reportRepository.observeAdminReports()
                .catch { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.localizedMessage
                                ?: "Gagal memuat daftar laporan untuk Admin."
                        )
                    }
                }
                .collect { reports ->
                    val counts = StatusTransitionRules.countByStatus(reports)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            allReports = reports,
                            statusCounts = counts,
                            errorMessage = null
                        )
                    }
                }
        }

        techniciansJob = viewModelScope.launch {
            reportRepository.observeTechnicians()
                .catch { }
                .collect { techs ->
                    _uiState.update { it.copy(technicians = techs) }
                }
        }
    }

    fun onStatusFilterSelected(status: ReportStatus?) {
        _uiState.update { it.copy(selectedStatusFilter = status) }
    }

    fun consumeMessages() {
        _uiState.update {
            it.copy(errorMessage = null, updateSuccessMessage = null)
        }
    }

    fun updateReport(
        report: Report,
        newStatus: ReportStatus,
        newPriority: ReportPriority,
        assignedTo: String,
        catatan: String,
        adminName: String
    ) {
        if (catatan.trim().isEmpty()) {
            _uiState.update {
                it.copy(errorMessage = "Catatan perubahan status wajib diisi.")
            }
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
            val result = reportRepository.updateReportByAdmin(
                report = report,
                newStatus = newStatus,
                newPriority = newPriority,
                assignedTo = assignedTo,
                catatan = catatan,
                adminName = adminName
            )
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isUpdating = false,
                            updateSuccessMessage = "Status, prioritas, dan penugasan laporan berhasil diperbarui."
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isUpdating = false,
                            errorMessage = error.localizedMessage
                                ?: "Gagal memperbarui laporan."
                        )
                    }
                }
            )
        }
    }
}
