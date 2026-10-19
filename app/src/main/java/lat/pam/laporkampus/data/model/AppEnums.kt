package lat.pam.laporkampus.data.model

/**
 * Peran pengguna di dalam sistem LaporKampus.
 * Registrasi mandiri dari aplikasi hanya diperbolehkan menggunakan peran MAHASISWA.
 */
enum class UserRole(val label: String) {
    MAHASISWA("Mahasiswa"),
    ADMIN("Admin"),
    TEKNISI("Teknisi"),
    SATGAS("Satgas");

    companion object {
        fun fromString(value: String?): UserRole {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: MAHASISWA
        }
    }
}

/**
 * Status siklus hidup laporan fasilitas/keluhan kampus.
 */
enum class ReportStatus(val label: String) {
    DITERIMA("Diterima"),
    DIVERIFIKASI("Diverifikasi"),
    DIKERJAKAN("Dikerjakan"),
    SELESAI("Selesai"),
    DITOLAK("Ditolak");

    companion object {
        fun fromString(value: String?): ReportStatus {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: DITERIMA
        }
    }
}

/**
 * Tingkat prioritas penanganan laporan.
 */
enum class ReportPriority(val label: String) {
    RENDAH("Rendah"),
    SEDANG("Sedang"),
    TINGGI("Tinggi"),
    DARURAT("Darurat");

    companion object {
        fun fromString(value: String?): ReportPriority {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: SEDANG
        }
    }
}
