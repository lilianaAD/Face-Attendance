package com.example.faceattendance.domain.usecase

import android.graphics.Bitmap
import android.graphics.Rect
import com.example.faceattendance.data.local.db.dao.FaceEmbeddingDao
import com.example.faceattendance.data.ml.*
import com.example.faceattendance.data.repository.AttendanceRecord
import com.example.faceattendance.data.repository.AttendanceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min

data class PanoramaMatchItem(
    val faceIndex: Int,
    val faceBounds: Rect,
    val matchResult: FaceMatchResult?,
    val croppedBitmap: Bitmap,
    val isConfirmed: Boolean = false
)

data class PanoramaProcessSummary(
    val totalFacesDetected: Int,
    val autoAcceptedList: List<PanoramaMatchItem>,
    val needConfirmList: List<PanoramaMatchItem>,
    val unrecognizedList: List<PanoramaMatchItem>
)

class ProcessPanoramaAttendanceUseCase @Inject constructor(
    private val faceEmbeddingDao: FaceEmbeddingDao,
    private val attendanceRepository: AttendanceRepository
) {
    suspend fun analyzePanorama(
        classId: Long,
        panoramaBitmap: Bitmap,
        faceDetectorHelper: FaceDetectorHelper,
        faceNetModel: FaceNetModel
    ): PanoramaProcessSummary = withContext(Dispatchers.Default) {
        // 1. Phát hiện tất cả khuôn mặt trong ảnh toàn cảnh
        val faces = faceDetectorHelper.detectFacesFromBitmap(panoramaBitmap)
        if (faces.isEmpty()) {
            return@withContext PanoramaProcessSummary(
                totalFacesDetected = 0,
                autoAcceptedList = emptyList(),
                needConfirmList = emptyList(),
                unrecognizedList = emptyList()
            )
        }

        // 2. Lấy toàn bộ vector mẫu mặt của sinh viên trong lớp học này
        val classEmbeddings = faceEmbeddingDao.getEmbeddingsByClass(classId)

        // 3. Xử lý song song crop & trích xuất vector cho từng khuôn mặt
        val matchJobs = faces.mapIndexed { index, face ->
            async {
                val box = face.boundingBox
                // Mở rộng viền 10% nhưng không vượt quá kích thước ảnh
                val left = max(0, box.left - (box.width() * 0.1f).toInt())
                val top = max(0, box.top - (box.height() * 0.1f).toInt())
                val width = min(panoramaBitmap.width - left, box.width() + (box.width() * 0.2f).toInt())
                val height = min(panoramaBitmap.height - top, box.height() + (box.height() * 0.2f).toInt())

                if (width <= 0 || height <= 0) return@async null

                val cropped = Bitmap.createBitmap(panoramaBitmap, left, top, width, height)
                val embedding = faceNetModel.extractEmbedding(cropped)
                val bestMatch = FaceMatcher.findBestMatch(embedding, classEmbeddings)

                PanoramaMatchItem(
                    faceIndex = index,
                    faceBounds = box,
                    matchResult = bestMatch,
                    croppedBitmap = cropped
                )
            }
        }

        val matchItems = matchJobs.awaitAll().filterNotNull()

        // 4. Phân loại theo 3 nhóm dựa trên ngưỡng AI
        val autoAccepted = mutableListOf<PanoramaMatchItem>()
        val needConfirm = mutableListOf<PanoramaMatchItem>()
        val unrecognized = mutableListOf<PanoramaMatchItem>()

        val seenStudentIds = mutableSetOf<Long>()

        for (item in matchItems) {
            val match = item.matchResult
            if (match == null || match.decision == MatchDecision.REJECT) {
                unrecognized.add(item)
            } else if (seenStudentIds.contains(match.studentId)) {
                // Đã nhận diện trước đó trong cùng ảnh (tránh trùng)
                continue
            } else {
                seenStudentIds.add(match.studentId)
                when (match.decision) {
                    MatchDecision.AUTO_ACCEPT -> autoAccepted.add(item)
                    MatchDecision.NEED_CONFIRM -> needConfirm.add(item)
                    MatchDecision.REJECT -> unrecognized.add(item)
                }
            }
        }

        PanoramaProcessSummary(
            totalFacesDetected = faces.size,
            autoAcceptedList = autoAccepted,
            needConfirmList = needConfirm,
            unrecognizedList = unrecognized
        )
    }

    suspend fun commitBatch(
        sessionId: Long,
        itemsToCommit: List<PanoramaMatchItem>
    ): Int {
        val records = itemsToCommit.mapNotNull { item ->
            val match = item.matchResult ?: return@mapNotNull null
            val method = if (match.decision == MatchDecision.AUTO_ACCEPT) "FACE" else "FACE_LOW_CONF"
            AttendanceRecord(
                studentId = match.studentId,
                confidence = match.similarity,
                method = method,
                note = "Toàn cảnh lớp (Độ khớp: ${(match.similarity * 100).toInt()}%)"
            )
        }

        return attendanceRepository.markBatchAttendance(sessionId, records)
    }
}
