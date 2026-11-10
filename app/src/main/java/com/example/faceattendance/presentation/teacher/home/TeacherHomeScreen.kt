package com.example.faceattendance.presentation.teacher.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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

data class SessionCardItem(
    val id: Long,
    val courseName: String,
    val className: String,
    val timeSlot: String,
    val room: String,
    val attendedCount: Int,
    val totalStudents: Int,
    val status: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherHomeScreen(
    viewModel: TeacherHomeViewModel,
    onCreateSessionClick: () -> Unit = {},
    onSessionClick: (Long) -> Unit = {},
    onManageStudentsClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Face Attendance", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Xin chào, ${uiState.teacherName}", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                    }
                },
                actions = {
                    IconButton(onClick = onCreateSessionClick) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Tạo buổi học")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Summary Hero Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Lịch giảng dạy hôm nay",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${uiState.todaySessions.size} buổi học được xếp lịch trong ngày",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // Quick Actions Strip
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onCreateSessionClick,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Tạo buổi học", fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = onManageStudentsClick,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sinh viên lớp", fontSize = 13.sp)
                    }
                }
            }

            // Smart Alert Banner
            uiState.missingFaceWarning?.let { alert ->
                item {
                    Surface(
                        color = Color(0xFFFEF3C7),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.WarningAmber, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(alert, fontSize = 12.sp, color = Color(0xFF92400E), lineHeight = 16.sp)
                        }
                    }
                }
            }

            // Session List Header
            item {
                Text(
                    text = "DANH SÁCH BUỔI HỌC",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    letterSpacing = 1.sp
                )
            }

            if (uiState.todaySessions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Hôm nay chưa có buổi học nào. Hãy nhấn 'Tạo buổi học' để bắt đầu.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            } else {
                items(uiState.todaySessions) { sessionWithDetails ->
                    val s = sessionWithDetails.session
                    Card(
                        onClick = { onSessionClick(s.id) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = sessionWithDetails.courseName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (s.status.name == "OPEN") Color(0xFFDCFCE7) else Color(0xFFF1F5F9)
                                ) {
                                    Text(
                                        text = if (s.status.name == "OPEN") "ĐANG MỞ" else s.status.name,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (s.status.name == "OPEN") Color(0xFF15803D) else Color(0xFF475569),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Lớp: ${sessionWithDetails.className} · Phòng: ${s.room}",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = "Thời gian: ${s.startTime} - ${s.endTime}",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Đã điểm danh: ${sessionWithDetails.attendedCount}/${sessionWithDetails.totalStudents}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                TextButton(onClick = { onSessionClick(s.id) }) {
                                    Text("Vào chi tiết →", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
