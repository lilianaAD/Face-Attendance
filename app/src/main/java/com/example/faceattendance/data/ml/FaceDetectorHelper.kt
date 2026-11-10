package com.example.faceattendance.data.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.RectF
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.math.abs

enum class PoseType {
    FRONT,
    LEFT,
    RIGHT,
    UNCERTAIN
}

data class FaceDetectionResult(
    val face: Face?,
    val pose: PoseType,
    val faceCount: Int,
    val isFacingFront: Boolean,
    val isLookingLeft: Boolean,
    val isLookingRight: Boolean,
    val headEulerY: Float,
    val headEulerZ: Float,
    val isInsideOval: Boolean,
    val isValidSize: Boolean,
    val message: String
)

class FaceDetectorHelper(private val context: Context) {

    private val detector: FaceDetector

    init {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .enableTracking()
            .build()

        detector = FaceDetection.getClient(options)
    }

    @OptIn(ExperimentalGetImage::class)
    suspend fun detectFaces(imageProxy: ImageProxy): List<Face> = suspendCancellableCoroutine { cont ->
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            cont.resume(emptyList())
            return@suspendCancellableCoroutine
        }

        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        detector.process(inputImage)
            .addOnSuccessListener { faces ->
                if (cont.isActive) cont.resume(faces)
            }
            .addOnFailureListener {
                if (cont.isActive) cont.resume(emptyList())
            }
    }

    suspend fun detectFacesFromBitmap(bitmap: Bitmap, rotationDegrees: Int = 0): List<Face> = suspendCancellableCoroutine { cont ->
        val inputImage = InputImage.fromBitmap(bitmap, rotationDegrees)
        detector.process(inputImage)
            .addOnSuccessListener { faces ->
                if (cont.isActive) cont.resume(faces)
            }
            .addOnFailureListener {
                if (cont.isActive) cont.resume(emptyList())
            }
    }

    /**
     * Phân tích điều kiện chụp ảnh khuôn mặt:
     * 1. Duy nhất 1 khuôn mặt trong khung hình
     * 2. Khuôn mặt nằm trong khung Oval định vị
     * 3. Kích thước khuôn mặt hợp lệ (không quá xa, không quá gần)
     * 4. Góc quay đầu (Euler Y) phù hợp với tư thế yêu cầu
     */
    fun evaluateFace(
        faces: List<Face>,
        imageWidth: Int,
        imageHeight: Int,
        ovalRect: RectF,
        targetPose: PoseType
    ): FaceDetectionResult {
        if (faces.isEmpty()) {
            return FaceDetectionResult(
                face = null,
                pose = PoseType.UNCERTAIN,
                faceCount = 0,
                isFacingFront = false,
                isLookingLeft = false,
                isLookingRight = false,
                headEulerY = 0f,
                headEulerZ = 0f,
                isInsideOval = false,
                isValidSize = false,
                message = "Không phát hiện khuôn mặt nào trong khung hình"
            )
        }

        if (faces.size > 1) {
            return FaceDetectionResult(
                face = null,
                pose = PoseType.UNCERTAIN,
                faceCount = faces.size,
                isFacingFront = false,
                isLookingLeft = false,
                isLookingRight = false,
                headEulerY = 0f,
                headEulerZ = 0f,
                isInsideOval = false,
                isValidSize = false,
                message = "Phát hiện ${faces.size} khuôn mặt. Chỉ ghi nhận duy nhất 1 sinh viên!"
            )
        }

        val face = faces[0]
        val boundingBox = face.boundingBox

        // 1. Kiểm tra kích thước khuôn mặt relative to frame
        val faceArea = (boundingBox.width() * boundingBox.height()).toFloat()
        val frameArea = (imageWidth * imageHeight).toFloat()
        val areaRatio = if (frameArea > 0) faceArea / frameArea else 0f

        val isValidSize = areaRatio in 0.08f..0.65f
        if (!isValidSize) {
            val sizeMsg = if (areaRatio < 0.08f) "Tiến lại gần camera hơn" else "Lùi ra xa camera một chút"
            return FaceDetectionResult(
                face = face,
                pose = PoseType.UNCERTAIN,
                faceCount = 1,
                isFacingFront = false,
                isLookingLeft = false,
                isLookingRight = false,
                headEulerY = face.headEulerAngleY,
                headEulerZ = face.headEulerAngleZ,
                isInsideOval = false,
                isValidSize = false,
                message = sizeMsg
            )
        }

        // 2. Kiểm tra tâm khuôn mặt có nằm trong Oval
        val faceCenter = RectF(
            boundingBox.centerX().toFloat(),
            boundingBox.centerY().toFloat(),
            boundingBox.centerX().toFloat(),
            boundingBox.centerY().toFloat()
        )
        val isInsideOval = ovalRect.contains(faceCenter.left, faceCenter.top)

        if (!isInsideOval) {
            return FaceDetectionResult(
                face = face,
                pose = PoseType.UNCERTAIN,
                faceCount = 1,
                isFacingFront = false,
                isLookingLeft = false,
                isLookingRight = false,
                headEulerY = face.headEulerAngleY,
                headEulerZ = face.headEulerAngleZ,
                isInsideOval = false,
                isValidSize = true,
                message = "Đưa khuôn mặt vào giữa khung Oval"
            )
        }

        // 3. Phân loại tư thế theo góc quay đầu Euler Y và Z
        val eulerY = face.headEulerAngleY // Quay ngang: dương = sang phải (hoặc trái tùy hướng camera), âm = ngược lại
        val eulerZ = face.headEulerAngleZ // Nghiêng cổ

        val isFacingFront = abs(eulerY) <= 12f && abs(eulerZ) <= 12f
        val isLookingLeft = eulerY in 15f..35f
        val isLookingRight = eulerY in -35f..-15f

        val currentPose = when {
            isFacingFront -> PoseType.FRONT
            isLookingLeft -> PoseType.LEFT
            isLookingRight -> PoseType.RIGHT
            else -> PoseType.UNCERTAIN
        }

        // 4. Kiểm tra sự phù hợp với targetPose
        val isPoseMatched = when (targetPose) {
            PoseType.FRONT -> isFacingFront
            PoseType.LEFT -> isLookingLeft
            PoseType.RIGHT -> isLookingRight
            PoseType.UNCERTAIN -> true
        }

        val message = when {
            isPoseMatched -> "Khuôn mặt đạt chuẩn. Giữ yên..."
            targetPose == PoseType.FRONT -> "Vui lòng nhìn thẳng vào ống kính"
            targetPose == PoseType.LEFT -> "Nghiêng nhẹ khuôn mặt sang trái (khoảng 20°)"
            targetPose == PoseType.RIGHT -> "Nghiêng nhẹ khuôn mặt sang phải (khoảng 20°)"
            else -> "Điều chỉnh tư thế phù hợp"
        }

        return FaceDetectionResult(
            face = face,
            pose = currentPose,
            faceCount = 1,
            isFacingFront = isFacingFront,
            isLookingLeft = isLookingLeft,
            isLookingRight = isLookingRight,
            headEulerY = eulerY,
            headEulerZ = eulerZ,
            isInsideOval = true,
            isValidSize = true,
            message = message
        )
    }

    fun close() {
        detector.close()
    }
}
