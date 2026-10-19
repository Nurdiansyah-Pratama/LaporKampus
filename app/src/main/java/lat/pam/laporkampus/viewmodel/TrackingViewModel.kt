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
import lat.pam.laporkampus.data.model.ReportHistory
import lat.pam.laporkampus.data.repository.FirebaseReportRepository
import lat.pam.laporkampus.data.repository.ReportRepository

/**
 * State UI untuk layar Daftar "Laporan Saya".
 */
data class MyReportsUiState(
    val isLoading: Boolean = true,
    val reports: List<Report> = emptyList(),
    val errorMessage: String? = null
)

/**
 * State UI untuk layar Detail Laporan & Timeline History.
 */
data class ReportDetailUiState(
    val isLoading: Boolean = true,
    val report: Report? = null,
    val history: List<ReportHistory> = emptyList(),
    val errorMessage: String? = null
)

class TrackingViewModel(
    private val reportRepository: ReportRepository = FirebaseReportRepository()
) : ViewModel() {

    private val _myReportsState = MutableStateFlow(MyReportsUiState())
    val myReportsState: StateFlow<MyReportsUiState> = _myReportsState.asStateFlow()

    private val _detailState = MutableStateFlow(ReportDetailUiState())
    val detailState: StateFlow<ReportDetailUiState> = _detailState.asStateFlow()

    private var myReportsJob: Job? = null
    private var detailReportJob: Job? = null
    private var detailHistoryJob: Job? = null
    private var activeReporterId: String? = null
    private var activeReportId: String? = null

    fun startObservingMyReports(reporterId: String) {
        if (activeReporterId == reporterId && myReportsJob?.isActive == true) return
        activeReporterId = reporterId
        myReportsJob?.cancel()

        _myReportsState.update { it.copy(isLoading = true, errorMessage = null) }
        myReportsJob = viewModelScope.launch {
            reportRepository.observeMyReports(reporterId)
                .catch { error ->
                    _myReportsState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.localizedMessage
                                ?: "Gagal memuat daftar laporan."
                        )
                    }
                }
                .collect { reports ->
                    _myReportsState.update {
                        it.copy(
                            isLoading = false,
                            reports = reports,
                            errorMessage = null
                        )
                    }
                }
        }
    }

    fun startObservingReportDetail(reportId: String) {
        if (activeReportId == reportId &&
            detailReportJob?.isActive == true &&
            detailHistoryJob?.isActive == true
        ) {
            return
        }
        activeReportId = reportId
        detailReportJob?.cancel()
        detailHistoryJob?.cancel()

        _detailState.update {
            ReportDetailUiState(isLoading = true)
        }

        detailReportJob = viewModelScope.launch {
            reportRepository.observeReportById(reportId)
                .catch { error ->
                    _detailState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.localizedMessage
                                ?: "Gagal memuat detail laporan."
                        )
                    }
                }
                .collect { report ->
                    _detailState.update {
                        it.copy(
                            isLoading = false,
                            report = report,
                            errorMessage = null
                        )
                    }
                }
        }

        detailHistoryJob = viewModelScope.launch {
            reportRepository.observeReportHistory(reportId)
                .catch { error ->
                    _detailState.update {
                        it.copy(
                            errorMessage = error.localizedMessage
                                ?: "Gagal memuat riwayat status laporan."
                        )
                    }
                }
                .collect { historyList ->
                    _detailState.update {
                        it.copy(history = historyList)
                    }
                }
        }
    }
}
