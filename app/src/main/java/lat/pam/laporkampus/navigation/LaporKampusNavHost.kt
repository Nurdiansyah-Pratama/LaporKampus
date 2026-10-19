package lat.pam.laporkampus.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import lat.pam.laporkampus.R
import lat.pam.laporkampus.data.model.UserProfile
import lat.pam.laporkampus.data.model.UserRole
import lat.pam.laporkampus.ui.auth.LoginScreen
import lat.pam.laporkampus.ui.auth.RegisterScreen
import lat.pam.laporkampus.ui.components.AccessDeniedScreen
import lat.pam.laporkampus.ui.components.FeaturePlaceholderScreen
import lat.pam.laporkampus.ui.components.ProfileScreen
import lat.pam.laporkampus.viewmodel.AuthViewModel

/**
 * Definisi item pada Bottom Navigation yang disesuaikan menurut peran pengguna.
 */
data class BottomNavItem(
    val route: String,
    @param:StringRes val labelRes: Int,
    val icon: ImageVector
)

fun bottomNavItemsForRole(role: UserRole): List<BottomNavItem> {
    return when (role) {
        UserRole.MAHASISWA -> listOf(
            BottomNavItem(
                route = ScreenRoute.MyReports.route,
                labelRes = R.string.nav_my_reports,
                icon = Icons.AutoMirrored.Filled.Assignment
            ),
            BottomNavItem(
                route = ScreenRoute.CreateReport.route,
                labelRes = R.string.nav_create_report,
                icon = Icons.Default.AddCircleOutline
            ),
            BottomNavItem(
                route = ScreenRoute.Profile.route,
                labelRes = R.string.nav_profile,
                icon = Icons.Default.Person
            )
        )
        UserRole.ADMIN -> listOf(
            BottomNavItem(
                route = ScreenRoute.AdminDashboard.route,
                labelRes = R.string.nav_admin_dashboard,
                icon = Icons.Default.AdminPanelSettings
            ),
            BottomNavItem(
                route = ScreenRoute.Profile.route,
                labelRes = R.string.nav_profile,
                icon = Icons.Default.Person
            )
        )
        UserRole.TEKNISI -> listOf(
            BottomNavItem(
                route = ScreenRoute.TechnicianTasks.route,
                labelRes = R.string.nav_technician_tasks,
                icon = Icons.Default.Build
            ),
            BottomNavItem(
                route = ScreenRoute.Profile.route,
                labelRes = R.string.nav_profile,
                icon = Icons.Default.Person
            )
        )
        UserRole.SATGAS -> listOf(
            BottomNavItem(
                route = ScreenRoute.SatgasReports.route,
                labelRes = R.string.nav_satgas_reports,
                icon = Icons.Default.Security
            ),
            BottomNavItem(
                route = ScreenRoute.Profile.route,
                labelRes = R.string.nav_profile,
                icon = Icons.Default.Person
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LaporKampusNavHost(
    authViewModel: AuthViewModel = viewModel(),
    navController: NavHostController = rememberNavController()
) {
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()
    val currentUser = authState.currentUser

    if (authState.isCheckingSession) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Sinkronisasi otomatis antara status login & rute aktif
    LaunchedEffect(currentUser, currentRoute) {
        val isOnAuthRoute = currentRoute == ScreenRoute.Login.route ||
            currentRoute == ScreenRoute.Register.route

        if (currentUser == null && !isOnAuthRoute && currentRoute != null) {
            navController.navigate(ScreenRoute.Login.route) {
                popUpTo(0) { inclusive = true }
            }
        } else if (currentUser != null && isOnAuthRoute) {
            val targetHome = RoleGuard.defaultHomeRouteFor(currentUser.roleEnum)
            navController.navigate(targetHome) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    val bottomItems = if (currentUser != null) {
        bottomNavItemsForRole(currentUser.roleEnum)
    } else {
        emptyList()
    }
    val showBars = currentUser != null &&
        currentRoute != ScreenRoute.Login.route &&
        currentRoute != ScreenRoute.Register.route

    Scaffold(
        topBar = {
            if (showBars) {
                TopAppBar(
                    title = {
                        Text(
                            text = "${stringResource(id = R.string.app_name)} • ${currentUser.roleEnum.label}"
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        },
        bottomBar = {
            if (showBars && bottomItems.isNotEmpty()) {
                NavigationBar {
                    bottomItems.forEach { item ->
                        val selected = currentRoute == item.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (!selected) {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = stringResource(id = item.labelRes)
                                )
                            },
                            label = { Text(text = stringResource(id = item.labelRes)) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        val startDest = if (currentUser != null) {
            RoleGuard.defaultHomeRouteFor(currentUser.roleEnum)
        } else {
            ScreenRoute.Login.route
        }

        NavHost(
            navController = navController,
            startDestination = startDest,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(ScreenRoute.Login.route) {
                LoginScreen(
                    uiState = authState,
                    onLoginClick = { email, password ->
                        authViewModel.login(email, password)
                    },
                    onNavigateToRegister = {
                        authViewModel.clearErrors()
                        navController.navigate(ScreenRoute.Register.route)
                    }
                )
            }

            composable(ScreenRoute.Register.route) {
                RegisterScreen(
                    uiState = authState,
                    onRegisterClick = { nama, email, password, confirmPassword ->
                        authViewModel.register(nama, email, password, confirmPassword)
                    },
                    onNavigateToLogin = {
                        authViewModel.clearErrors()
                        navController.popBackStack()
                    }
                )
            }

            composable(ScreenRoute.MyReports.route) {
                GuardedScreen(
                    currentUser = currentUser,
                    targetRoute = ScreenRoute.MyReports.route,
                    navController = navController
                ) { user ->
                    FeaturePlaceholderScreen(
                        title = stringResource(id = R.string.nav_my_reports),
                        description = stringResource(id = R.string.placeholder_my_reports_desc),
                        role = user.roleEnum
                    )
                }
            }

            composable(ScreenRoute.CreateReport.route) {
                GuardedScreen(
                    currentUser = currentUser,
                    targetRoute = ScreenRoute.CreateReport.route,
                    navController = navController
                ) { user ->
                    FeaturePlaceholderScreen(
                        title = stringResource(id = R.string.nav_create_report),
                        description = stringResource(id = R.string.placeholder_create_report_desc),
                        role = user.roleEnum
                    )
                }
            }

            composable(ScreenRoute.AdminDashboard.route) {
                GuardedScreen(
                    currentUser = currentUser,
                    targetRoute = ScreenRoute.AdminDashboard.route,
                    navController = navController
                ) { user ->
                    FeaturePlaceholderScreen(
                        title = stringResource(id = R.string.nav_admin_dashboard),
                        description = stringResource(id = R.string.placeholder_admin_desc),
                        role = user.roleEnum
                    )
                }
            }

            composable(ScreenRoute.TechnicianTasks.route) {
                GuardedScreen(
                    currentUser = currentUser,
                    targetRoute = ScreenRoute.TechnicianTasks.route,
                    navController = navController
                ) { user ->
                    FeaturePlaceholderScreen(
                        title = stringResource(id = R.string.nav_technician_tasks),
                        description = stringResource(id = R.string.placeholder_technician_desc),
                        role = user.roleEnum
                    )
                }
            }

            composable(ScreenRoute.SatgasReports.route) {
                GuardedScreen(
                    currentUser = currentUser,
                    targetRoute = ScreenRoute.SatgasReports.route,
                    navController = navController
                ) { user ->
                    FeaturePlaceholderScreen(
                        title = stringResource(id = R.string.nav_satgas_reports),
                        description = stringResource(id = R.string.placeholder_satgas_desc),
                        role = user.roleEnum
                    )
                }
            }

            composable(ScreenRoute.Profile.route) {
                GuardedScreen(
                    currentUser = currentUser,
                    targetRoute = ScreenRoute.Profile.route,
                    navController = navController
                ) { user ->
                    ProfileScreen(
                        userProfile = user,
                        onLogoutClick = {
                            authViewModel.logout()
                        }
                    )
                }
            }
        }
    }
}

/**
 * Komponen pembungkus penjaga rute (Route Guard) yang memastikan hanya peran yang diizinkan
 * oleh [RoleGuard] yang dapat melihat konten layar.
 */
@Composable
private fun GuardedScreen(
    currentUser: UserProfile?,
    targetRoute: String,
    navController: NavHostController,
    content: @Composable (UserProfile) -> Unit
) {
    if (currentUser == null) return
    val role = currentUser.roleEnum
    if (RoleGuard.canAccess(role, targetRoute)) {
        content(currentUser)
    } else {
        AccessDeniedScreen(
            currentRole = role,
            onBackToRoleHome = {
                val home = RoleGuard.defaultHomeRouteFor(role)
                navController.navigate(home) {
                    popUpTo(0) { inclusive = true }
                }
            }
        )
    }
}
