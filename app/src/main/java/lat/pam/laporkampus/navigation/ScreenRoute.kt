package lat.pam.laporkampus.navigation

/**
 * Definisi rute navigasi utama aplikasi LaporKampus.
 */
sealed class ScreenRoute(val route: String) {
    data object Login : ScreenRoute("login")
    data object Register : ScreenRoute("register")
    data object MahasiswaHome : ScreenRoute("mahasiswa_home")
    data object CreateReport : ScreenRoute("create_report")
    data object MyReports : ScreenRoute("my_reports")
    data object ReportDetail : ScreenRoute("report_detail/{reportId}") {
        fun createRoute(reportId: String): String = "report_detail/$reportId"
    }
    data object AdminDashboard : ScreenRoute("admin_dashboard")
    data object TechnicianTasks : ScreenRoute("technician_tasks")
    data object SatgasReports : ScreenRoute("satgas_reports")
}
