package com.example.faceattendance.presentation.teacher.session

import androidx.compose.foundation.background
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateSessionScreen(
    viewModel: CreateSessionViewModel,
    onNavigateBack: () -> Unit,
    onSessionCreated: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    var courseDropdownExpanded by remember { mutableStateOf(false) }
    var classDropdownExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.createdSessionId) {
        uiState.createdSessionId?.let { id ->
            onSessionCreated(id)
            viewModel.resetNav()
        }
    }

    val selectedCourse = uiState.courses.find { it.id == uiState.selectedCourseId }
    val selectedClass = uiState.assignedClasses.find { it.id == uiState.selectedClassId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tạo Buổi Học Mới", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Quay lại")
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
            // Error Banner
            uiState.errorMessage?.let { error ->
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Warning Banner for missing face embeddings
            uiState.missingFaceWarning?.let { warning ->
                Surface(
                    color = Color(0xFFFEF3C7),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.WarningAmber, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(warning, fontSize = 12.sp, color = Color(0xFF92400E), lineHeight = 16.sp)
                    }
                }
            }

            // 1. Choose Course Dropdown
            Text(
                text = "1. CHỌN MÔN HỌC",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
                letterSpacing = 1.sp
            )

            ExposedDropdownMenuBox(
                expanded = courseDropdownExpanded,
                onExpandedChange = { courseDropdownExpanded = !courseDropdownExpanded }
            ) {
                OutlinedTextField(
                    value = selectedCourse?.let { "${it.courseCode} - ${it.courseName}" } ?: "Chọn môn học...",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = courseDropdownExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                ExposedDropdownMenu(
                    expanded = courseDropdownExpanded,
                    onDismissRequest = { courseDropdownExpanded = false }
                ) {
                    uiState.courses.forEach { course ->
                        DropdownMenuItem(
                            text = { Text("${course.courseCode} - ${course.courseName} (${course.credits} TC)", fontSize = 13.sp) },
                            onClick = {
                                viewModel.onSelectCourse(course.id)
                                courseDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // 2. Choose Assigned Class Dropdown
            Text(
                text = "2. CHỌN LỚP HỌC (LỚP BẠN PHỤ TRÁCH)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
                letterSpacing = 1.sp
            )

            if (uiState.assignedClasses.isEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Bạn chưa được phân công phụ trách lớp học nào. Vui lòng liên hệ Admin.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            } else {
                ExposedDropdownMenuBox(
                    expanded = classDropdownExpanded,
                    onExpandedChange = { classDropdownExpanded = !classDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedClass?.let { "${it.classCode} - ${it.className}" } ?: "Chọn lớp học...",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = classDropdownExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = classDropdownExpanded,
                        onDismissRequest = { classDropdownExpanded = false }
                    ) {
                        uiState.assignedClasses.forEach { cls ->
                            DropdownMenuItem(
                                text = { Text("${cls.classCode} - ${cls.className}", fontSize = 13.sp) },
                                onClick = {
                                    viewModel.onSelectClass(cls.id)
                                    classDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // 3. Date & Time Inputs
            Text(
                text = "3. NGÀY HỌC & KHUNG GIỜ",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
                letterSpacing = 1.sp
            )

            OutlinedTextField(
                value = uiState.sessionDate,
                onValueChange = viewModel::onDateChange,
                label = { Text("Ngày học (YYYY-MM-DD)") },
                leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = uiState.startTime,
                    onValueChange = { viewModel.onTimeChange(it, uiState.endTime) },
                    label = { Text("Bắt đầu (HH:mm)") },
                    leadingIcon = { Icon(Icons.Default.AccessTime, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = uiState.endTime,
                    onValueChange = { viewModel.onTimeChange(uiState.startTime, it) },
                    label = { Text("Kết thúc (HH:mm)") },
                    leadingIcon = { Icon(Icons.Default.AccessTime, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // 4. Room Input
            Text(
                text = "4. PHÒNG HỌC",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
                letterSpacing = 1.sp
            )

            OutlinedTextField(
                value = uiState.room,
                onValueChange = viewModel::onRoomChange,
                label = { Text("Phòng học (VD: A2-301, B1-102)") },
                leadingIcon = { Icon(Icons.Default.MeetingRoom, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Submit Button
            Button(
                onClick = viewModel::submitCreateSession,
                enabled = !uiState.isLoading && uiState.selectedClassId != -1L && uiState.selectedCourseId != -1L,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Tạo buổi học & Mở điểm danh", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
            }
        }
    }
}
