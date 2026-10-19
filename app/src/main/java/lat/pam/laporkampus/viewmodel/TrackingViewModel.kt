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
import lat.pam.laporkampus.data.model.Rating
import lat.pam.laporkampus.data.model.Report
import lat.pam.laporkampus.data.model.ReportHistory
import lat.pam.laporkampus.data.repository.FirebaseReportRepository
import lat.pam.laporkampus.data.repository.ReportRepository

data class StatusChangeNotificationEvent(
    val reportId: String,
    val kategori: String,
    val namaLokasi: String,
    val newStatusLabel: String
)

/**
 * State UI untuk layar Daftar "Laporan Saya".
 */
data class MyReportsUiState(
    val isLoading: Boolean = true,
    val reports: List<Report> = emptyList(),
    val errorMessage: String? = null,
    val statusChangeEvent: StatusChangeNotificationEvent? = null
)

/**
 * State UI untuk layar Detail Laporan, Timeline History, & Penilaian (Rating 1-5).
 */
data class ReportDetailUiState(
    val isLoading: Boolean = true,
    val report: Report? = null,
    val history: List<ReportHistory> = emptyList(),
    val existingRating: Rating? = null,
    val isSubmittingRating: Boolean = false,
    val ratingSuccessMessage: String? = null,
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
    private var detailRatingJob: Job? = null
    private var activeReporterId: String? = null
    private var activeReportId: String? = null

    // Menyimpan peta status terakhir setiap laporan untuk memicu notifikasi lokal saat status berubah
    private val knownReportStatuses = mutableMapOf<String, String>()
    private var hasInitializedBaseline = false

    fun startObservingMyReports(reporterId: String) {
        if (activeReporterId == reporterId && myReportsJob?.isActive == true) return
        activeReporterId = reporterId
        knownReportStatuses.clear()
        hasInitializedBaseline = false
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
                    var detectedChange: StatusChangeNotificationEvent? = null
                    if (hasInitializedBaseline) {
                        for (report in reports) {
                            val previousStatus = knownReportStatuses[report.id]
                            if (previousStatus != null && previousStatus != report.status) {
                                detectedChange = StatusChangeNotificationEvent(
                                    reportId = report.id,
                                    kategori = report.kategori,
                                    namaLokasi = report.namaLokasi,
                                    newStatusLabel = report.statusEnum.label
                                )
                            }
                            knownReportStatuses[report.id] = report.status
                        }
                    } else {
                        for (report in reports) {
                            knownReportStatuses[report.id] = report.status
                        }
                        hasInitializedBaseline = true
                    }

                    _myReportsState.update {
                        it.copy(
                            isLoading = false,
                            reports = reports,
                            errorMessage = null,
                            statusChangeEvent = detectedChange ?: it.statusChangeEvent
                        )
                    }
                }
        }
    }

    fun consumeStatusChangeEvent() {
        _myReportsState.update { it.copy(statusChangeEvent = null) }
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
        detailRatingJob?.cancel()

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

        detailRatingJob = viewModelScope.launch {
            reportRepository.observeRatingForReport(reportId)
                .catch { }
                .collect { rating ->
                    _detailState.update {
                        it.copy(existingRating = rating)
                    }
                }
        }
    }

    fun submitRating(report: Report, nilai: Int, komentar: String) {
        viewModelScope.launch {
            _detailState.update {
                it.copy(
                    isSubmittingRating = true,
                    errorMessage = null,
                    ratingSuccessMessage = null
                )
            }
            val result = reportRepository.submitRating(
                report = report,
                nilai = nilai,
                komentar = komentar
            )
            result.fold(
                onSuccess = { savedRating ->
                    _detailState.update {
                        it.copy(
                            isSubmittingRating = false,
                            existingRating = savedRating,
                            ratingSuccessMessage = "Terima kasih! Penilaian Anda berhasil disimpan."
                        )
                    }
                },
                onFailure = { error ->
                    _detailState.update {
                        it.copy(
                            isSubmittingRating = false,
                            errorMessage = error.localizedMessage
                                ?: "Gagal menyimpan penilaian."
                        )
                    }
                }
            )
        }
    }

    fun consumeRatingMessages() {
        _detailState.update { it.copy(ratingSuccessMessage = null, errorMessage = null) }
    }
}
