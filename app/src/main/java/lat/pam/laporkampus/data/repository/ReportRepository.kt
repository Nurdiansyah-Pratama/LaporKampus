package lat.pam.laporkampus.data.repository

import android.net.Uri
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import lat.pam.laporkampus.data.firebase.FirestoreCollections
import lat.pam.laporkampus.data.model.Report
import lat.pam.laporkampus.data.model.ReportHistory
import lat.pam.laporkampus.data.model.ReportPriority
import lat.pam.laporkampus.data.model.ReportStatus
import lat.pam.laporkampus.data.model.UserProfile
import lat.pam.laporkampus.data.model.UserRole
import lat.pam.laporkampus.util.StatusTransitionRules
import java.util.Locale
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

    /**
     * Mengamati daftar laporan milik pelapor tertentu (`reporterId`) secara real-time.
     */
    fun observeMyReports(reporterId: String): Flow<List<Report>>

    /**
     * Mengamati satu dokumen laporan berdasarkan [reportId] secara real-time.
     */
    fun observeReportById(reportId: String): Flow<Report?>

    /**
     * Mengamati subkoleksi `reports/{reportId}/history` secara real-time, diurutkan berdasarkan waktu.
     */
    fun observeReportHistory(reportId: String): Flow<List<ReportHistory>>

    /**
     * Mengamati daftar laporan non-sensitif (`isSensitive == false`) untuk Dasbor Admin secara real-time.
     * Aturan Keamanan #2: Laporan dengan `isSensitive = true` hanya boleh dibaca oleh SATGAS dan pelapornya.
     */
    fun observeAdminReports(): Flow<List<Report>>

    /**
     * Mengamati daftar pengguna dengan peran `TEKNISI` untuk mempermudah penugasan oleh Admin.
     */
    fun observeTechnicians(): Flow<List<UserProfile>>

    /**
     * Mengamati daftar tugas untuk Teknisi (laporan non-sensitif yang ditugaskan kepadanya
     * atau berstatus `DIVERIFIKASI` / `DIKERJAKAN` / `SELESAI`).
     */
    fun observeTechnicianTasks(technicianId: String, technicianName: String): Flow<List<Report>>

    /**
     * Admin memverifikasi, mengatur prioritas, menugaskan teknisi, dan/atau mengubah status laporan
     * (divalidasi dengan [StatusTransitionRules]). Mencatat perubahan di subkoleksi `history`.
     */
    suspend fun updateReportByAdmin(
        report: Report,
        newStatus: ReportStatus,
        newPriority: ReportPriority,
        assignedTo: String,
        catatan: String,
        adminName: String
    ): Result<Unit>

    /**
     * Teknisi menyelesaikan tugas perbaikan dengan mengunggah foto bukti penyelesaian ke Storage
     * dan mengubah status laporan menjadi `SELESAI` (atau `DIKERJAKAN` -> `SELESAI`) beserta catatan `history`.
     */
    suspend fun completeTaskByTechnician(
        report: Report,
        completionPhotoUris: List<Uri>,
        catatan: String,
        technicianName: String
    ): Result<Unit>
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
                    String.format(Locale.US, "Titik Peta (%.5f, %.5f)", lat, lng)
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

    override fun observeMyReports(reporterId: String): Flow<List<Report>> = callbackFlow {
        if (reporterId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection(FirestoreCollections.REPORTS)
            .whereEqualTo("reporterId", reporterId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val reports = snapshot?.documents
                    ?.map { doc -> Report.fromFirestoreMap(doc.id, doc.data) }
                    ?.sortedByDescending { it.createdAt.seconds }
                    ?: emptyList()
                trySend(reports)
            }

        awaitClose { listener.remove() }
    }

    override fun observeReportById(reportId: String): Flow<Report?> = callbackFlow {
        if (reportId.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val listener = firestore.collection(FirestoreCollections.REPORTS)
            .document(reportId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    trySend(Report.fromFirestoreMap(snapshot.id, snapshot.data))
                } else {
                    trySend(null)
                }
            }

        awaitClose { listener.remove() }
    }

    override fun observeReportHistory(reportId: String): Flow<List<ReportHistory>> = callbackFlow {
        if (reportId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection(FirestoreCollections.REPORTS)
            .document(reportId)
            .collection(FirestoreCollections.HISTORY)
            .orderBy("waktu", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val historyList = snapshot?.documents
                    ?.map { doc -> ReportHistory.fromFirestoreMap(doc.id, doc.data) }
                    ?: emptyList()
                trySend(historyList)
            }

        awaitClose { listener.remove() }
    }

    override fun observeAdminReports(): Flow<List<Report>> = callbackFlow {
        val listener = firestore.collection(FirestoreCollections.REPORTS)
            .whereEqualTo("isSensitive", false)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val reports = snapshot?.documents
                    ?.map { doc -> Report.fromFirestoreMap(doc.id, doc.data) }
                    ?.filter { !it.isSensitive }
                    ?.sortedByDescending { it.createdAt.seconds }
                    ?: emptyList()
                trySend(reports)
            }

        awaitClose { listener.remove() }
    }

    override fun observeTechnicians(): Flow<List<UserProfile>> = callbackFlow {
        val listener = firestore.collection(FirestoreCollections.USERS)
            .whereEqualTo("peran", UserRole.TEKNISI.name)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val techs = snapshot?.documents
                    ?.map { doc -> UserProfile.fromFirestoreMap(doc.id, doc.data) }
                    ?: emptyList()
                trySend(techs)
            }

        awaitClose { listener.remove() }
    }

    override fun observeTechnicianTasks(
        technicianId: String,
        technicianName: String
    ): Flow<List<Report>> = callbackFlow {
        val listener = firestore.collection(FirestoreCollections.REPORTS)
            .whereEqualTo("isSensitive", false)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val allNonSensitive = snapshot?.documents
                    ?.map { doc -> Report.fromFirestoreMap(doc.id, doc.data) }
                    ?.filter { !it.isSensitive }
                    ?: emptyList()

                // Tampilkan laporan yang ditugaskan ke teknisi ini ATAU laporan yang sudah diverifikasi/dikerjakan
                val tasks = allNonSensitive.filter { report ->
                    val assignedMatches = report.assignedTo.isNotBlank() && (
                        report.assignedTo.contains(technicianId, ignoreCase = true) ||
                            (technicianName.isNotBlank() && report.assignedTo.contains(technicianName, ignoreCase = true))
                        )
                    val actionableStatus = report.statusEnum == ReportStatus.DIVERIFIKASI ||
                        report.statusEnum == ReportStatus.DIKERJAKAN
                    assignedMatches || actionableStatus
                }.sortedByDescending { it.updatedAt.seconds }

                trySend(tasks)
            }

        awaitClose { listener.remove() }
    }

    override suspend fun updateReportByAdmin(
        report: Report,
        newStatus: ReportStatus,
        newPriority: ReportPriority,
        assignedTo: String,
        catatan: String,
        adminName: String
    ): Result<Unit> {
        val currentStatus = report.statusEnum
        if (newStatus != currentStatus && !StatusTransitionRules.canTransition(currentStatus, newStatus)) {
            return Result.failure(
                IllegalArgumentException(
                    "Transisi status dari ${currentStatus.label} ke ${newStatus.label} tidak diizinkan."
                )
            )
        }

        return try {
            val now = Timestamp.now()
            val reportRef = firestore.collection(FirestoreCollections.REPORTS).document(report.id)
            val historyRef = reportRef.collection(FirestoreCollections.HISTORY).document()

            val historyEntry = ReportHistory(
                id = historyRef.id,
                status = newStatus.name,
                catatan = catatan.trim(),
                oleh = "Admin ($adminName)",
                waktu = now
            )

            firestore.runBatch { batch ->
                batch.update(
                    reportRef,
                    mapOf(
                        "status" to newStatus.name,
                        "prioritas" to newPriority.name,
                        "assignedTo" to assignedTo.trim(),
                        "updatedAt" to now
                    )
                )
                batch.set(historyRef, historyEntry.toFirestoreMap())
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun completeTaskByTechnician(
        report: Report,
        completionPhotoUris: List<Uri>,
        catatan: String,
        technicianName: String
    ): Result<Unit> {
        val currentStatus = report.statusEnum
        // Jika laporan masih berstatus DIVERIFIKASI, izinkan teknisi langsung memproses DIVERIFIKASI -> DIKERJAKAN -> SELESAI,
        // atau jika sudah DIKERJAKAN maka transisi DIKERJAKAN -> SELESAI.
        val canProceed = StatusTransitionRules.canTransition(currentStatus, ReportStatus.SELESAI) ||
            (currentStatus == ReportStatus.DIVERIFIKASI &&
                StatusTransitionRules.canTransition(ReportStatus.DIVERIFIKASI, ReportStatus.DIKERJAKAN) &&
                StatusTransitionRules.canTransition(ReportStatus.DIKERJAKAN, ReportStatus.SELESAI))

        if (!canProceed) {
            return Result.failure(
                IllegalArgumentException(
                    "Laporan dengan status ${currentStatus.label} tidak dapat ditandai SELESAI."
                )
            )
        }

        if (completionPhotoUris.isEmpty()) {
            return Result.failure(
                IllegalArgumentException("Wajib melampirkan minimal 1 foto bukti hasil perbaikan.")
            )
        }

        return try {
            val uploadedProofUrls = mutableListOf<String>()
            for ((index, uri) in completionPhotoUris.withIndex()) {
                val fileName = "bukti_${report.id}_${index}_${UUID.randomUUID()}.jpg"
                val proofRef = storage.reference
                    .child(FirestoreCollections.STORAGE_COMPLETION_PHOTOS)
                    .child(report.id)
                    .child(fileName)

                proofRef.putFile(uri).await()
                val downloadUrl = proofRef.downloadUrl.await().toString()
                uploadedProofUrls.add(downloadUrl)
            }

            val updatedPhotos = report.photoUrls + uploadedProofUrls
            val now = Timestamp.now()
            val reportRef = firestore.collection(FirestoreCollections.REPORTS).document(report.id)

            firestore.runBatch { batch ->
                // Jika sebelumnya masih DIVERIFIKASI, tambahkan jejak DIKERJAKAN terlebih dahulu agar urutan transisi utuh
                if (currentStatus == ReportStatus.DIVERIFIKASI) {
                    val inProgressHistoryRef = reportRef.collection(FirestoreCollections.HISTORY).document()
                    val inProgressHistory = ReportHistory(
                        id = inProgressHistoryRef.id,
                        status = ReportStatus.DIKERJAKAN.name,
                        catatan = "Teknisi mulai mengerjakan perbaikan di lapangan.",
                        oleh = "Teknisi ($technicianName)",
                        waktu = now
                    )
                    batch.set(inProgressHistoryRef, inProgressHistory.toFirestoreMap())
                }

                val doneHistoryRef = reportRef.collection(FirestoreCollections.HISTORY).document()
                val doneHistory = ReportHistory(
                    id = doneHistoryRef.id,
                    status = ReportStatus.SELESAI.name,
                    catatan = "${catatan.trim()} (Dilengkapi ${uploadedProofUrls.size} foto bukti perbaikan)",
                    oleh = "Teknisi ($technicianName)",
                    waktu = now
                )

                batch.update(
                    reportRef,
                    mapOf(
                        "status" to ReportStatus.SELESAI.name,
                        "photoUrls" to updatedPhotos,
                        "updatedAt" to now
                    )
                )
                batch.set(doneHistoryRef, doneHistory.toFirestoreMap())
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
