package com.example.faceattendance.presentation.common.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.example.faceattendance.data.local.db.entity.UserRole
import com.example.faceattendance.presentation.admin.home.AdminHomeScreen
import com.example.faceattendance.presentation.auth.LoginScreen
import com.example.faceattendance.presentation.auth.LoginViewModel
import com.example.faceattendance.presentation.teacher.home.TeacherHomeScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = NavRoute.Login.route
    ) {
        composable(NavRoute.Login.route) {
            val loginViewModel: LoginViewModel = hiltViewModel()
            LoginScreen(
                viewModel = loginViewModel,
                onLoginSuccess = { role ->
                    when (role) {
                        UserRole.ADMIN -> {
                            navController.navigate(NavRoute.AdminRoot.route) {
                                popUpTo(NavRoute.Login.route) { inclusive = true }
                            }
                        }
                        UserRole.TEACHER -> {
                            navController.navigate(NavRoute.TeacherRoot.route) {
                                popUpTo(NavRoute.Login.route) { inclusive = true }
                            }
                        }
                    }
                }
            )
        }

        composable(NavRoute.TeacherRoot.route) {
            TeacherRootContainer(
                onLogout = {
                    navController.navigate(NavRoute.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(NavRoute.AdminRoot.route) {
            AdminRootContainer(
                onLogout = {
                    navController.navigate(NavRoute.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}

@Composable
fun TeacherRootContainer(onLogout: () -> Unit) {
    val teacherNavController = rememberNavController()
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = {
                        selectedTab = 0
                        teacherNavController.navigate(NavRoute.TeacherHome.route) {
                            popUpTo(NavRoute.TeacherHome.route) { inclusive = true }
                        }
                    },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Trang chủ") },
                    label = { Text("Trang chủ") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Assessment, contentDescription = "Báo cáo") },
                    label = { Text("Báo cáo") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Cài đặt") },
                    label = { Text("Cài đặt") }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedTab) {
                0 -> {
                    NavHost(
                        navController = teacherNavController,
                        startDestination = NavRoute.TeacherHome.route
                    ) {
                        composable(NavRoute.TeacherHome.route) {
                            val homeViewModel: com.example.faceattendance.presentation.teacher.home.TeacherHomeViewModel = hiltViewModel()
                            com.example.faceattendance.presentation.teacher.home.TeacherHomeScreen(
                                viewModel = homeViewModel,
                                onCreateSessionClick = { teacherNavController.navigate(NavRoute.CreateSession.route) },
                                onSessionClick = { sessionId ->
                                    teacherNavController.navigate(NavRoute.SessionDetail.createRoute(sessionId))
                                },
                                onManageStudentsClick = { teacherNavController.navigate(NavRoute.TeacherStudents.route) }
                            )
                        }

                        composable(NavRoute.CreateSession.route) {
                            val createViewModel: com.example.faceattendance.presentation.teacher.session.CreateSessionViewModel = hiltViewModel()
                            com.example.faceattendance.presentation.teacher.session.CreateSessionScreen(
                                viewModel = createViewModel,
                                onNavigateBack = { teacherNavController.popBackStack() },
                                onSessionCreated = { sessionId ->
                                    teacherNavController.navigate(NavRoute.SessionDetail.createRoute(sessionId)) {
                                        popUpTo(NavRoute.TeacherHome.route)
                                    }
                                }
                            )
                        }

                        composable(
                            route = NavRoute.SessionDetail.route,
                            arguments = listOf(androidx.navigation.navArgument("sessionId") {
                                type = androidx.navigation.NavType.LongType
                            })
                        ) { backStackEntry ->
                            val sessionId = backStackEntry.arguments?.getLong("sessionId") ?: 1L
                            val detailViewModel: com.example.faceattendance.presentation.teacher.session.SessionDetailViewModel = hiltViewModel()
                            com.example.faceattendance.presentation.teacher.session.SessionDetailScreen(
                                viewModel = detailViewModel,
                                onNavigateBack = { teacherNavController.popBackStack() },
                                onOpenClassCamera = {
                                    val classId = detailViewModel.uiState.value.sessionDetails?.session?.classId ?: 1L
                                    teacherNavController.navigate(NavRoute.PanoramaAttendance.createRoute(sessionId, classId))
                                },
                                onOpenSingleCamera = {
                                    val classId = detailViewModel.uiState.value.sessionDetails?.session?.classId ?: 1L
                                    teacherNavController.navigate(NavRoute.SingleAttendance.createRoute(sessionId, classId))
                                },
                                onOpenManualAttendance = {
                                    val classId = detailViewModel.uiState.value.sessionDetails?.session?.classId ?: 1L
                                    teacherNavController.navigate(NavRoute.ManualAttendance.createRoute(sessionId, classId))
                                }
                            )
                        }

                        composable(NavRoute.TeacherStudents.route) {
                            val studentViewModel: com.example.faceattendance.presentation.teacher.students.StudentViewModel = hiltViewModel()
                            com.example.faceattendance.presentation.teacher.students.StudentManagementScreen(
                                viewModel = studentViewModel,
                                onNavigateBack = { teacherNavController.popBackStack() },
                                onStudentClick = { studentId ->
                                    teacherNavController.navigate(NavRoute.StudentDetail.createRoute(studentId))
                                }
                            )
                        }

                        composable(
                            route = NavRoute.StudentDetail.route,
                            arguments = listOf(androidx.navigation.navArgument("studentId") {
                                type = androidx.navigation.NavType.LongType
                            })
                        ) { backStackEntry ->
                            val studentId = backStackEntry.arguments?.getLong("studentId") ?: 1L
                            val studentViewModel: com.example.faceattendance.presentation.teacher.students.StudentViewModel = hiltViewModel()
                            com.example.faceattendance.presentation.teacher.students.StudentDetailScreen(
                                studentId = studentId,
                                viewModel = studentViewModel,
                                onNavigateBack = { teacherNavController.popBackStack() },
                                onEnrollFaceClick = { id, name, mssv ->
                                    teacherNavController.navigate(NavRoute.FaceEnrollment.createRoute(id, name, mssv))
                                }
                            )
                        }

                        composable(
                            route = NavRoute.FaceEnrollment.route,
                            arguments = listOf(
                                androidx.navigation.navArgument("studentId") {
                                    type = androidx.navigation.NavType.LongType
                                },
                                androidx.navigation.navArgument("name") {
                                    type = androidx.navigation.NavType.StringType
                                    defaultValue = ""
                                },
                                androidx.navigation.navArgument("mssv") {
                                    type = androidx.navigation.NavType.StringType
                                    defaultValue = ""
                                }
                            )
                        ) { backStackEntry ->
                            val studentId = backStackEntry.arguments?.getLong("studentId") ?: 1L
                            val studentName = backStackEntry.arguments?.getString("name") ?: ""
                            val studentMssv = backStackEntry.arguments?.getString("mssv") ?: ""
                            val enrollViewModel: com.example.faceattendance.presentation.teacher.face_enrollment.FaceEnrollmentViewModel = hiltViewModel()
                            com.example.faceattendance.presentation.teacher.face_enrollment.FaceEnrollmentScreen(
                                studentId = studentId,
                                studentName = studentName,
                                studentMssv = studentMssv,
                                viewModel = enrollViewModel,
                                onNavigateBack = { teacherNavController.popBackStack() },
                                onEnrollSuccess = { teacherNavController.popBackStack() }
                            )
                        }

                        composable(
                            route = NavRoute.PanoramaAttendance.route,
                            arguments = listOf(
                                androidx.navigation.navArgument("sessionId") { type = androidx.navigation.NavType.LongType },
                                androidx.navigation.navArgument("classId") { type = androidx.navigation.NavType.LongType }
                            )
                        ) { backStackEntry ->
                            val sessionId = backStackEntry.arguments?.getLong("sessionId") ?: 1L
                            val classId = backStackEntry.arguments?.getLong("classId") ?: 1L
                            val panoramaViewModel: com.example.faceattendance.presentation.teacher.attendance.PanoramaAttendanceViewModel = hiltViewModel()
                            com.example.faceattendance.presentation.teacher.attendance.PanoramaAttendanceScreen(
                                sessionId = sessionId,
                                classId = classId,
                                viewModel = panoramaViewModel,
                                onNavigateBack = { teacherNavController.popBackStack() },
                                onSuccessFinished = { teacherNavController.popBackStack() }
                            )
                        }

                        composable(
                            route = NavRoute.SingleAttendance.route,
                            arguments = listOf(
                                androidx.navigation.navArgument("sessionId") { type = androidx.navigation.NavType.LongType },
                                androidx.navigation.navArgument("classId") { type = androidx.navigation.NavType.LongType }
                            )
                        ) { backStackEntry ->
                            val sessionId = backStackEntry.arguments?.getLong("sessionId") ?: 1L
                            val classId = backStackEntry.arguments?.getLong("classId") ?: 1L
                            val singleViewModel: com.example.faceattendance.presentation.teacher.attendance.SingleAttendanceViewModel = hiltViewModel()
                            com.example.faceattendance.presentation.teacher.attendance.SingleAttendanceScreen(
                                sessionId = sessionId,
                                classId = classId,
                                viewModel = singleViewModel,
                                onNavigateBack = { teacherNavController.popBackStack() }
                            )
                        }

                        composable(
                            route = NavRoute.ManualAttendance.route,
                            arguments = listOf(
                                androidx.navigation.navArgument("sessionId") { type = androidx.navigation.NavType.LongType },
                                androidx.navigation.navArgument("classId") { type = androidx.navigation.NavType.LongType }
                            )
                        ) { backStackEntry ->
                            val sessionId = backStackEntry.arguments?.getLong("sessionId") ?: 1L
                            val classId = backStackEntry.arguments?.getLong("classId") ?: 1L
                            val manualViewModel: com.example.faceattendance.presentation.teacher.attendance.ManualAttendanceViewModel = hiltViewModel()
                            com.example.faceattendance.presentation.teacher.attendance.ManualAttendanceScreen(
                                sessionId = sessionId,
                                classId = classId,
                                viewModel = manualViewModel,
                                onNavigateBack = { teacherNavController.popBackStack() }
                            )
                        }
                    }
                }
                1 -> {
                    val reportsViewModel: com.example.faceattendance.presentation.teacher.reports.TeacherReportsViewModel = hiltViewModel()
                    com.example.faceattendance.presentation.teacher.reports.TeacherReportsScreen(
                        viewModel = reportsViewModel,
                        onNavigateBack = { selectedTab = 0 }
                    )
                }
                2 -> {
                    val settingsViewModel: com.example.faceattendance.presentation.settings.SettingsViewModel = hiltViewModel()
                    com.example.faceattendance.presentation.settings.SettingsScreen(
                        viewModel = settingsViewModel,
                        onNavigateBack = { selectedTab = 0 },
                        onLoggedOut = onLogout
                    )
                }
            }
        }
    }
}

@Composable
fun AdminRootContainer(onLogout: () -> Unit) {
    val adminNavController = rememberNavController()
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = {
                        selectedTab = 0
                        adminNavController.navigate(NavRoute.AdminHome.route) {
                            popUpTo(NavRoute.AdminHome.route) { inclusive = true }
                        }
                    },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Tổng quan") },
                    label = { Text("Tổng quan") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Assessment, contentDescription = "Báo cáo") },
                    label = { Text("Báo cáo") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Cài đặt") },
                    label = { Text("Cài đặt") }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedTab) {
                0 -> {
                    NavHost(
                        navController = adminNavController,
                        startDestination = NavRoute.AdminHome.route
                    ) {
                        composable(NavRoute.AdminHome.route) {
                            AdminHomeScreen(
                                onNavigateToImport = { adminNavController.navigate(NavRoute.AdminImport.route) },
                                onNavigateToUsers = { adminNavController.navigate(NavRoute.AdminTeachers.route) },
                                onNavigateToClasses = { adminNavController.navigate(NavRoute.AdminClasses.route) },
                                onNavigateToCourses = { adminNavController.navigate(NavRoute.AdminCourses.route) },
                                onNavigateToAssignments = { adminNavController.navigate(NavRoute.AdminAssignments.route) },
                                onNavigateToReports = { selectedTab = 1 },
                                onNavigateToBackup = { adminNavController.navigate(NavRoute.AdminBackup.route) },
                                onNavigateToSettings = { selectedTab = 2 }
                            )
                        }

                        composable(NavRoute.AdminImport.route) {
                            val importViewModel: com.example.faceattendance.presentation.admin.import_data.ImportViewModel = hiltViewModel()
                            com.example.faceattendance.presentation.admin.import_data.ImportDataScreen(
                                viewModel = importViewModel,
                                onNavigateBack = { adminNavController.popBackStack() }
                            )
                        }

                        composable(NavRoute.AdminTeachers.route) {
                            val teacherViewModel: com.example.faceattendance.presentation.admin.teachers.TeacherViewModel = hiltViewModel()
                            com.example.faceattendance.presentation.admin.teachers.TeacherManagementScreen(
                                viewModel = teacherViewModel,
                                onNavigateBack = { adminNavController.popBackStack() }
                            )
                        }

                        composable(NavRoute.AdminClasses.route) {
                            val classViewModel: com.example.faceattendance.presentation.admin.classes.ClassViewModel = hiltViewModel()
                            com.example.faceattendance.presentation.admin.classes.ClassManagementScreen(
                                viewModel = classViewModel,
                                onNavigateBack = { adminNavController.popBackStack() }
                            )
                        }

                        composable(NavRoute.AdminCourses.route) {
                            val courseViewModel: com.example.faceattendance.presentation.admin.courses.CourseViewModel = hiltViewModel()
                            com.example.faceattendance.presentation.admin.courses.CourseManagementScreen(
                                viewModel = courseViewModel,
                                onNavigateBack = { adminNavController.popBackStack() }
                            )
                        }

                        composable(NavRoute.AdminAssignments.route) {
                            val assignmentViewModel: com.example.faceattendance.presentation.admin.assignment.AssignmentViewModel = hiltViewModel()
                            com.example.faceattendance.presentation.admin.assignment.TeacherAssignmentScreen(
                                viewModel = assignmentViewModel,
                                onNavigateBack = { adminNavController.popBackStack() }
                            )
                        }

                        composable(NavRoute.AdminBackup.route) {
                            val backupViewModel: com.example.faceattendance.presentation.admin.backup.BackupViewModel = hiltViewModel()
                            com.example.faceattendance.presentation.admin.backup.BackupManagementScreen(
                                viewModel = backupViewModel,
                                onNavigateBack = { adminNavController.popBackStack() }
                            )
                        }
                    }
                }
                1 -> {
                    val adminReportsViewModel: com.example.faceattendance.presentation.admin.reports.AdminReportsViewModel = hiltViewModel()
                    com.example.faceattendance.presentation.admin.reports.AdminReportsScreen(
                        viewModel = adminReportsViewModel,
                        onNavigateBack = { selectedTab = 0 }
                    )
                }
                2 -> {
                    val settingsViewModel: com.example.faceattendance.presentation.settings.SettingsViewModel = hiltViewModel()
                    com.example.faceattendance.presentation.settings.SettingsScreen(
                        viewModel = settingsViewModel,
                        onNavigateBack = { selectedTab = 0 },
                        onLoggedOut = onLogout
                    )
                }
            }
        }
    }
}
