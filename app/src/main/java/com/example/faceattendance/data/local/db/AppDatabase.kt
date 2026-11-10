package com.example.faceattendance.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.faceattendance.data.local.db.dao.*
import com.example.faceattendance.data.local.db.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.faceattendance.data.security.PasswordHasher

@Database(
    entities = [
        UserEntity::class,
        ClassEntity::class,
        CourseEntity::class,
        StudentEntity::class,
        TeacherClassEntity::class,
        SessionEntity::class,
        FaceEmbeddingEntity::class,
        AttendanceEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun classDao(): ClassDao
    abstract fun courseDao(): CourseDao
    abstract fun studentDao(): StudentDao
    abstract fun teacherClassDao(): TeacherClassDao
    abstract fun sessionDao(): SessionDao
    abstract fun faceEmbeddingDao(): FaceEmbeddingDao
    abstract fun attendanceDao(): AttendanceDao

    companion object {
        const val DATABASE_NAME = "face_attendance.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .addCallback(DatabaseCallback(scope))
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    seedDatabase(database)
                }
            }
        }

        private suspend fun seedDatabase(database: AppDatabase) {
            val userDao = database.userDao()
            val hasher = PasswordHasher()

            if (userDao.getByUsername("admin") == null) {
                userDao.insert(
                    UserEntity(
                        username = "admin",
                        passwordHash = hasher.hashPassword("admin123"),
                        role = UserRole.ADMIN,
                        displayName = "Quản trị viên Hệ thống",
                        email = "admin@university.edu.vn",
                        isActive = true
                    )
                )
            }
            if (userDao.getByUsername("teacher1") == null) {
                userDao.insert(
                    UserEntity(
                        username = "teacher1",
                        passwordHash = hasher.hashPassword("teacher123"),
                        role = UserRole.TEACHER,
                        displayName = "Giáo viên 1",
                        email = "teacher1@university.edu.vn",
                        isActive = true
                    )
                )
            }
        }
    }
}
