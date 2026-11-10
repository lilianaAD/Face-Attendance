package com.example.faceattendance.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.example.faceattendance.data.local.db.AppDatabase
import com.example.faceattendance.data.local.db.entity.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton

data class BackupExportResult(
    val totalRecords: Int,
    val timestamp: Long,
    val message: String
)

data class BackupRestoreStats(
    val usersCount: Int,
    val classesCount: Int,
    val coursesCount: Int,
    val studentsCount: Int,
    val assignmentsCount: Int,
    val sessionsCount: Int,
    val faceEmbeddingsCount: Int,
    val attendancesCount: Int
)

@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    suspend fun exportBackup(outputUri: Uri, db: AppDatabase): Result<BackupExportResult> = withContext(Dispatchers.IO) {
        try {
            val users = db.userDao().getAllUsersSync()
            val classes = db.classDao().getAllClassesSync()
            val courses = db.courseDao().getAllCoursesSync()
            val students = db.studentDao().getAllStudentsSync()
            val assignments = db.teacherClassDao().getAllAssignmentsSync()
            val sessions = db.sessionDao().getAllSessions()
            val embeddings = db.faceEmbeddingDao().getAllFaceEmbeddingsSync()
            val attendances = db.attendanceDao().getAll()

            val rootJson = JSONObject().apply {
                put("app", "FaceAttendance")
                put("schemaVersion", 1)
                put("createdAt", System.currentTimeMillis())

                // Users
                val usersArray = JSONArray()
                users.forEach { u ->
                    usersArray.put(JSONObject().apply {
                        put("id", u.id)
                        put("username", u.username)
                        put("passwordHash", u.passwordHash)
                        put("role", u.role.name)
                        put("displayName", u.displayName)
                        put("email", u.email)
                        put("isActive", u.isActive)
                        put("createdAt", u.createdAt)
                    })
                }
                put("users", usersArray)

                // Classes
                val classesArray = JSONArray()
                classes.forEach { c ->
                    classesArray.put(JSONObject().apply {
                        put("id", c.id)
                        put("classCode", c.classCode)
                        put("className", c.className)
                        put("description", c.description)
                    })
                }
                put("classes", classesArray)

                // Courses
                val coursesArray = JSONArray()
                courses.forEach { co ->
                    coursesArray.put(JSONObject().apply {
                        put("id", co.id)
                        put("courseCode", co.courseCode)
                        put("courseName", co.courseName)
                        put("credits", co.credits)
                    })
                }
                put("courses", coursesArray)

                // Students
                val studentsArray = JSONArray()
                students.forEach { s ->
                    studentsArray.put(JSONObject().apply {
                        put("id", s.id)
                        put("mssv", s.mssv)
                        put("hoTen", s.hoTen)
                        put("classId", s.classId)
                        put("email", s.email)
                    })
                }
                put("students", studentsArray)

                // TeacherClasses
                val assignArray = JSONArray()
                assignments.forEach { a ->
                    assignArray.put(JSONObject().apply {
                        put("teacherId", a.teacherId)
                        put("classId", a.classId)
                    })
                }
                put("teacherClasses", assignArray)

                // Sessions
                val sessionsArray = JSONArray()
                sessions.forEach { se ->
                    sessionsArray.put(JSONObject().apply {
                        put("id", se.id)
                        put("courseId", se.courseId)
                        put("classId", se.classId)
                        put("sessionDate", se.sessionDate)
                        put("startTime", se.startTime)
                        put("endTime", se.endTime)
                        put("room", se.room)
                        put("status", se.status.name)
                        put("createdAt", se.createdAt)
                    })
                }
                put("sessions", sessionsArray)

                // FaceEmbeddings
                val embeddingsArray = JSONArray()
                embeddings.forEach { fe ->
                    embeddingsArray.put(JSONObject().apply {
                        put("id", fe.id)
                        put("studentId", fe.studentId)
                        put("pose", fe.pose)
                        put("qualityScore", fe.qualityScore.toDouble())
                        put("imagePath", fe.imagePath)
                        put("createdAt", fe.createdAt)
                        val embArr = JSONArray()
                        fe.embedding.forEach { embArr.put(it.toDouble()) }
                        put("embedding", embArr)
                    })
                }
                put("faceEmbeddings", embeddingsArray)

                // Attendances
                val attendancesArray = JSONArray()
                attendances.forEach { at ->
                    attendancesArray.put(JSONObject().apply {
                        put("id", at.id)
                        put("sessionId", at.sessionId)
                        put("studentId", at.studentId)
                        put("timestamp", at.timestamp)
                        put("confidence", at.confidence.toDouble())
                        put("method", at.method.name)
                        put("imagePath", at.imagePath)
                    })
                }
                put("attendances", attendancesArray)
            }

            val total = users.size + classes.size + courses.size + students.size +
                    assignments.size + sessions.size + embeddings.size + attendances.size

            context.contentResolver.openOutputStream(outputUri)?.use { stream ->
                OutputStreamWriter(stream, StandardCharsets.UTF_8).use { writer ->
                    writer.write(rootJson.toString(2))
                }
            }

            Result.success(
                BackupExportResult(
                    totalRecords = total,
                    timestamp = System.currentTimeMillis(),
                    message = "Đã sao lưu toàn bộ $total bản ghi cơ sở dữ liệu thành công"
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreBackup(inputUri: Uri, db: AppDatabase): Result<BackupRestoreStats> = withContext(Dispatchers.IO) {
        try {
            val jsonString = context.contentResolver.openInputStream(inputUri)?.use { stream ->
                BufferedReader(InputStreamReader(stream, StandardCharsets.UTF_8)).use { it.readText() }
            } ?: return@withContext Result.failure(Exception("Không thể đọc tệp sao lưu"))

            val rootJson = JSONObject(jsonString)
            if (!rootJson.has("schemaVersion") || !rootJson.has("users")) {
                return@withContext Result.failure(Exception("Tệp sao lưu không đúng định dạng chuẩn"))
            }

            var stats: BackupRestoreStats? = null

            db.withTransaction {
                // Parse and insert users
                val usersArr = rootJson.getJSONArray("users")
                for (i in 0 until usersArr.length()) {
                    val u = usersArr.getJSONObject(i)
                    db.userDao().insertOrUpdate(
                        UserEntity(
                            id = u.getLong("id"),
                            username = u.getString("username"),
                            passwordHash = u.getString("passwordHash"),
                            role = UserRole.valueOf(u.getString("role")),
                            displayName = u.getString("displayName"),
                            email = u.optString("email", ""),
                            isActive = u.optBoolean("isActive", true),
                            createdAt = u.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }

                // Parse and insert classes
                val classesArr = rootJson.getJSONArray("classes")
                for (i in 0 until classesArr.length()) {
                    val c = classesArr.getJSONObject(i)
                    db.classDao().insert(
                        ClassEntity(
                            id = c.getLong("id"),
                            classCode = c.getString("classCode"),
                            className = c.getString("className"),
                            description = c.optString("description", "")
                        )
                    )
                }

                // Parse and insert courses
                val coursesArr = rootJson.getJSONArray("courses")
                for (i in 0 until coursesArr.length()) {
                    val co = coursesArr.getJSONObject(i)
                    db.courseDao().insert(
                        CourseEntity(
                            id = co.getLong("id"),
                            courseCode = co.getString("courseCode"),
                            courseName = co.getString("courseName"),
                            credits = co.getInt("credits")
                        )
                    )
                }

                // Parse and insert students
                val studentsArr = rootJson.getJSONArray("students")
                for (i in 0 until studentsArr.length()) {
                    val s = studentsArr.getJSONObject(i)
                    db.studentDao().insert(
                        StudentEntity(
                            id = s.getLong("id"),
                            mssv = s.getString("mssv"),
                            hoTen = s.getString("hoTen"),
                            classId = s.getLong("classId"),
                            email = s.optString("email", "")
                        )
                    )
                }

                // Parse and insert teacher assignments
                val assignArr = rootJson.optJSONArray("teacherClasses")
                if (assignArr != null) {
                    for (i in 0 until assignArr.length()) {
                        val a = assignArr.getJSONObject(i)
                        db.teacherClassDao().insert(
                            TeacherClassEntity(
                                teacherId = a.getLong("teacherId"),
                                classId = a.getLong("classId")
                            )
                        )
                    }
                }

                // Parse and insert sessions
                val sessionsArr = rootJson.optJSONArray("sessions")
                if (sessionsArr != null) {
                    for (i in 0 until sessionsArr.length()) {
                        val se = sessionsArr.getJSONObject(i)
                        db.sessionDao().insert(
                            SessionEntity(
                                id = se.getLong("id"),
                                courseId = se.getLong("courseId"),
                                classId = se.getLong("classId"),
                                sessionDate = se.getString("sessionDate"),
                                startTime = se.getString("startTime"),
                                endTime = se.getString("endTime"),
                                room = se.getString("room"),
                                status = SessionStatus.valueOf(se.optString("status", "SCHEDULED")),
                                createdAt = se.optLong("createdAt", System.currentTimeMillis())
                            )
                        )
                    }
                }

                // Parse and insert face embeddings
                val embeddingsArr = rootJson.optJSONArray("faceEmbeddings")
                if (embeddingsArr != null) {
                    for (i in 0 until embeddingsArr.length()) {
                        val fe = embeddingsArr.getJSONObject(i)
                        val embArr = fe.getJSONArray("embedding")
                        val floatArray = FloatArray(embArr.length()) { j -> embArr.getDouble(j).toFloat() }
                        db.faceEmbeddingDao().insert(
                            FaceEmbeddingEntity(
                                id = fe.getLong("id"),
                                studentId = fe.getLong("studentId"),
                                pose = fe.optString("pose", "FRONT"),
                                embedding = floatArray,
                                qualityScore = fe.optDouble("qualityScore", 0.9).toFloat(),
                                imagePath = fe.optString("imagePath", ""),
                                createdAt = fe.optLong("createdAt", System.currentTimeMillis())
                            )
                        )
                    }
                }

                // Parse and insert attendances
                val attendancesArr = rootJson.optJSONArray("attendances")
                if (attendancesArr != null) {
                    for (i in 0 until attendancesArr.length()) {
                        val at = attendancesArr.getJSONObject(i)
                        db.attendanceDao().insert(
                            AttendanceEntity(
                                id = at.getLong("id"),
                                sessionId = at.getLong("sessionId"),
                                studentId = at.getLong("studentId"),
                                timestamp = at.optLong("timestamp", System.currentTimeMillis()),
                                confidence = at.optDouble("confidence", 1.0).toFloat(),
                                method = AttendanceMethod.valueOf(at.optString("method", "MANUAL")),
                                imagePath = at.optString("imagePath", "")
                            )
                        )
                    }
                }

                stats = BackupRestoreStats(
                    usersCount = usersArr.length(),
                    classesCount = classesArr.length(),
                    coursesCount = coursesArr.length(),
                    studentsCount = studentsArr.length(),
                    assignmentsCount = assignArr?.length() ?: 0,
                    sessionsCount = sessionsArr?.length() ?: 0,
                    faceEmbeddingsCount = embeddingsArr?.length() ?: 0,
                    attendancesCount = attendancesArr?.length() ?: 0
                )
            }

            Result.success(stats ?: throw Exception("Không có dữ liệu khôi phục"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
