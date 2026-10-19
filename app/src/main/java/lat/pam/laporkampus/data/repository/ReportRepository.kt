package lat.pam.laporkampus.data.repository

import android.net.Uri
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import lat.pam.laporkampus.data.firebase.FirestoreCollections
import lat.pam.laporkampus.data.model.Report
import lat.pam.laporkampus.data.model.ReportHistory
import lat.pam.laporkampus.data.model.ReportPriority
import lat.pam.laporkampus.data.model.ReportStatus
import java.util.UUID

/**
 * Kontrak repositori untuk pengelolaan laporan fasilitas dan keluhan kampus.
 */
interface ReportRepository {
    suspend fun createReport(
        reporterId: String,
        reporterName: String,
        kategori: String,
        deskripsi: String,
        isAnonymous: Boolean,
        isSensitive: Boolean,
        photoUris: List<Uri>,
        lat: Double?,
        lng: Double?,
        namaLokasi: String
    ): Result<Report>
}

/**
 * Implementasi [ReportRepository] menggunakan Firebase Storage dan Cloud Firestore.
 */
class FirebaseReportRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) : ReportRepository {

    override suspend fun createReport(
        reporterId: String,
        reporterName: String,
        kategori: String,
        deskripsi: String,
        isAnonymous: Boolean,
        isSensitive: Boolean,
        photoUris: List<Uri>,
        lat: Double?,
        lng: Double?,
        namaLokasi: String
    ): Result<Report> {
        return try {
            val reportDocRef = firestore.collection(FirestoreCollections.REPORTS).document()
            val reportId = reportDocRef.id

            // 1. Unggah seluruh foto bukti (jika ada) ke Firebase Storage
            val uploadedPhotoUrls = mutableListOf<String>()
            for ((index, uri) in photoUris.withIndex()) {
                val fileName = "${reportId}_${index}_${UUID.randomUUID()}.jpg"
                val photoRef = storage.reference
                    .child(FirestoreCollections.STORAGE_REPORT_PHOTOS)
                    .child(reportId)
                    .child(fileName)

                photoRef.putFile(uri).await()
                val downloadUrl = photoRef.downloadUrl.await().toString()
                uploadedPhotoUrls.add(downloadUrl)
            }

            val now = Timestamp.now()
            val resolvedLocationName = namaLokasi.trim().ifBlank {
                if (lat != null && lng != null) {
                    String.format(java.util.Locale.US, "Titik Peta (%.5f, %.5f)", lat, lng)
                } else {
                    "Lokasi Kampus"
                }
            }

            // 2. Siapkan dokumen laporan utama dengan status awal DITERIMA
            val report = Report(
                id = reportId,
                reporterId = reporterId,
                isAnonymous = isAnonymous,
                isSensitive = isSensitive,
                kategori = kategori.trim(),
                deskripsi = deskripsi.trim(),
                photoUrls = uploadedPhotoUrls,
                lat = lat,
                lng = lng,
                namaLokasi = resolvedLocationName,
                status = ReportStatus.DITERIMA.name,
                prioritas = ReportPriority.SEDANG.name,
                assignedTo = "",
                createdAt = now,
                updatedAt = now
            )

            // 3. Siapkan dokumen catatan awal di subkoleksi `reports/{id}/history`
            val historyDocRef = reportDocRef
                .collection(FirestoreCollections.HISTORY)
                .document()

            val actorLabel = if (isAnonymous) "Pelapor Anonim" else reporterName.ifBlank { reporterId }
            val initialHistory = ReportHistory(
                id = historyDocRef.id,
                status = ReportStatus.DITERIMA.name,
                catatan = "Laporan baru dikirim oleh pelapor dan menunggu verifikasi.",
                oleh = actorLabel,
                waktu = now
            )

            // 4. Simpan laporan dan catatan history awal secara atomik menggunakan WriteBatch
            firestore.runBatch { batch ->
                batch.set(reportDocRef, report.toFirestoreMap())
                batch.set(historyDocRef, initialHistory.toFirestoreMap())
            }.await()

            Result.success(report)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
