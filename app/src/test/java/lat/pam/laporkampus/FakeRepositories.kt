package lat.pam.laporkampus

import android.net.Uri
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import lat.pam.laporkampus.data.model.Rating
import lat.pam.laporkampus.data.model.Report
import lat.pam.laporkampus.data.model.ReportHistory
import lat.pam.laporkampus.data.model.ReportPriority
import lat.pam.laporkampus.data.model.ReportStatus
import lat.pam.laporkampus.data.model.UserProfile
import lat.pam.laporkampus.data.model.UserRole
import lat.pam.laporkampus.data.repository.AuthRepository
import lat.pam.laporkampus.data.repository.ReportRepository
import lat.pam.laporkampus.util.ReportFormValidator
import lat.pam.laporkampus.util.StatusTransitionRules

/**
 * Implementasi tiruan (Fake) dari [AuthRepository] untuk pengujian unit [lat.pam.laporkampus.viewmodel.AuthViewModel].
 */
class FakeAuthRepository(
    initialUser: UserProfile? = null
) : AuthRepository {

    var activeUser: UserProfile? = initialUser
    var shouldFailAuth: Boolean = false
    var failureMessage: String = "Kredensial tidak valid"

    override val currentUserId: String?
        get() = activeUser?.id

    override suspend fun getCurrentUserProfile(): Result<UserProfile?> {
        return if (shouldFailAuth) {
            Result.failure(IllegalStateException(failureMessage))
        } else {
            Result.success(activeUser)
        }
    }

    override suspend fun login(email: String, password: String): Result<UserProfile> {
        if (shouldFailAuth) {
            return Result.failure(IllegalArgumentException(failureMessage))
        }
        if (lat.pam.laporkampus.util.AuthInputValidator.isSeedStaffCredentials(email, password)) {
            val seedProfile = UserProfile(
                id = lat.pam.laporkampus.util.AuthInputValidator.SEED_STAFF_ID,
                nama = lat.pam.laporkampus.util.AuthInputValidator.SEED_STAFF_NAME,
                email = lat.pam.laporkampus.util.AuthInputValidator.SEED_STAFF_EMAIL,
                peran = UserRole.ADMIN.name
            )
            activeUser = seedProfile
            return Result.success(seedProfile)
        }
        val profile = activeUser ?: UserProfile(
            id = "uid-login-1",
            nama = "Pengguna Uji",
            email = email.trim(),
            peran = UserRole.MAHASISWA.name
        )
        activeUser = profile
        return Result.success(profile)
    }

    override suspend fun registerMahasiswa(
        nama: String,
        email: String,
        password: String
    ): Result<UserProfile> {
        if (shouldFailAuth) {
            return Result.failure(IllegalArgumentException(failureMessage))
        }
        // Sesuai Aturan Keamanan #1: Registrasi dari aplikasi HANYA membuat peran MAHASISWA
        val newProfile = UserProfile(
            id = "uid-mhs-baru",
            nama = nama.trim(),
            email = email.trim(),
            peran = UserRole.MAHASISWA.name
        )
        activeUser = newProfile
        return Result.success(newProfile)
    }

    override fun logout() {
        activeUser = null
    }
}

/**
 * Implementasi tiruan (Fake) in-memory dari [ReportRepository] untuk menguji seluruh ViewModel
 * tanpa koneksi ke Firebase sesungguhnya.
 */
