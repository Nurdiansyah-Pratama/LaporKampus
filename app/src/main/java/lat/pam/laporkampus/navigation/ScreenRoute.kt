package lat.pam.laporkampus.navigation

import lat.pam.laporkampus.data.model.UserRole

/**
 * Definisi rute navigasi utama aplikasi LaporKampus dan aturan akses per peran (Route Guard).
 */
sealed class ScreenRoute(val route: String) {
    data object Login : ScreenRoute("login")
    data object Register : ScreenRoute("register")
    data object CreateReport : ScreenRoute("create_report")
    data object MyReports : ScreenRoute("my_reports")
    data object ReportDetail : ScreenRoute("report_detail/{reportId}") {
        fun createRoute(reportId: String): String = "report_detail/$reportId"
    }
    data object AdminDashboard : ScreenRoute("admin_dashboard")
    data object TechnicianTasks : ScreenRoute("technician_tasks")
    data object SatgasReports : ScreenRoute("satgas_reports")
    data object Profile : ScreenRoute("profile")
}

/**
 * Penjaga rute (Route Guard) terpusat berdasarkan peran pengguna (`MAHASISWA`, `ADMIN`, `TEKNISI`, `SATGAS`).
 * Dapat diuji langsung di Unit Test tanpa ketergantungan UI.
 */
object RoleGuard {

    private val roleAllowedRoutes: Map<UserRole, Set<String>> = mapOf(
        UserRole.MAHASISWA to setOf(
            ScreenRoute.CreateReport.route,
            ScreenRoute.MyReports.route,
            ScreenRoute.ReportDetail.route,
            ScreenRoute.Profile.route
        ),
        UserRole.ADMIN to setOf(
            ScreenRoute.AdminDashboard.route,
            ScreenRoute.SatgasReports.route,
            ScreenRoute.ReportDetail.route,
            ScreenRoute.Profile.route
        ),
        UserRole.TEKNISI to setOf(
            ScreenRoute.TechnicianTasks.route,
            ScreenRoute.ReportDetail.route,
            ScreenRoute.Profile.route
        ),
        UserRole.SATGAS to setOf(
            ScreenRoute.SatgasReports.route,
            ScreenRoute.AdminDashboard.route,
            ScreenRoute.ReportDetail.route,
            ScreenRoute.Profile.route
        )
    )

    /**
     * Mengembalikan rute beranda awal (start destination setelah login) sesuai peran pengguna.
     */
    fun defaultHomeRouteFor(role: UserRole): String {
        return when (role) {
            UserRole.MAHASISWA -> ScreenRoute.MyReports.route
            UserRole.ADMIN -> ScreenRoute.AdminDashboard.route
            UserRole.TEKNISI -> ScreenRoute.TechnicianTasks.route
            UserRole.SATGAS -> ScreenRoute.SatgasReports.route
        }
    }

    /**
     * Memeriksa apakah pengguna dengan [role] berhak mengakses [route].
     */
    fun canAccess(role: UserRole, route: String): Boolean {
        val normalizedRoute = if (route.startsWith("report_detail/")) {
            ScreenRoute.ReportDetail.route
        } else {
            route
        }
        return roleAllowedRoutes[role]?.contains(normalizedRoute) == true
    }
}
