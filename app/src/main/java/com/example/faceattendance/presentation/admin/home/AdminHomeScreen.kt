package com.example.faceattendance.presentation.admin.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class AdminMenuItem(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val action: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminHomeScreen(
    adminName: String = "Quản trị viên",
    classCount: Int = 0,
    teacherCount: Int = 0,
    studentCount: Int = 0,
    courseCount: Int = 0,
    onNavigateToImport: () -> Unit = {},
    onNavigateToUsers: () -> Unit = {},
    onNavigateToClasses: () -> Unit = {},
    onNavigateToCourses: () -> Unit = {},
    onNavigateToAssignments: () -> Unit = {},
    onNavigateToReports: () -> Unit = {},
    onNavigateToBackup: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    val menuItems = listOf(
        AdminMenuItem("Import Dữ Liệu", "Excel/CSV lớp, môn, SV", Icons.Default.FileUpload, onNavigateToImport),
        AdminMenuItem("Quản Lý Giáo Viên", "Tạo, sửa, khóa tài khoản", Icons.Default.People, onNavigateToUsers),
        AdminMenuItem("Quản Lý Lớp Học", "Danh sách & chỉnh sửa lớp", Icons.Default.MeetingRoom, onNavigateToClasses),
        AdminMenuItem("Quản Lý Môn Học", "Mã môn, số tín chỉ", Icons.Default.MenuBook, onNavigateToCourses),
        AdminMenuItem("Phân Công Giảng Dạy", "Gán giáo viên vào lớp", Icons.Default.AssignmentInd, onNavigateToAssignments),
        AdminMenuItem("Báo Cáo Toàn Trường", "Thống kê chuyên cần", Icons.Default.Assessment, onNavigateToReports),
        AdminMenuItem("Sao Lưu & Phục Hồi", "Backup DB và ảnh", Icons.Default.Backup, onNavigateToBackup),
        AdminMenuItem("Cài Đặt Hệ Thống", "Ngưỡng AI, tham số", Icons.Default.Settings, onNavigateToSettings)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Quản Trị Hệ Thống", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(adminName, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
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
        ) {
            // Stats summary card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "TỔNG QUAN TOÀN TRƯỜNG",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatItem(label = "Lớp học", value = classCount.toString())
                        StatItem(label = "Giáo viên", value = teacherCount.toString())
                        StatItem(label = "Sinh viên", value = studentCount.toString())
                        StatItem(label = "Môn học", value = courseCount.toString())
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "DANH MỤC QUẢN TRỊ",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Grid menu
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(menuItems) { item ->
                    Card(
                        onClick = item.action,
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = item.title,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = item.description,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.secondary,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}
