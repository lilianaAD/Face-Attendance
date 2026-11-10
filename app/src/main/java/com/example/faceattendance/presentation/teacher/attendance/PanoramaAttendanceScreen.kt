package com.example.faceattendance.presentation.teacher.attendance

import android.graphics.Bitmap
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.faceattendance.data.ml.FaceDetectorHelper
import com.example.faceattendance.data.ml.FaceNetModel
import com.example.faceattendance.data.ml.MatchDecision

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PanoramaAttendanceScreen(
    sessionId: Long,
    classId: Long,
    viewModel: PanoramaAttendanceViewModel,
    onNavigateBack: () -> Unit,
    onSuccessFinished: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()

    val faceNetModel = remember { FaceNetModel(context) }
    val faceDetectorHelper = remember { FaceDetectorHelper(context) }

    DisposableEffect(Unit) {
        onDispose {
            faceNetModel.close()
            faceDetectorHelper.close()
        }
    }

    var previewView: PreviewView? by remember { mutableStateOf(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Cửa 1: Điểm Danh Cả Lớp", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Chụp toàn cảnh giảng đường & nhận diện nhiều SV", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFF0F172A))
        ) {
            if (uiState.capturedPhoto == null) {
                // Live CameraX Preview for wide shot
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                            previewView = this

                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                            cameraProviderFuture.addListener({
                                val cameraProvider = cameraProviderFuture.get()
                                val preview = Preview.Builder().build().also {
                                    it.setSurfaceProvider(surfaceProvider)
                                }
                                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                                try {
                                    cameraProvider.unbindAll()
                                    cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
                                } catch (_: Exception) {}
                            }, ContextCompat.getMainExecutor(ctx))
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Wide Framing Guide Overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .border(2.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    )
                }

                // Shutter Button
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Bao quát toàn bộ sinh viên trong phòng rồi bấm chụp",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    IconButton(
                        onClick = {
                            val bitmap = previewView?.bitmap ?: Bitmap.createBitmap(800, 600, Bitmap.Config.ARGB_8888)
                            viewModel.onPhotoCaptured(bitmap, faceDetectorHelper, faceNetModel)
                        },
                        modifier = Modifier
                            .size(76.dp)
                            .background(Color.White, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Chụp toàn cảnh",
                            tint = Color(0xFF0F172A),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            } else if (uiState.isAnalyzing) {
                // Analyzing State
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, strokeWidth = 3.dp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Đang phát hiện & so khớp các khuôn mặt trong lớp...",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text("Áp dụng Qualcomm MobileFaceNet 128-D", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                }
            } else {
                // Results Review Screen
                val summary = uiState.summary
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(16.dp)
                ) {
                    // Summary Metric Header
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Phát hiện", fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary)
                                Text("${summary?.totalFacesDetected ?: 0} mặt", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Tự động (≥0.65)", fontSize = 10.sp, color = Color(0xFF16A34A))
                                Text("${summary?.autoAcceptedList?.size ?: 0} SV", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Cần xác nhận", fontSize = 10.sp, color = Color(0xFFD97706))
                                Text("${summary?.needConfirmList?.size ?: 0} SV", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "DANH SÁCH SINH VIÊN ĐÃ NHẬN DIỆN ĐƯỢC",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    val candidates = (summary?.autoAcceptedList ?: emptyList()) + (summary?.needConfirmList ?: emptyList())

                    if (candidates.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Không tìm thấy khuôn mặt nào trùng khớp với danh sách sinh viên lớp.",
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.secondary,
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(candidates) { item ->
                                val match = item.matchResult
                                val isSelected = uiState.selectedForCommit.contains(item.faceIndex)
                                val isAuto = match?.decision == MatchDecision.AUTO_ACCEPT

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.toggleSelection(item.faceIndex) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) {
                                            if (isAuto) Color(0xFFF0FDF4) else Color(0xFFFFFBEB)
                                        } else MaterialTheme.colorScheme.surface
                                    ),
                                    border = CardDefaults.outlinedCardBorder().copy(
                                        width = if (isSelected) 1.5.dp else 1.dp
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Checkbox(
                                                checked = isSelected,
                                                onCheckedChange = { viewModel.toggleSelection(item.faceIndex) }
                                            )
                                            Column {
                                                Text(
                                                    text = match?.hoTen ?: "Chưa xác định",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp
                                                )
                                                Text(
                                                    text = "MSSV: ${match?.mssv} · Độ khớp: ${((match?.similarity ?: 0f) * 100).toInt()}%",
                                                    fontSize = 11.sp,
                                                    color = if (isAuto) Color(0xFF16A34A) else Color(0xFFD97706)
                                                )
                                            }
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isAuto) Color(0xFFDCFCE7) else Color(0xFFFEF3C7)
                                        ) {
                                            Text(
                                                text = if (isAuto) "Tự động" else "Cần duyệt",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isAuto) Color(0xFF15803D) else Color(0xFFB45309),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Bottom Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = viewModel::resetPhoto,
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Chụp Lại")
                        }

                        Button(
                            onClick = viewModel::commitAttendance,
                            enabled = uiState.selectedForCommit.isNotEmpty() && !uiState.isCommitting,
                            modifier = Modifier.weight(1.5f).height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (uiState.isCommitting) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text("Ghi Nhận (${uiState.selectedForCommit.size} Bạn)")
                        }
                    }
                }
            }

            // Success Dialog
            uiState.commitSuccessCount?.let { count ->
                AlertDialog(
                    onDismissRequest = onSuccessFinished,
                    title = { Text("Điểm Danh Thành Công!", fontWeight = FontWeight.Bold) },
                    text = { Text("Đã lưu thành công $count sinh viên từ ảnh toàn cảnh vào cơ sở dữ liệu.") },
                    confirmButton = {
                        Button(onClick = onSuccessFinished) {
                            Text("Quay Về Buổi Học")
                        }
                    }
                )
            }
        }
    }
}
