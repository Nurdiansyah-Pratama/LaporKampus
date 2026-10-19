package lat.pam.laporkampus.util

/**
 * Validator murni untuk form pembuatan laporan agar mudah diuji lewat Unit Test.
 */
object ReportFormValidator {
    const val MIN_DESCRIPTION_LENGTH = 10

    fun isValidCategory(kategori: String): Boolean {
        return kategori.trim().isNotEmpty()
    }

    fun isValidDescription(deskripsi: String): Boolean {
        return deskripsi.trim().length >= MIN_DESCRIPTION_LENGTH
    }

    /**
     * Lokasi dianggap sah apabila pengguna mengisi nama lokasi manual
     * ATAU memilih koordinat lat/lng di peta.
     */
    fun isValidLocation(namaLokasi: String, lat: Double?, lng: Double?): Boolean {
        val hasManualName = namaLokasi.trim().isNotEmpty()
        val hasCoordinates = lat != null && lng != null
        return hasManualName || hasCoordinates
    }

    /**
     * Memvalidasi bahwa nilai rating berada pada rentang 1 sampai 5 (inklusif)
     * dan laporan berstatus `SELESAI`.
     */
    fun canSubmitRating(
        status: lat.pam.laporkampus.data.model.ReportStatus,
        nilai: Int
    ): Boolean {
        return status == lat.pam.laporkampus.data.model.ReportStatus.SELESAI && nilai in 1..5
    }
}
