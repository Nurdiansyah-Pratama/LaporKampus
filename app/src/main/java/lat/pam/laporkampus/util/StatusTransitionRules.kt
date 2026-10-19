package lat.pam.laporkampus.util

import lat.pam.laporkampus.data.model.ReportStatus

/**
 * Aturan terpusat untuk validasi transisi status laporan:
 * DITERIMA -> DIVERIFIKASI -> DIKERJAKAN -> SELESAI, ditambah DITOLAK.
 */
object StatusTransitionRules {

    private val allowedTransitions: Map<ReportStatus, Set<ReportStatus>> = mapOf(
        ReportStatus.DITERIMA to setOf(ReportStatus.DIVERIFIKASI, ReportStatus.DITOLAK),
        ReportStatus.DIVERIFIKASI to setOf(ReportStatus.DIKERJAKAN, ReportStatus.DITOLAK),
        ReportStatus.DIKERJAKAN to setOf(ReportStatus.SELESAI),
        ReportStatus.SELESAI to emptySet(),
        ReportStatus.DITOLAK to emptySet()
    )

    /**
     * Memeriksa apakah perubahan status dari [from] ke [to] diizinkan.
     */
    fun canTransition(from: ReportStatus, to: ReportStatus): Boolean {
        return allowedTransitions[from]?.contains(to) == true
    }

    /**
     * Mengembalikan daftar status berikutnya yang valid dari status [current].
     */
    fun nextValidStatuses(current: ReportStatus): Set<ReportStatus> {
        return allowedTransitions[current] ?: emptySet()
    }

    /**
     * Menghitung jumlah laporan untuk setiap [ReportStatus] (digunakan pada Dasbor Admin).
     */
    fun countByStatus(reports: List<lat.pam.laporkampus.data.model.Report>): Map<ReportStatus, Int> {
        val counts = ReportStatus.entries.associateWith { 0 }.toMutableMap()
        for (report in reports) {
            val status = report.statusEnum
            counts[status] = (counts[status] ?: 0) + 1
        }
        return counts
    }
}