class FakeReportRepository(
    initialReports: List<Report> = emptyList(),
    initialTechnicians: List<UserProfile> = emptyList()
) : ReportRepository {

    private val reportsFlow = MutableStateFlow(initialReports)
    private val historyMap = mutableMapOf<String, MutableStateFlow<List<ReportHistory>>>()
    private val ratingsMap = mutableMapOf<String, MutableStateFlow<Rating?>>()
    private val techniciansFlow = MutableStateFlow(initialTechnicians)

    var shouldFailNextOperation: Boolean = false
    var customErrorMessage: String = "Operasi repositori gagal"

    fun emitReports(newReports: List<Report>) {
        reportsFlow.value = newReports
    }

    fun getHistoryFor(reportId: String): List<ReportHistory> {
        return historyMap[reportId]?.value.orEmpty()
    }

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
        if (shouldFailNextOperation) {
            return Result.failure(IllegalStateException(customErrorMessage))
        }
        val id = "rep-${reportsFlow.value.size + 1}"
        val now = Timestamp.now()
        val report = Report(
            id = id,
            reporterId = reporterId,
            isAnonymous = isAnonymous,
            isSensitive = isSensitive,
            kategori = kategori,
            deskripsi = deskripsi,
            photoUrls = photoUris.map { it.toString() },
            lat = lat,
            lng = lng,
            namaLokasi = namaLokasi,
            status = ReportStatus.DITERIMA.name,
            prioritas = ReportPriority.SEDANG.name,
            assignedTo = "",
            createdAt = now,
            updatedAt = now
        )
        val initialHistory = ReportHistory(
            id = "hist-1",
            status = ReportStatus.DITERIMA.name,
            catatan = "Laporan berhasil dibuat dan diterima oleh sistem.",
            oleh = if (isAnonymous) "Pelapor Anonim" else reporterName,
            waktu = now
        )
        reportsFlow.value = reportsFlow.value + report
        historyMap[id] = MutableStateFlow(listOf(initialHistory))
        return Result.success(report)
    }

    override fun observeMyReports(reporterId: String): Flow<List<Report>> {
        return reportsFlow.map { list -> list.filter { it.reporterId == reporterId } }
    }

    override fun observeReportById(reportId: String): Flow<Report?> {
        return reportsFlow.map { list -> list.find { it.id == reportId } }
    }

    override fun observeReportHistory(reportId: String): Flow<List<ReportHistory>> {
        return historyMap.getOrPut(reportId) { MutableStateFlow(emptyList()) }
    }

    override fun observeAdminReports(): Flow<List<Report>> {
        // Sesuai Aturan Keamanan #2: Admin tidak boleh membaca laporan sensitif
        return reportsFlow.map { list -> list.filter { !it.isSensitive } }
    }

    override fun observeTechnicians(): Flow<List<UserProfile>> {
        return techniciansFlow
    }

    override fun observeTechnicianTasks(technicianId: String, technicianName: String): Flow<List<Report>> {
        return reportsFlow.map { list ->
            list.filter { report ->
                !report.isSensitive &&
                    (report.assignedTo.contains(technicianId, ignoreCase = true) ||
                        (technicianName.isNotBlank() && report.assignedTo.contains(technicianName, ignoreCase = true)))
            }
        }
    }

    override fun observeSensitiveReportsForSatgas(): Flow<List<Report>> {
        return reportsFlow.map { list -> list.filter { it.isSensitive } }
    }

    override fun observeRatingForReport(reportId: String): Flow<Rating?> {
        return ratingsMap.getOrPut(reportId) { MutableStateFlow(null) }
    }

    override suspend fun updateReportByAdmin(
        report: Report,
        newStatus: ReportStatus,
        newPriority: ReportPriority,
        assignedTo: String,
        catatan: String,
        adminName: String
    ): Result<Unit> {
        if (shouldFailNextOperation) {
            return Result.failure(IllegalStateException(customErrorMessage))
        }
        val currentStatus = report.statusEnum
        if (newStatus != currentStatus && !StatusTransitionRules.canTransition(currentStatus, newStatus)) {
            return Result.failure(IllegalArgumentException("Transisi tidak sah"))
        }
        val updated = report.copy(
            status = newStatus.name,
            prioritas = newPriority.name,
            assignedTo = assignedTo.trim(),
            updatedAt = Timestamp.now()
        )
        reportsFlow.value = reportsFlow.value.map { if (it.id == report.id) updated else it }
        val histFlow = historyMap.getOrPut(report.id) { MutableStateFlow(emptyList()) }
        histFlow.value = histFlow.value + ReportHistory(
            id = "hist-${histFlow.value.size + 1}",
            status = newStatus.name,
            catatan = catatan.trim(),
            oleh = "$adminName (Admin)",
            waktu = Timestamp.now()
        )
        return Result.success(Unit)
    }

    override suspend fun completeTaskByTechnician(
        report: Report,
        completionPhotoUris: List<Uri>,
        catatan: String,
        technicianName: String
    ): Result<Unit> {
        if (shouldFailNextOperation) {
            return Result.failure(IllegalStateException(customErrorMessage))
        }
        val currentStatus = report.statusEnum
        if (!StatusTransitionRules.canTransition(currentStatus, ReportStatus.SELESAI)) {
            return Result.failure(IllegalArgumentException("Transisi ke SELESAI tidak sah"))
        }
        val updated = report.copy(
            status = ReportStatus.SELESAI.name,
            photoUrls = report.photoUrls + completionPhotoUris.map { "https://storage.example/bukti_${it}" },
            updatedAt = Timestamp.now()
        )
        reportsFlow.value = reportsFlow.value.map { if (it.id == report.id) updated else it }
        val histFlow = historyMap.getOrPut(report.id) { MutableStateFlow(emptyList()) }
        histFlow.value = histFlow.value + ReportHistory(
            id = "hist-${histFlow.value.size + 1}",
            status = ReportStatus.SELESAI.name,
            catatan = catatan.trim(),
            oleh = "$technicianName (Teknisi)",
            waktu = Timestamp.now()
        )
        return Result.success(Unit)
    }

    override suspend fun updateSensitiveReportBySatgas(
        report: Report,
        newStatus: ReportStatus,
        newPriority: ReportPriority,
        catatan: String,
        satgasName: String
    ): Result<Unit> {
        if (shouldFailNextOperation) {
            return Result.failure(IllegalStateException(customErrorMessage))
        }
        if (!report.isSensitive) {
            return Result.failure(IllegalArgumentException("Bukan laporan sensitif"))
        }
        val currentStatus = report.statusEnum
        if (newStatus != currentStatus && !StatusTransitionRules.canTransition(currentStatus, newStatus)) {
            return Result.failure(IllegalArgumentException("Transisi tidak sah"))
        }
        val updated = report.copy(
            status = newStatus.name,
            prioritas = newPriority.name,
            updatedAt = Timestamp.now()
        )
        reportsFlow.value = reportsFlow.value.map { if (it.id == report.id) updated else it }
        val histFlow = historyMap.getOrPut(report.id) { MutableStateFlow(emptyList()) }
        histFlow.value = histFlow.value + ReportHistory(
            id = "hist-${histFlow.value.size + 1}",
            status = newStatus.name,
            catatan = catatan.trim(),
            oleh = "$satgasName (Satgas)",
            waktu = Timestamp.now()
        )
        return Result.success(Unit)
    }

    override suspend fun submitRating(
        report: Report,
        nilai: Int,
        komentar: String
    ): Result<Rating> {
        if (shouldFailNextOperation) {
            return Result.failure(IllegalStateException(customErrorMessage))
        }
        if (!ReportFormValidator.canSubmitRating(report.statusEnum, nilai)) {
            return Result.failure(IllegalArgumentException("Penilaian tidak valid"))
        }
        val rating = Rating(
            reportId = report.id,
            nilai = nilai,
            komentar = komentar.trim()
        )
        ratingsMap.getOrPut(report.id) { MutableStateFlow(null) }.value = rating
        return Result.success(rating)
    }
}
