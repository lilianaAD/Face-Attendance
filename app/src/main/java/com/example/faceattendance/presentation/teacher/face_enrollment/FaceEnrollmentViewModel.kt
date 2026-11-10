package com.example.faceattendance.presentation.teacher.face_enrollment

import android.graphics.Bitmap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.faceattendance.data.ml.*
import com.example.faceattendance.domain.usecase.EnrollFaceUseCase
import com.example.faceattendance.domain.usecase.EnrollResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class EnrollmentStep(val title: String, val targetPose: PoseType) {
    STEP_FRONT("Chính diện", PoseType.FRONT),
    STEP_LEFT("Nghiêng trái", PoseType.LEFT),
    STEP_RIGHT("Nghiêng phải", PoseType.RIGHT),
    STEP_REVIEW("Xác nhận & Lưu", PoseType.UNCERTAIN)
}

data class CapturedPoseData(
    val pose: PoseType,
    val bitmap: Bitmap,
    val qualityScore: Float,
    val embedding: FloatArray
)

data class FaceEnrollmentUiState(
    val studentId: Long = 0L,
    val studentName: String = "",
    val studentMssv: String = "",
    val currentStep: EnrollmentStep = EnrollmentStep.STEP_FRONT,
    val instructionMessage: String = "Đưa khuôn mặt vào khung Oval và nhìn thẳng",
    val isFaceValid: Boolean = false,
    val currentQualityScore: Float = 0f,
    val capturedFront: CapturedPoseData? = null,
    val capturedLeft: CapturedPoseData? = null,
    val capturedRight: CapturedPoseData? = null,
    val duplicateWarning: DuplicateMatch? = null,
    val isProcessing: Boolean = false,
    val isSaving: Boolean = false,
    val isComplete: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class FaceEnrollmentViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val enrollFaceUseCase: EnrollFaceUseCase
) : ViewModel() {

    private val studentId: Long = savedStateHandle.get<Long>("studentId") ?: 0L

    private val _uiState = MutableStateFlow(FaceEnrollmentUiState(studentId = studentId))
    val uiState: StateFlow<FaceEnrollmentUiState> = _uiState.asStateFlow()

    fun updateStudentInfo(name: String, mssv: String) {
        _uiState.update { it.copy(studentName = name, studentMssv = mssv) }
    }

    /**
     * Cập nhật phản hồi phân tích realtime từ CameraX + ML Kit
     */
    fun onAnalysisFrame(
        detectionResult: FaceDetectionResult,
        qualityResult: ImageQualityResult?
    ) {
        if (_uiState.value.isSaving || _uiState.value.currentStep == EnrollmentStep.STEP_REVIEW) return

        val targetPose = _uiState.value.currentStep.targetPose
        val isPoseMatched = detectionResult.pose == targetPose
        val isAcceptableQuality = qualityResult?.isAcceptable ?: false

        val isValid = detectionResult.face != null &&
                detectionResult.isInsideOval &&
                detectionResult.isValidSize &&
                isPoseMatched &&
                isAcceptableQuality

        val message = when {
            detectionResult.face == null -> detectionResult.message
            !detectionResult.isInsideOval -> "Đưa khuôn mặt vào giữa khung Oval"
            !detectionResult.isValidSize -> detectionResult.message
            !isPoseMatched -> detectionResult.message
            qualityResult?.isBlurry == true -> qualityResult.failureReason ?: "Khuôn mặt bị mờ, giữ yên"
            qualityResult?.isTooDark == true -> qualityResult.failureReason ?: "Ánh sáng quá tối"
            qualityResult?.isTooBright == true -> qualityResult.failureReason ?: "Quá chói sáng"
            isValid -> "Khuôn mặt đạt chuẩn. Nhấn Chụp hoặc giữ yên..."
            else -> detectionResult.message
        }

        _uiState.update {
            it.copy(
                isFaceValid = isValid,
                instructionMessage = message,
                currentQualityScore = qualityResult?.qualityScore ?: 0f
            )
        }
    }

    /**
     * Thực hiện chụp và trích xuất vector cho tư thế hiện tại
     */
    fun capturePose(
        croppedFaceBitmap: Bitmap,
        embedding: FloatArray,
        qualityScore: Float
    ) {
        val currentStep = _uiState.value.currentStep
        val captured = CapturedPoseData(
            pose = currentStep.targetPose,
            bitmap = croppedFaceBitmap,
            qualityScore = qualityScore,
            embedding = embedding
        )

        when (currentStep) {
            EnrollmentStep.STEP_FRONT -> {
                _uiState.update {
                    it.copy(
                        capturedFront = captured,
                        currentStep = EnrollmentStep.STEP_LEFT,
                        instructionMessage = "Nghiêng nhẹ mặt sang trái khoảng 20°",
                        isFaceValid = false
                    )
                }
            }
            EnrollmentStep.STEP_LEFT -> {
                _uiState.update {
                    it.copy(
                        capturedLeft = captured,
                        currentStep = EnrollmentStep.STEP_RIGHT,
                        instructionMessage = "Nghiêng nhẹ mặt sang phải khoảng 20°",
                        isFaceValid = false
                    )
                }
            }
            EnrollmentStep.STEP_RIGHT -> {
                _uiState.update {
                    it.copy(
                        capturedRight = captured,
                        currentStep = EnrollmentStep.STEP_REVIEW,
                        instructionMessage = "Đã thu thập đủ 3 tư thế. Xem lại và Lưu mẫu.",
                        isFaceValid = false
                    )
                }
            }
            EnrollmentStep.STEP_REVIEW -> {}
        }
    }

    /**
     * Lưu toàn bộ 3 mẫu khuôn mặt vào Database
     */
    fun saveAllPoses(forceOverride: Boolean = false) {
        val front = _uiState.value.capturedFront ?: return
        val left = _uiState.value.capturedLeft ?: return
        val right = _uiState.value.capturedRight ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }

            // Lưu mẫu 1: FRONT
            val resFront = enrollFaceUseCase(
                studentId = studentId,
                bitmap = front.bitmap,
                embedding = front.embedding,
                qualityScore = front.qualityScore,
                pose = "FRONT",
                forceOverrideDuplicate = forceOverride
            )

            if (resFront is EnrollResult.DuplicateWarning) {
                _uiState.update {
                    it.copy(isSaving = false, duplicateWarning = resFront.match)
                }
                return@launch
            }

            if (resFront is EnrollResult.Failure) {
                _uiState.update { it.copy(isSaving = false, errorMessage = resFront.message) }
                return@launch
            }

            // Lưu mẫu 2: LEFT
            enrollFaceUseCase(
                studentId = studentId,
                bitmap = left.bitmap,
                embedding = left.embedding,
                qualityScore = left.qualityScore,
                pose = "LEFT",
                forceOverrideDuplicate = true
            )

            // Lưu mẫu 3: RIGHT
            enrollFaceUseCase(
                studentId = studentId,
                bitmap = right.bitmap,
                embedding = right.embedding,
                qualityScore = right.qualityScore,
                pose = "RIGHT",
                forceOverrideDuplicate = true
            )

            _uiState.update { it.copy(isSaving = false, isComplete = true) }
        }
    }

    fun dismissDuplicateWarning() {
        _uiState.update { it.copy(duplicateWarning = null) }
    }

    fun resetToStep(step: EnrollmentStep) {
        _uiState.update {
            it.copy(
                currentStep = step,
                errorMessage = null,
                duplicateWarning = null,
                isFaceValid = false
            )
        }
    }
}
