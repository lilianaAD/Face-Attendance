package com.example.faceattendance.presentation.teacher.attendance

import android.graphics.Bitmap
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.faceattendance.data.ml.FaceNetModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SingleAttendanceScreen(
    sessionId: Long,
    classId: Long,
    viewModel: SingleAttendanceViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()

    val faceNetModel = remember { FaceNetModel(context) }

    DisposableEffect(Unit) {
        onDispose {
            faceNetModel.close()
        }
    }

    var previewView: PreviewView? by remember { mutableStateOf(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Cửa 2: Điểm Danh Cận Mặt", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Nhận diện tuần tự từng sinh viên qua khung Oval", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                actions = {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFDCFCE7),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Text(
                            text = "${uiState.markedCount} Đã có mặt",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF15803D),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.Black)
        ) {
            // CameraX Preview
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
                            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
                            } catch (_: Exception) {
                                try {
                                    cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview)
                                } catch (_: Exception) {}
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Canvas Oval Overlay
            Canvas(modifier = Modifier.fillMaxSize()) {
                val ovalWidth = size.width * 0.70f
                val ovalHeight = ovalWidth * 1.35f
                val ovalLeft = (size.width - ovalWidth) / 2f
                val ovalTop = (size.height - ovalHeight) / 2.3f

                val ovalPath = Path().apply {
                    addOval(Rect(ovalLeft, ovalTop, ovalLeft + ovalWidth, ovalTop + ovalHeight))
                }

                clipPath(ovalPath, clipOp = ClipOp.Difference) {
                    drawRect(color = Color.Black.copy(alpha = 0.65f))
                }

                val strokeColor = when {
                    uiState.isAutoSuccessShowing -> Color(0xFF22C55E) // Xanh lục thành công
                    uiState.needConfirmMatch != null -> Color(0xFFF59E0B) // Vàng cần duyệt
                    else -> Color.White.copy(alpha = 0.8f)
                }

                drawPath(
                    path = ovalPath,
                    color = strokeColor,
                    style = Stroke(width = 4.dp.toPx())
                )
            }

            // Top Status Pill
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (uiState.isAutoSuccessShowing) Color(0xFF15803D) else Color(0xCC0F172A)
                ),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (uiState.isAutoSuccessShowing) Icons.Default.CheckCircle else Icons.Default.Face,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = uiState.statusMessage,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Bottom Manual Scan Trigger (Or auto interval trigger)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Auto Success Banner Animation
                AnimatedVisibility(
                    visible = uiState.isAutoSuccessShowing,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF15803D))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                            Column {
                                Text(
                                    text = "ĐÃ ĐIỂM DANH THÀNH CÔNG",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = "${uiState.lastRecognized?.hoTen} (${uiState.lastRecognized?.mssv})",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Độ tin cậy: ${((uiState.lastRecognized?.similarity ?: 0f) * 100).toInt()}% (Auto-accept)",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                        }
                    }
                }

                // Shutter Button to capture & evaluate
                IconButton(
                    onClick = {
                        val bitmap = previewView?.bitmap ?: Bitmap.createBitmap(112, 112, Bitmap.Config.ARGB_8888)
                        viewModel.onFrameCaptured(bitmap, faceNetModel)
                    },
                    enabled = !uiState.isPaused && !uiState.isAutoSuccessShowing,
                    modifier = Modifier
                        .size(76.dp)
                        .background(if (uiState.isAutoSuccessShowing) Color(0xFF22C55E) else Color.White, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Điểm danh ngay",
                        tint = Color(0xFF0F172A),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            // Need Confirm Dialog (0.55 <= similarity < 0.65)
            uiState.needConfirmMatch?.let { match ->
                AlertDialog(
                    onDismissRequest = viewModel::dismissPendingMatch,
                    icon = { Icon(Icons.Default.HelpOutline, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(32.dp)) },
                    title = { Text("Xác Nhận Nhận Diện", fontWeight = FontWeight.Bold) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Phát hiện khuôn mặt có độ khớp trung bình:",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = "${match.hoTen} (${match.mssv})",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Độ tương đồng: ${((match.similarity) * 100).toInt()}% (Ngưỡng kiểm tra: 55% - 65%)",
                                fontSize = 12.sp,
                                color = Color(0xFFD97706),
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Giáo viên vui lòng xác nhận có đúng là sinh viên này không?",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = viewModel::confirmPendingMatch,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                        ) {
                            Text("Đúng (Xác nhận)")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = viewModel::dismissPendingMatch) {
                            Text("Bỏ qua")
                        }
                    }
                )
            }
        }
    }
}
