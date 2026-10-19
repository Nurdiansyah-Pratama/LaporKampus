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
}