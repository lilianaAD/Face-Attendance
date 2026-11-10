package com.example.faceattendance.presentation.teacher.students

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.faceattendance.data.local.db.entity.FaceEmbeddingEntity
import com.example.faceattendance.data.local.db.entity.StudentEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDetailScreen(
    studentId: Long,
    viewModel: StudentViewModel,
    onNavigateBack: () -> Unit,
    onEnrollFaceClick: (studentId: Long, name: String, mssv: String) -> Unit = { _, _, _ -> }
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(studentId) {
        viewModel.selectStudentForDetail(studentId)
    }

    val student = uiState.selectedStudent
    val embeddings = uiState.faceEmbeddings

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hồ Sơ Sinh Viên", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                actions = {
                    if (embeddings.isNotEmpty()) {
                        IconButton(onClick = { showDeleteConfirmDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Xóa toàn bộ mẫu mặt",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = student?.hoTen ?: "Đang tải...",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "MSSV: ${student?.mssv ?: ""}", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    Text(text = "Email: ${student?.email ?: "Chưa có email"}", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                }
            }

            // 3-Pose Face Enrollment Grid Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MẪU KHUÔN MẶT ĐÃ GHI (3 GÓC CHỤP)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "${embeddings.size}/3 mẫu",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (embeddings.size == 3) Color(0xFF16A34A) else Color(0xFFD97706)
                )
            }

            // 3 Slots Grid
            val frontPose = embeddings.find { it.pose.equals("FRONT", ignoreCase = true) }
            val leftPose = embeddings.find { it.pose.equals("LEFT", ignoreCase = true) }
            val rightPose = embeddings.find { it.pose.equals("RIGHT", ignoreCase = true) }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf(
                    Triple("Chính diện", frontPose, "FRONT"),
                    Triple("Nghiêng trái", leftPose, "LEFT"),
                    Triple("Nghiêng phải", rightPose, "RIGHT")
                ).forEach { (label, emb, _) ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(125.dp)
                            .border(
                                width = 1.dp,
                                color = if (emb != null) Color(0xFF22C55E).copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .background(
                                color = if (emb != null) Color(0xFF22C55E).copy(alpha = 0.05f) else MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            if (emb != null) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(Color(0xFF22C55E).copy(alpha = 0.15f), RoundedCornerShape(20.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF16A34A),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = "${emb.qualityScore.toInt()}% đạt",
                                    fontSize = 10.sp,
                                    color = Color(0xFF16A34A),
                                    fontWeight = FontWeight.Medium
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Face,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                                Text("Chưa có mẫu", fontSize = 9.sp, color = Color(0xFFD97706))
                            }
                        }
                    }
                }
            }

            // Enroll / Re-enroll Action Button
            Button(
                onClick = {
                    student?.let {
                        onEnrollFaceClick(it.id, it.hoTen, it.mssv)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (embeddings.isEmpty()) "Ghi Mẫu Khuôn Mặt Mới (CameraX)" else "Chụp Lại Mẫu Khuôn Mặt",
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Attendance History Summary
            Text(
                text = "LỊCH SỬ CHUYÊN CẦN",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
                letterSpacing = 1.sp
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Tổng buổi đã học", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                            Text("12 buổi", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("Có mặt", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                            Text("11 buổi", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                        }
                        Column {
                            Text("Vắng mặt", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                            Text("1 buổi", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                        }
                    }
                }
            }
        }

        // Delete Confirmation Dialog
        if (showDeleteConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = false },
                title = { Text("Xác Nhận Xóa Mẫu") },
                text = { Text("Bạn có chắc chắn muốn xóa toàn bộ mẫu khuôn mặt của sinh viên này không? Sau khi xóa, sinh viên sẽ không thể điểm danh tự động.") },
                confirmButton = {
                    Button(
                        onClick = {
                            student?.let { viewModel.deleteAllFaceEmbeddingsForStudent(it.id) }
                            showDeleteConfirmDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Xóa Mẫu")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmDialog = false }) {
                        Text("Hủy")
                    }
                }
            )
        }
    }
}
