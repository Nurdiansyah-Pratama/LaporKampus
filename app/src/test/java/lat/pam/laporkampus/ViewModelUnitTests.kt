package lat.pam.laporkampus

import android.net.Uri
import lat.pam.laporkampus.data.model.Report
import lat.pam.laporkampus.data.model.ReportPriority
import lat.pam.laporkampus.data.model.ReportStatus
import lat.pam.laporkampus.data.model.UserProfile
import lat.pam.laporkampus.data.model.UserRole
import lat.pam.laporkampus.viewmodel.AdminViewModel
import lat.pam.laporkampus.viewmodel.AuthViewModel
import lat.pam.laporkampus.viewmodel.CreateReportViewModel
import lat.pam.laporkampus.viewmodel.SatgasViewModel
import lat.pam.laporkampus.viewmodel.TechnicianViewModel
import lat.pam.laporkampus.viewmodel.TrackingViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Unit test untuk seluruh ViewModel utama LaporKampus (Sprint 6):
 * - [AuthViewModel]
 * - [CreateReportViewModel]
 * - [AdminViewModel]
 * - [TechnicianViewModel]
 * - [SatgasViewModel]
 * - [TrackingViewModel]
 */
class ViewModelUnitTests {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun authViewModel_registerAlwaysAssignsMahasiswaRole_andValidatesInputs() {
        val fakeAuthRepo = FakeAuthRepository()
        val viewModel = AuthViewModel(authRepository = fakeAuthRepo)

        // Input tidak valid: nama kosong, email salah, password kurang dari 6 karakter
        viewModel.register(nama = " ", email = "bukan-email", password = "123", confirmPassword = "456")
        val errorState = viewModel.uiState.value
        assertNotNull(errorState.nameError)
        assertNotNull(errorState.emailError)
        assertNotNull(errorState.passwordError)
        assertNotNull(errorState.confirmPasswordError)
        assertNull(errorState.currentUser)

        // Input valid: registrasi berhasil dan selalu mendapat peran MAHASISWA
        viewModel.register(
            nama = "Siti Aminah",
            email = "siti@mahasiswa.kampus.ac.id",
            password = "password123",
            confirmPassword = "password123"
        )
        val successState = viewModel.uiState.value
        assertNotNull(successState.currentUser)
        assertEquals(UserRole.MAHASISWA, successState.currentUser?.roleEnum)
        assertEquals("Siti Aminah", successState.currentUser?.nama)

        // Logout membersihkan sesi pengguna
        viewModel.logout()
        assertNull(viewModel.uiState.value.currentUser)
    }

    @Test
    fun createReportViewModel_validatesAndSubmitsWithInitialDiterimaAndHistory() {
        val fakeReportRepo = FakeReportRepository()
        val viewModel = CreateReportViewModel(reportRepository = fakeReportRepo)

        // Deskripsi terlalu pendek (< 10 karakter) harus ditolak
        viewModel.onDeskripsiChanged("Pendek")
        viewModel.submitReport(reporterId = "mhs-1", reporterName = "Budi")
        assertNotNull(viewModel.uiState.value.descriptionError)
        assertFalse(viewModel.uiState.value.isSubmitSuccess)

        // Kategori yang mengandung kata "Sensitif" otomatis mencentang isSensitive = true
        viewModel.onKategoriChanged("Keluhan Sensitif / Satgas PPKS")
        assertTrue(viewModel.uiState.value.isSensitive)

        // Isi deskripsi & lokasi manual, lalu kirim
        viewModel.onSensitiveChanged(false)
        viewModel.onKategoriChanged("Kelistrikan & Pencahayaan")
        viewModel.onDeskripsiChanged("Lampu proyektor dan AC ruang kuliah R.302 mati total.")
        viewModel.onNamaLokasiChanged("Gedung Kuliah Umum Lt 3 R.302")
        viewModel.submitReport(reporterId = "mhs-1", reporterName = "Budi")

        assertTrue(viewModel.uiState.value.isSubmitSuccess)
        assertNull(viewModel.uiState.value.submitErrorMessage)

        // Pastikan riwayat awal DITERIMA juga tercatat di repository
        val history = fakeReportRepo.getHistoryFor("rep-1")
        assertEquals(1, history.size)
        assertEquals(ReportStatus.DITERIMA.name, history.first().status)
    }

