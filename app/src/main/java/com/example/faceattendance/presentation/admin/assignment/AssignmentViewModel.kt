package com.example.faceattendance.presentation.admin.assignment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.faceattendance.data.local.db.dao.ClassDao
import com.example.faceattendance.data.local.db.dao.TeacherClassDao
import com.example.faceattendance.data.local.db.dao.UserDao
import com.example.faceattendance.data.local.db.entity.ClassEntity
import com.example.faceattendance.data.local.db.entity.UserEntity
import com.example.faceattendance.data.local.db.entity.UserRole
import com.example.faceattendance.domain.usecase.ManageAssignmentsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AssignmentItem(
    val classEntity: ClassEntity,
    val teachers: List<UserEntity>
)

data class AssignmentUiState(
    val classes: List<ClassEntity> = emptyList(),
    val teachers: List<UserEntity> = emptyList(),
    val selectedClass: ClassEntity? = null,
    val assignedTeachers: List<UserEntity> = emptyList(),
    val showAssignDialog: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class AssignmentViewModel @Inject constructor(
    private val classDao: ClassDao,
    private val userDao: UserDao,
    private val manageAssignmentsUseCase: ManageAssignmentsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AssignmentUiState())
    val uiState: StateFlow<AssignmentUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        combine(
            classDao.getAllClasses(),
            userDao.getUsersByRole(UserRole.TEACHER)
        ) { classes, teachers ->
            val firstClass = _uiState.value.selectedClass ?: classes.firstOrNull()
            _uiState.update { it.copy(classes = classes, teachers = teachers, selectedClass = firstClass) }
            firstClass?.let { loadTeachersForClass(it.id, teachers) }
        }.launchIn(viewModelScope)
    }

    fun onSelectClass(cls: ClassEntity) {
        _uiState.update { it.copy(selectedClass = cls) }
        loadTeachersForClass(cls.id, _uiState.value.teachers)
    }

    private fun loadTeachersForClass(classId: Long, allTeachers: List<UserEntity>) {
        viewModelScope.launch {
            manageAssignmentsUseCase.getAssignedTeacherIdsForClass(classId).collect { ids ->
                val assigned = allTeachers.filter { ids.contains(it.id) }
                _uiState.update { it.copy(assignedTeachers = assigned) }
            }
        }
    }

    fun toggleAssignDialog(show: Boolean) {
        _uiState.update { it.copy(showAssignDialog = show, errorMessage = null) }
    }

    fun assignTeacher(teacherId: Long) {
        val cls = _uiState.value.selectedClass ?: return
        viewModelScope.launch {
            val result = manageAssignmentsUseCase.assignTeacherToClass(UserRole.ADMIN, teacherId, cls.id)
            result.onSuccess {
                _uiState.update { it.copy(showAssignDialog = false, successMessage = "Phân công thành công!") }
                loadTeachersForClass(cls.id, _uiState.value.teachers)
            }.onFailure { ex ->
                _uiState.update { it.copy(errorMessage = ex.message) }
            }
        }
    }

    fun removeAssignment(teacherId: Long) {
        val cls = _uiState.value.selectedClass ?: return
        viewModelScope.launch {
            val result = manageAssignmentsUseCase.removeAssignment(UserRole.ADMIN, teacherId, cls.id)
            result.onSuccess {
                _uiState.update { it.copy(successMessage = "Đã gỡ phân công giáo viên") }
                loadTeachersForClass(cls.id, _uiState.value.teachers)
            }
        }
    }

    fun clearFeedback() {
        _uiState.update { it.copy(successMessage = null, errorMessage = null) }
    }
}
