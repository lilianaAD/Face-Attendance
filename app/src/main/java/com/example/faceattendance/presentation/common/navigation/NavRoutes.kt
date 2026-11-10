package com.example.faceattendance.presentation.common.navigation

sealed class NavRoute(val route: String) {
    object Splash : NavRoute("splash")
    object Login : NavRoute("login")
    object TeacherRoot : NavRoute("teacher_root")
    object AdminRoot : NavRoute("admin_root")

    // Teacher subroutes
    object TeacherHome : NavRoute("teacher_home")
    object CreateSession : NavRoute("create_session")
    object TeacherStudents : NavRoute("teacher_students")
    object TeacherReports : NavRoute("teacher_reports")
    object TeacherSettings : NavRoute("teacher_settings")
    object SessionDetail : NavRoute("session_detail/{sessionId}") {
        fun createRoute(sessionId: Long) = "session_detail/$sessionId"
    }
    object StudentDetail : NavRoute("student_detail/{studentId}") {
        fun createRoute(studentId: Long) = "student_detail/$studentId"
    }
    object FaceEnrollment : NavRoute("face_enrollment/{studentId}?name={name}&mssv={mssv}") {
        fun createRoute(studentId: Long, name: String = "", mssv: String = "") = 
            "face_enrollment/$studentId?name=$name&mssv=$mssv"
    }
    object PanoramaAttendance : NavRoute("panorama_attendance/{sessionId}/{classId}") {
        fun createRoute(sessionId: Long, classId: Long) = "panorama_attendance/$sessionId/$classId"
    }
    object SingleAttendance : NavRoute("single_attendance/{sessionId}/{classId}") {
        fun createRoute(sessionId: Long, classId: Long) = "single_attendance/$sessionId/$classId"
    }
    object ManualAttendance : NavRoute("manual_attendance/{sessionId}/{classId}") {
        fun createRoute(sessionId: Long, classId: Long) = "manual_attendance/$sessionId/$classId"
    }

    // Admin subroutes
    object AdminHome : NavRoute("admin_home")
    object AdminImport : NavRoute("admin_import")
    object AdminTeachers : NavRoute("admin_teachers")
    object AdminClasses : NavRoute("admin_classes")
    object AdminCourses : NavRoute("admin_courses")
    object AdminAssignments : NavRoute("admin_assignments")
    object AdminReports : NavRoute("admin_reports")
    object AdminBackup : NavRoute("admin_backup")
    object AdminSettings : NavRoute("admin_settings")
    object Settings : NavRoute("settings")
}
