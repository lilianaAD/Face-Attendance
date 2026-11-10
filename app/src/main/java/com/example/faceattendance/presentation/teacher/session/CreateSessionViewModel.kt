package com.example.faceattendance.presentation.teacher.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.faceattendance.data.local.db.dao.ClassDao
import com.example.faceattendance.data.local.db.dao.CourseDao
import com.example.faceattendance.data.local.db.entity.ClassEntity
import com.example.faceattendance.data.local.db.entity.CourseEntity
import com.example.faceattendance.data.local.db.entity.UserRole
import com.example.faceattendance.data.local.preferences.AuthDataStore
import com.example.faceattendance.domain.usecase.CreateSessionUseCase
import com.example.faceattendance.domain.usecase.GetTeacherStudentsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class CreateSessionUiState(
    val teacherId: Long = -1L,
    val teacherRole: UserRole = UserRole.TEACHER,
    val assignedClasses: List<ClassEntity> = emptyList(),
    val courses: List<CourseEntity> = emptyList(),
    val selectedClassId: Long = -1L,
    val selectedCourseId: Long = -1L,
    val sessionDate: String = "",
    val startTime: String = "07:30",
    val endTime: String = "10:00",
    val room: String = "A2-301",
    val isLoading: Boolean = false,
    val missingFaceWarning: String? = null,
    val createdSessionId: Long? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class CreateSessionViewModel @Inject constructor(
    private val authDataStore: AuthDataStore,
    private val classDao: ClassDao,
    private val courseDao: CourseDao,
    private val createSessionUseCase: CreateSessionUseCase,
    private val getTeacherStudentsUseCase: GetTeacherStudentsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateSessionUiState())
    val uiState: StateFlow<CreateSessionUiState> = _uiState.asStateFlow()

    init {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        _uiState.update { it.copy(sessionDate = todayStr) }

        viewModelScope.launch {
            authDataStore.authSessionFlow.collect { session ->
                if (session.isLoggedIn && session.role != null) {
                    _uiState.update { it.copy(teacherId = session.userId, teacherRole = session.role) }
                    loadClassesAndCourses(session.userId)
                }
            }
        }
    }

    private fun loadClassesAndCourses(teacherId: Long) {
        viewModelScope.launch {
            combine(
                classDao.getClassesForTeacher(teacherId),
                courseDao.getAllCourses()
            ) { classes, courses ->
                val defaultClassId = _uiState.value.selectedClassId.takeIf { it != -1L } ?: classes.firstOrNull()?.id ?: -1L
                val defaultCourseId = _uiState.value.selectedCourseId.takeIf { it != -1L } ?: courses.firstOrNull()?.id ?: -1L
                _uiState.update {
                    it.copy(
                        assignedClasses = classes,
                        courses = courses,
                        selectedClassId = defaultClassId,
                        selectedCourseId = defaultCourseId
                    )
                }
                if (defaultClassId != -1L) {
                    checkFaceWarning(defaultClassId)
                }
            }.collect()
        }
    }

    fun onSelectClass(classId: Long) {
        _uiState.update { it.copy(selectedClassId = classId, errorMessage = null) }
        checkFaceWarning(classId)
    }

    fun onSelectCourse(courseId: Long) {
        _uiState.update { it.copy(selectedCourseId = courseId, errorMessage = null) }
    }

    fun onDateChange(date: String) {
        _uiState.update { it.copy(sessionDate = date, errorMessage = null) }
    }

    fun onTimeChange(start: String, end: String) {
        _uiState.update { it.copy(startTime = start, endTime = end, errorMessage = null) }
    }

    fun onRoomChange(room: String) {
        _uiState.update { it.copy(room = room, errorMessage = null) }
    }

    private fun checkFaceWarning(classId: Long) {
        viewModelScope.launch {
            val missing = getTeacherStudentsUseCase.countMissingFace(classId)
            _uiState.update {
                it.copy(
                    missingFaceWarning = if (missing > 0) {
                        "Cảnh báo: Lớp này hiện có $missing sinh viên chưa đăng ký mẫu khuôn mặt."
                    } else null
                )
            }
        }
    }

    fun submitCreateSession() {
        val s = _uiState.value
        if (s.selectedClassId == -1L || s.selectedCourseId == -1L) {
            _uiState.update { it.copy(errorMessage = "Vui lòng chọn môn học và lớp học") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            val result = createSessionUseCase(
                userId = s.teacherId,
                role = s.teacherRole,
                courseId = s.selectedCourseId,
                classId = s.selectedClassId,
                sessionDate = s.sessionDate,
                startTime = s.startTime,
                endTime = s.endTime,
                room = s.room
            )

            result.onSuccess { session ->
                _uiState.update { it.copy(isLoading = false, createdSessionId = session.id) }
            }.onFailure { ex ->
                _uiState.update { it.copy(isLoading = false, errorMessage = ex.message) }
            }
        }
    }

    fun resetNav() {
        _uiState.update { it.copy(createdSessionId = null) }
    }
}
