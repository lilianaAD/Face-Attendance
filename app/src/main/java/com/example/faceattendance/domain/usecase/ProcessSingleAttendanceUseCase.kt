package com.example.faceattendance.domain.usecase

import android.graphics.Bitmap
import com.example.faceattendance.data.local.db.dao.FaceEmbeddingDao
import com.example.faceattendance.data.ml.FaceMatchResult
import com.example.faceattendance.data.ml.FaceMatcher
import com.example.faceattendance.data.ml.FaceNetModel
import com.example.faceattendance.data.ml.MatchDecision
import com.example.faceattendance.data.repository.AttendanceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

sealed class SingleAttendanceResult {
    data class AutoAccepted(val match: FaceMatchResult, val isNewlyMarked: Boolean) : SingleAttendanceResult()
    data class NeedConfirm(val match: FaceMatchResult) : SingleAttendanceResult()
    object Rejected : SingleAttendanceResult()
    data class Failure(val message: String) : SingleAttendanceResult()
}

class ProcessSingleAttendanceUseCase @Inject constructor(
    private val faceEmbeddingDao: FaceEmbeddingDao,
    private val attendanceRepository: AttendanceRepository
) {
    suspend fun processFrame(
        sessionId: Long,
        classId: Long,
        faceBitmap: Bitmap,
        faceNetModel: FaceNetModel,
        autoSaveIfConfident: Boolean = true
    ): SingleAttendanceResult = withContext(Dispatchers.Default) {
        try {
            val classEmbeddings = faceEmbeddingDao.getEmbeddingsByClass(classId)
            if (classEmbeddings.isEmpty()) {
                return@withContext SingleAttendanceResult.Failure("Lớp học này chưa có sinh viên nào đăng ký mẫu khuôn mặt.")
            }

            val embedding = faceNetModel.extractEmbedding(faceBitmap)
            val match = FaceMatcher.findBestMatch(embedding, classEmbeddings)
                ?: return@withContext SingleAttendanceResult.Rejected

            when (match.decision) {
                MatchDecision.AUTO_ACCEPT -> {
                    var newlyMarked = false
                    if (autoSaveIfConfident) {
                        newlyMarked = attendanceRepository.markAttendance(
                            sessionId = sessionId,
                            studentId = match.studentId,
                            confidence = match.similarity,
                            method = "FACE",
                            note = "Cận mặt cá nhân (Độ khớp: ${(match.similarity * 100).toInt()}%)"
                        )
                    }
                    SingleAttendanceResult.AutoAccepted(match, newlyMarked)
                }
                MatchDecision.NEED_CONFIRM -> {
                    SingleAttendanceResult.NeedConfirm(match)
                }
                MatchDecision.REJECT -> {
                    SingleAttendanceResult.Rejected
                }
            }
        } catch (e: Exception) {
            SingleAttendanceResult.Failure(e.message ?: "Lỗi xử lý điểm danh cận mặt")
        }
    }

    suspend fun confirmAndMark(
        sessionId: Long,
        match: FaceMatchResult
    ): Boolean {
        return attendanceRepository.markAttendance(
            sessionId = sessionId,
            studentId = match.studentId,
            confidence = match.similarity,
            method = "FACE_LOW_CONF",
            note = "Cận mặt (Giáo viên xác nhận thủ công, khớp ${(match.similarity * 100).toInt()}%)"
        )
    }
}
