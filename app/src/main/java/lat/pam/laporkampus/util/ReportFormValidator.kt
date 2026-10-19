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
}
