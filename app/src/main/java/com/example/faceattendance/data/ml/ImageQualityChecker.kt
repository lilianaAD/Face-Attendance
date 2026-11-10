package com.example.faceattendance.data.ml

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.max
import kotlin.math.min

data class ImageQualityResult(
    val isAcceptable: Boolean,
    val laplacianVariance: Float,
    val brightness: Float,
    val isBlurry: Boolean,
    val isTooDark: Boolean,
    val isTooBright: Boolean,
    val qualityScore: Float, // 0..100
    val failureReason: String?
)

object ImageQualityChecker {

    const val MIN_LAPLACIAN_VARIANCE = 100.0f
    const val MIN_BRIGHTNESS = 40.0f
    const val MAX_BRIGHTNESS = 220.0f

    /**
     * Kiểm tra độ mờ bằng phương sai ma trận Laplacian trên ảnh xám
     */
    fun calculateLaplacianVariance(bitmap: Bitmap): Float {
        val width = bitmap.width
        val height = bitmap.height
        if (width < 3 || height < 3) return 0f

        // Lấy dữ liệu pixel
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        // Chuyển sang ảnh xám 1D array
        val gray = FloatArray(width * height)
        for (i in pixels.indices) {
            val c = pixels[i]
            val r = (c shr 16) and 0xFF
            val g = (c shr 8) and 0xFF
            val b = c and 0xFF
            gray[i] = 0.299f * r + 0.587f * g + 0.114f * b
        }

        // Áp dụng bộ lọc Laplace:
        // [ 0,  1,  0 ]
        // [ 1, -4,  1 ]
        // [ 0,  1,  0 ]
        var sum = 0.0
        var sumSq = 0.0
        var count = 0

        for (y in 1 until height - 1) {
            val rowOffset = y * width
            for (x in 1 until width - 1) {
                val center = gray[rowOffset + x]
                val top = gray[(y - 1) * width + x]
                val bottom = gray[(y + 1) * width + x]
                val left = gray[rowOffset + x - 1]
                val right = gray[rowOffset + x + 1]

                val lap = top + bottom + left + right - 4f * center
                sum += lap
                sumSq += lap * lap
                count++
            }
        }

        if (count == 0) return 0f
        val mean = sum / count
        val variance = (sumSq / count) - (mean * mean)
        return max(0f, variance.toFloat())
    }

    /**
     * Tính toán độ sáng trung bình của ảnh (0..255)
     */
    fun calculateBrightness(bitmap: Bitmap): Float {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        var totalLum = 0.0
        for (c in pixels) {
            val r = (c shr 16) and 0xFF
            val g = (c shr 8) and 0xFF
            val b = c and 0xFF
            totalLum += 0.299 * r + 0.587 * g + 0.114 * b
        }

        return (totalLum / pixels.size).toFloat()
    }

    /**
     * Tổng hợp kiểm tra chất lượng ảnh khuôn mặt
     */
    fun evaluateQuality(faceBitmap: Bitmap): ImageQualityResult {
        val variance = calculateLaplacianVariance(faceBitmap)
        val brightness = calculateBrightness(faceBitmap)

        val isBlurry = variance < MIN_LAPLACIAN_VARIANCE
        val isTooDark = brightness < MIN_BRIGHTNESS
        val isTooBright = brightness > MAX_BRIGHTNESS

        val isAcceptable = !isBlurry && !isTooDark && !isTooBright

        // Tính điểm chất lượng từ 0..100
        val sharpnessScore = min(100f, (variance / 200f) * 100f)
        val brightnessScore = when {
            brightness < MIN_BRIGHTNESS -> (brightness / MIN_BRIGHTNESS) * 50f
            brightness > MAX_BRIGHTNESS -> ((255f - brightness) / (255f - MAX_BRIGHTNESS)) * 50f
            else -> 100f
        }

        val compositeScore = (sharpnessScore * 0.7f + brightnessScore * 0.3f).coerceIn(0f, 100f)

        val reason = when {
            isBlurry -> "Ảnh chụp khuôn mặt bị mờ (Độ nét: ${variance.toInt()}/$MIN_LAPLACIAN_VARIANCE). Vui lòng giữ yên tay."
            isTooDark -> "Ánh sáng quá tối (Độ sáng: ${brightness.toInt()}). Vui lòng di chuyển đến nơi sáng hơn."
            isTooBright -> "Ảnh bị chói sáng hoặc ngược sáng. Vui lòng điều chỉnh hướng đèn."
            else -> null
        }

        return ImageQualityResult(
            isAcceptable = isAcceptable,
            laplacianVariance = variance,
            brightness = brightness,
            isBlurry = isBlurry,
            isTooDark = isTooDark,
            isTooBright = isTooBright,
            qualityScore = compositeScore,
            failureReason = reason
        )
    }
}
