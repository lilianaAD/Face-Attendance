package com.example.faceattendance.presentation.teacher.attendance

import android.graphics.Bitmap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.faceattendance.data.ml.FaceDetectorHelper
import com.example.faceattendance.data.ml.FaceNetModel
import com.example.faceattendance.domain.usecase.PanoramaMatchItem
import com.example.faceattendance.domain.usecase.PanoramaProcessSummary
import com.example.faceattendance.domain.usecase.ProcessPanoramaAttendanceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PanoramaUiState(
    val sessionId: Long = 0L,
    val classId: Long = 0L,
    val capturedPhoto: Bitmap? = null,
    val isAnalyzing: Boolean = false,
    val summary: PanoramaProcessSummary? = null,
    val selectedForCommit: Set<Int> = emptySet(),
    val isCommitting: Boolean = false,
    val commitSuccessCount: Int? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class PanoramaAttendanceViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val processPanoramaUseCase: ProcessPanoramaAttendanceUseCase
) : ViewModel() {

    val sessionId: Long = savedStateHandle.get<Long>("sessionId") ?: 0L
    val classId: Long = savedStateHandle.get<Long>("classId") ?: 0L

    private val _uiState = MutableStateFlow(PanoramaUiState(sessionId = sessionId, classId = classId))
    val uiState: StateFlow<PanoramaUiState> = _uiState.asStateFlow()

    fun onPhotoCaptured(
        bitmap: Bitmap,
        faceDetectorHelper: FaceDetectorHelper,
        faceNetModel: FaceNetModel
    ) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    capturedPhoto = bitmap,
                    isAnalyzing = true,
                    summary = null,
                    commitSuccessCount = null,
                    errorMessage = null
                )
            }

            try {
                val result = processPanoramaUseCase.analyzePanorama(
                    classId = classId,
                    panoramaBitmap = bitmap,
                    faceDetectorHelper = faceDetectorHelper,
                    faceNetModel = faceNetModel
                )

                // Mặc định chọn tất cả các khuôn mặt auto-accepted
                val initialSelected = result.autoAcceptedList.map { it.faceIndex }.toSet()

                _uiState.update {
                    it.copy(
                        isAnalyzing = false,
                        summary = result,
                        selectedForCommit = initialSelected
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isAnalyzing = false,
                        errorMessage = e.message ?: "Lỗi phân tích ảnh toàn cảnh"
                    )
                }
            }
        }
    }

    fun toggleSelection(faceIndex: Int) {
        _uiState.update { state ->
            val current = state.selectedForCommit
            val next = if (current.contains(faceIndex)) current - faceIndex else current + faceIndex
            state.copy(selectedForCommit = next)
        }
    }

    fun commitAttendance() {
        val summary = _uiState.value.summary ?: return
        val selectedIndices = _uiState.value.selectedForCommit
        val allCandidates = summary.autoAcceptedList + summary.needConfirmList
        val toCommit = allCandidates.filter { selectedIndices.contains(it.faceIndex) }

        if (toCommit.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isCommitting = true) }
            val count = processPanoramaUseCase.commitBatch(sessionId, toCommit)
            _uiState.update {
                it.copy(
                    isCommitting = false,
                    commitSuccessCount = count
                )
            }
        }
    }

    fun resetPhoto() {
        _uiState.update {
            it.copy(
                capturedPhoto = null,
                isAnalyzing = false,
                summary = null,
                selectedForCommit = emptySet(),
                commitSuccessCount = null,
                errorMessage = null
            )
        }
    }
}
