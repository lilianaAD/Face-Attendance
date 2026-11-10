package com.example.faceattendance.presentation.teacher.attendance

import android.graphics.Bitmap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.faceattendance.data.ml.FaceMatchResult
import com.example.faceattendance.data.ml.FaceNetModel
import com.example.faceattendance.domain.usecase.ProcessSingleAttendanceUseCase
import com.example.faceattendance.domain.usecase.SingleAttendanceResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SingleAttendanceUiState(
    val sessionId: Long = 0L,
    val classId: Long = 0L,
    val markedCount: Int = 0,
    val lastRecognized: FaceMatchResult? = null,
    val isAutoSuccessShowing: Boolean = false,
    val needConfirmMatch: FaceMatchResult? = null,
    val isPaused: Boolean = false,
    val statusMessage: String = "Đưa từng sinh viên vào khung Oval để điểm danh",
    val errorMessage: String? = null
)

@HiltViewModel
class SingleAttendanceViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val processSingleAttendanceUseCase: ProcessSingleAttendanceUseCase
) : ViewModel() {

    val sessionId: Long = savedStateHandle.get<Long>("sessionId") ?: 0L
    val classId: Long = savedStateHandle.get<Long>("classId") ?: 0L

    private val _uiState = MutableStateFlow(SingleAttendanceUiState(sessionId = sessionId, classId = classId))
    val uiState: StateFlow<SingleAttendanceUiState> = _uiState.asStateFlow()

    private var isBusyProcessing = false

    fun onFrameCaptured(
        croppedFaceBitmap: Bitmap,
        faceNetModel: FaceNetModel
    ) {
        if (isBusyProcessing || _uiState.value.isPaused || _uiState.value.isAutoSuccessShowing) return

        isBusyProcessing = true

        viewModelScope.launch {
            try {
                val result = processSingleAttendanceUseCase.processFrame(
                    sessionId = sessionId,
                    classId = classId,
                    faceBitmap = croppedFaceBitmap,
                    faceNetModel = faceNetModel,
                    autoSaveIfConfident = true
                )

                when (result) {
                    is SingleAttendanceResult.AutoAccepted -> {
                        _uiState.update {
                            it.copy(
                                lastRecognized = result.match,
                                isAutoSuccessShowing = true,
                                markedCount = if (result.isNewlyMarked) it.markedCount + 1 else it.markedCount,
                                statusMessage = "Đã điểm danh: ${result.match.hoTen}"
                            )
                        }
                        // Chờ 1.5 giây để giáo viên & sinh viên nhìn thấy rồi tự động đón người kế tiếp
                        delay(1500)
                        _uiState.update {
                            it.copy(
                                isAutoSuccessShowing = false,
                                statusMessage = "Đưa sinh viên tiếp theo vào khung Oval"
                            )
                        }
                    }
                    is SingleAttendanceResult.NeedConfirm -> {
                        _uiState.update {
                            it.copy(
                                needConfirmMatch = result.match,
                                isPaused = true,
                                statusMessage = "Cần xác nhận độ khớp ${(result.match.similarity * 100).toInt()}%"
                            )
                        }
                    }
                    is SingleAttendanceResult.Rejected -> {
                        _uiState.update {
                            it.copy(statusMessage = "Không khớp với sinh viên nào trong lớp (<0.55)")
                        }
                    }
                    is SingleAttendanceResult.Failure -> {
                        _uiState.update {
                            it.copy(errorMessage = result.message)
                        }
                    }
                }
            } finally {
                isBusyProcessing = false
            }
        }
    }

    fun confirmPendingMatch() {
        val match = _uiState.value.needConfirmMatch ?: return
        viewModelScope.launch {
            processSingleAttendanceUseCase.confirmAndMark(sessionId, match)
            _uiState.update {
                it.copy(
                    needConfirmMatch = null,
                    isPaused = false,
                    markedCount = it.markedCount + 1,
                    statusMessage = "Đã xác nhận: ${match.hoTen}"
                )
            }
        }
    }

    fun dismissPendingMatch() {
        _uiState.update {
            it.copy(
                needConfirmMatch = null,
                isPaused = false,
                statusMessage = "Đưa sinh viên tiếp theo vào khung Oval"
            )
        }
    }
}
