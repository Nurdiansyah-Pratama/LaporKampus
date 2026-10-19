package lat.pam.laporkampus.util

import com.google.firebase.Timestamp
import lat.pam.laporkampus.data.model.Report
import lat.pam.laporkampus.data.model.UserRole
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utilitas pemformatan tanggal/waktu Bahasa Indonesia dan aturan privasi tampilan pelapor.
 */
object DateAndPrivacyUtils {

    private const val PATTERN_DATETIME = "dd MMM yyyy, HH:mm"

    fun formatTimestamp(timestamp: Timestamp?): String {
        if (timestamp == null) return "-"
        return formatEpochMillis(timestamp.toDate().time)
    }

    fun formatEpochMillis(epochMillis: Long): String {
        val formatter = SimpleDateFormat(PATTERN_DATETIME, Locale.forLanguageTag("id-ID"))
        return formatter.format(Date(epochMillis))
    }

    /**
     * Aturan Keamanan #2:
     * Jika `isAnonymous = true`, `reporterId` tidak boleh terlihat oleh ADMIN atau TEKNISI di UI.
     * Hanya pelapor itu sendiri yang mengetahui bahwa laporan anonim tersebut adalah miliknya.
     */
    fun getVisibleReporterLabel(
        report: Report,
        viewerRole: UserRole,
        viewerUserId: String
    ): String {
        if (report.isAnonymous) {
            return if (report.reporterId == viewerUserId) {
                "Anda (Dikirim sebagai Anonim)"
            } else {
                "Pelapor Anonim (Identitas Disembunyikan)"
            }
        }
        return when (viewerRole) {
            UserRole.MAHASISWA -> if (report.reporterId == viewerUserId) "Anda" else report.reporterId
            UserRole.ADMIN, UserRole.TEKNISI, UserRole.SATGAS -> report.reporterId
        }
    }
}