    @Test
    fun adminViewModel_excludesSensitiveReports_countsByStatus_andEnforcesValidTransitions() {
        val initialReports = listOf(
            Report(
                id = "rep-normal-1",
                reporterId = "mhs-1",
                kategori = "Sanitasi & Air",
                isSensitive = false,
                status = ReportStatus.DITERIMA.name
            ),
            Report(
                id = "rep-sensitif-1",
                reporterId = "mhs-2",
                kategori = "Keluhan Sensitif / Satgas PPKS",
                isSensitive = true,
                status = ReportStatus.DITERIMA.name
            )
        )
        val fakeRepo = FakeReportRepository(initialReports = initialReports)
        val adminViewModel = AdminViewModel(reportRepository = fakeRepo)
        adminViewModel.startObserving()

        // Admin hanya melihat laporan non-sensitif (1 laporan)
        val state = adminViewModel.uiState.value
        assertEquals(1, state.allReports.size)
        assertEquals(1, state.statusCounts[ReportStatus.DITERIMA])

        val report = state.allReports.first()

        // Mencoba loncat dari DITERIMA langsung ke SELESAI harus ditolak oleh aturan transisi
        adminViewModel.updateReport(
            report = report,
            newStatus = ReportStatus.SELESAI,
            newPriority = ReportPriority.TINGGI,
            assignedTo = "Teknisi Andi",
            catatan = "Langsung selesai",
            adminName = "Admin Kampus"
        )
        assertNotNull(adminViewModel.uiState.value.errorMessage)

        // Transisi sah: DITERIMA -> DIVERIFIKASI dengan catatan & penugasan teknisi
        adminViewModel.updateReport(
            report = report,
            newStatus = ReportStatus.DIVERIFIKASI,
            newPriority = ReportPriority.TINGGI,
            assignedTo = "Teknisi Andi",
            catatan = "Laporan telah diverifikasi dan ditugaskan ke Teknisi Andi.",
            adminName = "Admin Kampus"
        )
        assertNull(adminViewModel.uiState.value.errorMessage)
        assertNotNull(adminViewModel.uiState.value.updateSuccessMessage)
        assertEquals(ReportStatus.DIVERIFIKASI, adminViewModel.uiState.value.allReports.first().statusEnum)
        assertEquals("Teknisi Andi", adminViewModel.uiState.value.allReports.first().assignedTo)
    }

    @Test
    fun technicianViewModel_requiresProofPhotoAndNoteBeforeMarkingSelesai() {
        val taskReport = Report(
            id = "rep-tech-1",
            reporterId = "mhs-1",
            kategori = "Kelistrikan & Pencahayaan",
            isSensitive = false,
            status = ReportStatus.DIKERJAKAN.name,
            assignedTo = "tech-01 - Teknisi Andi"
        )
        val fakeRepo = FakeReportRepository(initialReports = listOf(taskReport))
        val techViewModel = TechnicianViewModel(reportRepository = fakeRepo)
        techViewModel.startObservingTasks(technicianId = "tech-01", technicianName = "Teknisi Andi")

        assertEquals(1, techViewModel.uiState.value.tasks.size)

        // Tanpa foto bukti harus ditolak
        techViewModel.completeTask(
            report = taskReport,
            proofPhotoUris = emptyList(),
            catatan = "Sudah diperbaiki",
            technicianName = "Teknisi Andi"
        )
        assertNotNull(techViewModel.uiState.value.errorMessage)

        // Dengan foto bukti tetapi tanpa catatan harus ditolak
        val dummyUri = Uri.EMPTY
        techViewModel.completeTask(
            report = taskReport,
            proofPhotoUris = listOf(dummyUri),
            catatan = "   ",
            technicianName = "Teknisi Andi"
        )
        assertNotNull(techViewModel.uiState.value.errorMessage)

        // Dengan foto bukti dan catatan valid, status berubah menjadi SELESAI
        techViewModel.completeTask(
            report = taskReport,
            proofPhotoUris = listOf(dummyUri),
            catatan = "Lampu dan ballast telah diganti dengan unit baru.",
            technicianName = "Teknisi Andi"
        )
        assertNull(techViewModel.uiState.value.errorMessage)
        assertNotNull(techViewModel.uiState.value.completionSuccessMessage)
        assertEquals(ReportStatus.SELESAI, techViewModel.uiState.value.tasks.first().statusEnum)
    }

    @Test
    fun satgasViewModel_onlyObservesSensitiveReports_andUpdatesStatusValidly() {
        val initialReports = listOf(
            Report(id = "rep-biasa", isSensitive = false, status = ReportStatus.DITERIMA.name),
            Report(id = "rep-sensitif", isSensitive = true, status = ReportStatus.DITERIMA.name)
        )
        val fakeRepo = FakeReportRepository(initialReports = initialReports)
        val satgasViewModel = SatgasViewModel(reportRepository = fakeRepo)
        satgasViewModel.startObservingSensitiveReports()

        assertEquals(1, satgasViewModel.uiState.value.allSensitiveReports.size)
        val sensitiveReport = satgasViewModel.uiState.value.allSensitiveReports.first()
        assertEquals("rep-sensitif", sensitiveReport.id)

        satgasViewModel.updateSensitiveReport(
            report = sensitiveReport,
            newStatus = ReportStatus.DIVERIFIKASI,
            newPriority = ReportPriority.DARURAT,
            catatan = "Kasus diterima oleh Satgas untuk pendampingan lebih lanjut.",
            satgasName = "Petugas Satgas"
        )
        assertNull(satgasViewModel.uiState.value.errorMessage)
        assertEquals(
            ReportStatus.DIVERIFIKASI,
            satgasViewModel.uiState.value.allSensitiveReports.first().statusEnum
        )
    }

