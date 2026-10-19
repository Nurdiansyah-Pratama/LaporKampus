package lat.pam.laporkampus.data.model

import com.google.firebase.Timestamp

/**
 * Daftar kategori laporan fasilitas dan keluhan kampus.
 */
object ReportCategories {
    val ALL = listOf(
        "Fasilitas Kelas & Laboratorium",
        "Kelistrikan & Pencahayaan",
        "Air & Sanitasi (Toilet)",
        "Jaringan Internet / Wi-Fi Kampus",
        "Kebersihan & Taman",
        "Keamanan & Ketertiban",
        "Kekerasan / Pelecehan / Perundungan (Sensitif)",
        "Lainnya"
    )
}

/**
 * Model dokumen pada koleksi `reports/{id}` di Cloud Firestore.
 */
data class Report(
    val id: String = "",
    val reporterId: String = "",
    val isAnonymous: Boolean = false,
    val isSensitive: Boolean = false,
    val kategori: String = "",
    val deskripsi: String = "",
    val photoUrls: List<String> = emptyList(),
    val lat: Double? = null,
    val lng: Double? = null,
    val namaLokasi: String = "",
    val status: String = ReportStatus.DITERIMA.name,
    val prioritas: String = ReportPriority.SEDANG.name,
    val assignedTo: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
) {
    val statusEnum: ReportStatus
        get() = ReportStatus.fromString(status)

    val priorityEnum: ReportPriority
        get() = ReportPriority.fromString(prioritas)

    fun toFirestoreMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "reporterId" to reporterId,
        "isAnonymous" to isAnonymous,
        "isSensitive" to isSensitive,
        "kategori" to kategori,
        "deskripsi" to deskripsi,
        "photoUrls" to photoUrls,
        "lat" to lat,
        "lng" to lng,
        "namaLokasi" to namaLokasi,
        "status" to status,
        "prioritas" to prioritas,
        "assignedTo" to assignedTo,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt
    )

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromFirestoreMap(id: String, data: Map<String, Any?>?): Report {
            if (data == null) return Report(id = id)
            return Report(
                id = (data["id"] as? String)?.ifBlank { id } ?: id,
                reporterId = data["reporterId"] as? String ?: "",
                isAnonymous = data["isAnonymous"] as? Boolean ?: false,
                isSensitive = data["isSensitive"] as? Boolean ?: false,
                kategori = data["kategori"] as? String ?: "",
                deskripsi = data["deskripsi"] as? String ?: "",
                photoUrls = (data["photoUrls"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                lat = (data["lat"] as? Number)?.toDouble(),
                lng = (data["lng"] as? Number)?.toDouble(),
                namaLokasi = data["namaLokasi"] as? String ?: "",
                status = data["status"] as? String ?: ReportStatus.DITERIMA.name,
                prioritas = data["prioritas"] as? String ?: ReportPriority.SEDANG.name,
                assignedTo = data["assignedTo"] as? String ?: "",
                createdAt = data["createdAt"] as? Timestamp ?: Timestamp.now(),
                updatedAt = data["updatedAt"] as? Timestamp ?: Timestamp.now()
            )
        }
    }
}

/**
 * Model dokumen riwayat status pada subkoleksi `reports/{id}/history/{historyId}`.
 */
data class ReportHistory(
    val id: String = "",
    val status: String = ReportStatus.DITERIMA.name,
    val catatan: String = "",
    val oleh: String = "",
    val waktu: Timestamp = Timestamp.now()
) {
    val statusEnum: ReportStatus
        get() = ReportStatus.fromString(status)

    fun toFirestoreMap(): Map<String, Any> = mapOf(
        "status" to status,
        "catatan" to catatan,
        "oleh" to oleh,
        "waktu" to waktu
    )

    companion object {
        fun fromFirestoreMap(id: String, data: Map<String, Any?>?): ReportHistory {
            if (data == null) return ReportHistory(id = id)
            return ReportHistory(
                id = id,
                status = data["status"] as? String ?: ReportStatus.DITERIMA.name,
                catatan = data["catatan"] as? String ?: "",
                oleh = data["oleh"] as? String ?: "",
                waktu = data["waktu"] as? Timestamp ?: Timestamp.now()
            )
        }
    }
}

/**
 * Model dokumen penilaian pada koleksi `ratings/{reportId}` di Cloud Firestore.
 * Field: `reportId`, `nilai` (1..5), `komentar`.
 */
data class Rating(
    val reportId: String = "",
    val nilai: Int = 5,
    val komentar: String = ""
) {
    fun toFirestoreMap(): Map<String, Any> = mapOf(
        "reportId" to reportId,
        "nilai" to nilai,
        "komentar" to komentar
    )

    companion object {
        fun fromFirestoreMap(reportIdFallback: String, data: Map<String, Any?>?): Rating? {
            if (data == null) return null
            val nilaiNumber = (data["nilai"] as? Number)?.toInt() ?: return null
            return Rating(
                reportId = (data["reportId"] as? String)?.ifBlank { reportIdFallback } ?: reportIdFallback,
                nilai = nilaiNumber.coerceIn(1, 5),
                komentar = data["komentar"] as? String ?: ""
            )
        }
    }
}

