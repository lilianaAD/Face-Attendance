package com.example.faceattendance.presentation.teacher.students

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.faceattendance.data.local.db.dao.StudentWithFaceInfo
import com.example.faceattendance.data.local.db.entity.ClassEntity
import com.example.faceattendance.data.local.db.entity.StudentEntity
import com.example.faceattendance.data.local.preferences.AuthDataStore
import com.example.faceattendance.data.local.db.entity.FaceEmbeddingEntity
import com.example.faceattendance.data.repository.FaceRepository
import com.example.faceattendance.domain.usecase.GetTeacherStudentsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class FaceFilter {
    ALL,
    HAS_FACE,
    MISSING_FACE
}

data class StudentManagementUiState(
    val teacherId: Long = 2L,
    val classes: List<ClassEntity> = emptyList(),
    val selectedClass: ClassEntity? = null,
    val students: List<StudentWithFaceInfo> = emptyList(),
    val searchQuery: String = "",
    val faceFilter: FaceFilter = FaceFilter.ALL,
    val selectedStudent: StudentEntity? = null,
    val faceEmbeddings: List<FaceEmbeddingEntity> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class StudentViewModel @Inject constructor(
    private val authDataStore: AuthDataStore,
    private val getTeacherStudentsUseCase: GetTeacherStudentsUseCase,
    private val faceRepository: FaceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudentManagementUiState())
    val uiState: StateFlow<StudentManagementUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authDataStore.authSessionFlow.collect { session ->
                if (session.isLoggedIn && session.userId != -1L) {
                    _uiState.update { it.copy(teacherId = session.userId) }
                    loadTeacherClasses(session.userId)
                }
            }
        }
    }

    private fun loadTeacherClasses(teacherId: Long) {
        getTeacherStudentsUseCase.getClassesForTeacher(teacherId)
            .onEach { classList ->
                val first = _uiState.value.selectedClass ?: classList.firstOrNull()
                _uiState.update { it.copy(classes = classList, selectedClass = first) }
                first?.let { loadStudentsForClass(it.id) }
            }
            .launchIn(viewModelScope)
    }

    fun onSelectClass(cls: ClassEntity) {
        _uiState.update { it.copy(selectedClass = cls) }
        loadStudentsForClass(cls.id)
    }

    private fun loadStudentsForClass(classId: Long) {
        getTeacherStudentsUseCase.getStudentsWithFaceInfo(classId)
            .onEach { studentList ->
                _uiState.update { it.copy(students = studentList) }
            }
            .launchIn(viewModelScope)
    }

    fun onSearchChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onFilterChange(filter: FaceFilter) {
        _uiState.update { it.copy(faceFilter = filter) }
    }

    fun selectStudentForDetail(studentId: Long) {
        viewModelScope.launch {
            val student = getTeacherStudentsUseCase.getStudentById(studentId)
            _uiState.update { it.copy(selectedStudent = student) }
            
            faceRepository.getEmbeddingsForStudent(studentId).collect { embeddings ->
                _uiState.update { it.copy(faceEmbeddings = embeddings) }
            }
        }
    }

    fun deleteFaceEmbedding(embedding: FaceEmbeddingEntity) {
        viewModelScope.launch {
            faceRepository.deleteEmbedding(embedding)
            _uiState.value.selectedClass?.let { loadStudentsForClass(it.id) }
        }
    }

    fun deleteAllFaceEmbeddingsForStudent(studentId: Long) {
        viewModelScope.launch {
            faceRepository.deleteAllEmbeddingsForStudent(studentId)
            _uiState.value.selectedClass?.let { loadStudentsForClass(it.id) }
        }
    }
}