    @Test
    fun trackingViewModel_emitsStatusChangeEvent_andSubmitsRatingWhenSelesai() {
        val initialReport = Report(
            id = "rep-track-1",
            reporterId = "mhs-1",
            kategori = "Fasilitas Kelas & Laboratorium",
            namaLokasi = "Lab Komputer 2",
            status = ReportStatus.DITERIMA.name
        )
        val fakeRepo = FakeReportRepository(initialReports = listOf(initialReport))
        val trackingViewModel = TrackingViewModel(reportRepository = fakeRepo)

        trackingViewModel.startObservingMyReports("mhs-1")
        assertNull(trackingViewModel.myReportsState.value.statusChangeEvent)

        // Simulasikan perubahan status laporan menjadi SELESAI secara real-time
        val completedReport = initialReport.copy(status = ReportStatus.SELESAI.name)
        fakeRepo.emitReports(listOf(completedReport))

        // Pastikan event notifikasi perubahan status terpicu
        val event = trackingViewModel.myReportsState.value.statusChangeEvent
        assertNotNull(event)
        assertEquals("rep-track-1", event?.reportId)
        assertEquals(ReportStatus.SELESAI.label, event?.newStatusLabel)

        trackingViewModel.consumeStatusChangeEvent()
        assertNull(trackingViewModel.myReportsState.value.statusChangeEvent)

        // Uji pemberian rating 1-5 setelah status SELESAI
        trackingViewModel.startObservingReportDetail("rep-track-1")
        trackingViewModel.submitRating(
            report = completedReport,
            nilai = 5,
            komentar = "Penanganan sangat cepat dan rapi, terima kasih!"
        )
        val detailState = trackingViewModel.detailState.value
        assertNotNull(detailState.existingRating)
        assertEquals(5, detailState.existingRating?.nilai)
        assertNotNull(detailState.ratingSuccessMessage)
    }

    @Test
    fun seedAdminLogin_1237050139_logsInAsAdmin_andCanUpdateProgressStages() {
        val fakeAuthRepo = FakeAuthRepository()
        val authViewModel = AuthViewModel(authRepository = fakeAuthRepo)

        // Login menggunakan ID seed 1237050139 dan password admin123
        authViewModel.login("1237050139", "admin123")
        val loggedInUser = authViewModel.uiState.value.currentUser
        assertNotNull(loggedInUser)
        assertEquals(UserRole.ADMIN, loggedInUser?.roleEnum)

        val initialReport = Report(
            id = "rep-progres-1",
            reporterId = "mhs-1",
            kategori = "Air & Sanitasi (Toilet)",
            status = ReportStatus.DITERIMA.name
        )
        val fakeReportRepo = FakeReportRepository(initialReports = listOf(initialReport))
        val adminViewModel = AdminViewModel(reportRepository = fakeReportRepo)
        adminViewModel.startObserving()

        // Tahap 1: Ubah ke Sedang Ditinjau (DIVERIFIKASI)
        adminViewModel.updateReportProgress(
            report = adminViewModel.uiState.value.allReports.first(),
            newStatus = ReportStatus.DIVERIFIKASI,
            catatan = "",
            adminName = "Petugas 1237050139"
        )
        assertEquals(ReportStatus.DIVERIFIKASI, adminViewModel.uiState.value.allReports.first().statusEnum)
        assertEquals("Sedang Ditinjau", adminViewModel.uiState.value.allReports.first().statusEnum.label)

        // Tahap 2: Ubah ke Sedang Diproses (DIKERJAKAN)
        adminViewModel.updateReportProgress(
            report = adminViewModel.uiState.value.allReports.first(),
            newStatus = ReportStatus.DIKERJAKAN,
            catatan = "",
            adminName = "Petugas 1237050139"
        )
        assertEquals(ReportStatus.DIKERJAKAN, adminViewModel.uiState.value.allReports.first().statusEnum)
        assertEquals("Sedang Diproses", adminViewModel.uiState.value.allReports.first().statusEnum.label)

        // Tahap 3: Ubah ke Sudah Selesai (SELESAI)
        adminViewModel.updateReportProgress(
            report = adminViewModel.uiState.value.allReports.first(),
            newStatus = ReportStatus.SELESAI,
            catatan = "",
            adminName = "Petugas 1237050139"
        )
        assertEquals(ReportStatus.SELESAI, adminViewModel.uiState.value.allReports.first().statusEnum)
        assertEquals("Sudah Selesai", adminViewModel.uiState.value.allReports.first().statusEnum.label)
    }
}
