package lat.pam.laporkampus

import lat.pam.laporkampus.data.model.ReportStatus
import lat.pam.laporkampus.data.model.UserRole
import lat.pam.laporkampus.navigation.RoleGuard
import lat.pam.laporkampus.navigation.ScreenRoute
import lat.pam.laporkampus.util.AuthInputValidator
import lat.pam.laporkampus.util.StatusTransitionRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun validStatusTransitions_areAllowed() {
        assertTrue(StatusTransitionRules.canTransition(ReportStatus.DITERIMA, ReportStatus.DIVERIFIKASI))
        assertTrue(StatusTransitionRules.canTransition(ReportStatus.DITERIMA, ReportStatus.DITOLAK))
        assertTrue(StatusTransitionRules.canTransition(ReportStatus.DIVERIFIKASI, ReportStatus.DIKERJAKAN))
        assertTrue(StatusTransitionRules.canTransition(ReportStatus.DIKERJAKAN, ReportStatus.SELESAI))
    }

    @Test
    fun invalidStatusTransitions_areBlocked() {
        assertFalse(StatusTransitionRules.canTransition(ReportStatus.DITERIMA, ReportStatus.SELESAI))
        assertFalse(StatusTransitionRules.canTransition(ReportStatus.SELESAI, ReportStatus.DIKERJAKAN))
        assertFalse(StatusTransitionRules.canTransition(ReportStatus.DITOLAK, ReportStatus.DIVERIFIKASI))
    }

    @Test
    fun authInputValidator_validatesEmailAndPassword() {
        assertTrue(AuthInputValidator.isValidName("Budi Santoso"))
        assertFalse(AuthInputValidator.isValidName(" "))
        assertTrue(AuthInputValidator.isValidEmail("mahasiswa@kampus.ac.id"))
        assertFalse(AuthInputValidator.isValidEmail("email-tanpa-domain"))
        assertTrue(AuthInputValidator.isValidPassword("rahasia123"))
        assertFalse(AuthInputValidator.isValidPassword("12345"))
        assertTrue(AuthInputValidator.doPasswordsMatch("rahasia123", "rahasia123"))
        assertFalse(AuthInputValidator.doPasswordsMatch("rahasia123", "berbeda"))
    }

    @Test
    fun roleGuard_restrictsRoutesByRole() {
        assertEquals(ScreenRoute.MyReports.route, RoleGuard.defaultHomeRouteFor(UserRole.MAHASISWA))
        assertEquals(ScreenRoute.AdminDashboard.route, RoleGuard.defaultHomeRouteFor(UserRole.ADMIN))
        assertEquals(ScreenRoute.TechnicianTasks.route, RoleGuard.defaultHomeRouteFor(UserRole.TEKNISI))
        assertEquals(ScreenRoute.SatgasReports.route, RoleGuard.defaultHomeRouteFor(UserRole.SATGAS))

        assertTrue(RoleGuard.canAccess(UserRole.MAHASISWA, ScreenRoute.CreateReport.route))
        assertFalse(RoleGuard.canAccess(UserRole.MAHASISWA, ScreenRoute.AdminDashboard.route))
        assertTrue(RoleGuard.canAccess(UserRole.ADMIN, ScreenRoute.AdminDashboard.route))
        assertFalse(RoleGuard.canAccess(UserRole.ADMIN, ScreenRoute.TechnicianTasks.route))
        assertTrue(RoleGuard.canAccess(UserRole.SATGAS, ScreenRoute.SatgasReports.route))
    }

    @Test
    fun reportFormValidator_validatesCategoryDescriptionAndFallbackLocation() {
        assertTrue(lat.pam.laporkampus.util.ReportFormValidator.isValidCategory("Kelistrikan & Pencahayaan"))
        assertFalse(lat.pam.laporkampus.util.ReportFormValidator.isValidCategory("   "))
        assertTrue(lat.pam.laporkampus.util.ReportFormValidator.isValidDescription("Lampu koridor gedung B mati total sejak kemarin"))
        assertFalse(lat.pam.laporkampus.util.ReportFormValidator.isValidDescription("Rusak"))

        // Valid jika ada nama lokasi manual meskipun koordinat null (fallback peta/GPS gagal)
        assertTrue(lat.pam.laporkampus.util.ReportFormValidator.isValidLocation("Gedung A Lt 2", null, null))
        // Valid jika ada koordinat peta meskipun nama lokasi manual kosong
        assertTrue(lat.pam.laporkampus.util.ReportFormValidator.isValidLocation("", -6.8915, 107.6107))
        // Tidak valid jika keduanya kosong
        assertFalse(lat.pam.laporkampus.util.ReportFormValidator.isValidLocation(" ", null, null))
    }

    @Test
    fun dateAndPrivacyUtils_hidesAnonymousReporterFromAdminAndTechnician() {
        val anonymousReport = lat.pam.laporkampus.data.model.Report(
            id = "rep-01",
            reporterId = "uid-mahasiswa-123",
            isAnonymous = true
        )
        // Admin dan Teknisi tidak boleh melihat reporterId asli jika isAnonymous = true
        val forAdmin = lat.pam.laporkampus.util.DateAndPrivacyUtils.getVisibleReporterLabel(
            report = anonymousReport,
            viewerRole = UserRole.ADMIN,
            viewerUserId = "uid-admin-1"
        )
        val forTech = lat.pam.laporkampus.util.DateAndPrivacyUtils.getVisibleReporterLabel(
            report = anonymousReport,
            viewerRole = UserRole.TEKNISI,
            viewerUserId = "uid-tech-1"
        )
        assertFalse(forAdmin.contains("uid-mahasiswa-123"))
        assertFalse(forTech.contains("uid-mahasiswa-123"))
    }

    @Test
    fun statusTransitionRules_countsReportsByStatusAccurately() {
        val sampleReports = listOf(
            lat.pam.laporkampus.data.model.Report(id = "1", status = ReportStatus.DITERIMA.name),
            lat.pam.laporkampus.data.model.Report(id = "2", status = ReportStatus.DITERIMA.name),
            lat.pam.laporkampus.data.model.Report(id = "3", status = ReportStatus.DIVERIFIKASI.name),
            lat.pam.laporkampus.data.model.Report(id = "4", status = ReportStatus.SELESAI.name)
        )
        val counts = StatusTransitionRules.countByStatus(sampleReports)
        assertEquals(2, counts[ReportStatus.DITERIMA])
        assertEquals(1, counts[ReportStatus.DIVERIFIKASI])
        assertEquals(0, counts[ReportStatus.DIKERJAKAN])
        assertEquals(1, counts[ReportStatus.SELESAI])
        assertEquals(0, counts[ReportStatus.DITOLAK])
    }
}