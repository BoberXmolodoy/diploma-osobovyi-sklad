package com.example.diplom.ui.screens

// 🔺 Додай цей import
import android.os.Build
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.diplom.data.TokenManager
import com.example.diplom.data.network.RetrofitClient
import com.example.diplom.data.repository.AttendanceRepository
import com.example.diplom.ui.theme.CyberBackground
import com.example.diplom.ui.theme.CyberCard
import com.example.diplom.ui.theme.CyberText
import com.example.diplom.ui.theme.SoftAccent
import com.example.diplom.viewmodel.AttendanceViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseCommanderScreen(
    navController: NavController,
    tokenManager: TokenManager,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val apiService = RetrofitClient.apiService
    val repository = AttendanceRepository(apiService)
    val viewModel: AttendanceViewModel = viewModel(
        factory = AttendanceViewModel.Factory(repository)
    )
    val courseReports by viewModel.todayReports.collectAsState()
    val missingGroups by viewModel.missingGroups.collectAsState()

    val today = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    LaunchedEffect(Unit) {
        val token = tokenManager.getAccessToken() ?: return@LaunchedEffect
        val courseId = tokenManager.getCourseId() ?: return@LaunchedEffect
        viewModel.fetchTodayReportsByCourse(courseId, token)
        viewModel.fetchMissingGroups(courseId, token)
    }

    Scaffold(
        containerColor = CyberBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Розходи за сьогодні",
                        color = CyberText
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)  // Зменшено відступ між елементами
        ) {
            item {
                Text(
                    text = "Останні подані розходи:",
                    color = SoftAccent,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.SansSerif,
                    modifier = Modifier.padding(bottom = 8.dp)  // Зменшено падінг знизу
                )
            }

            items(courseReports) { report ->
                val group = "Група ${report.groupNumber}"
                val present = report.presentCount
                val total = report.totalCount
                val absent = total - present

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CyberCard),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = group,
                            color = CyberText,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.SansSerif
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // 📝 Мітка "Змінено"
                        if (report.wasUpdated && !report.updatedAt.isNullOrBlank()) {
                            val time = try {
                                val instant = Instant.parse(report.updatedAt)
                                val kyivTime = instant.atZone(ZoneId.of("Europe/Kyiv"))
                                val formatter = DateTimeFormatter.ofPattern("HH:mm", Locale("uk"))
                                formatter.format(kyivTime)
                            } catch (e: Exception) { null }

                            time?.let {
                                Text(
                                    text = "📝 Змінено о $it",
                                    color = Color.Yellow,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }

                        Text(
                            text = "🟢 Присутні: $present",
                            color = Color(0xFF00E676),
                            fontSize = 14.sp
                        )

                        Text(
                            text = if (absent == 0) "✅ Усі на місці" else "🔴 Відсутні: $absent",
                            color = if (absent == 0) Color(0xFF00E676) else Color(0xFFFF5252),
                            fontSize = 14.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                navController.navigate("report_detail/${report.reportId}")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SoftAccent),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("🔍 Детальніше", color = CyberBackground)
                        }
                    }
                }
            }




            if (missingGroups.isNotEmpty()) {
                item {
                    Text(
                        text = "⚠️ Групи, які не подали розхід:",
                        color = Color.Yellow,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)  // Зменшено падінг
                    )
                }

                items(missingGroups) { group ->
                    Text(
                        text = "🔸 Група ${group.groupNumber}",
                        color = Color.Yellow,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(start = 8.dp, bottom = 2.dp)  // Зменшено падінг
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))  // Зменшено висоту спейсера
            }

            item {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val token = tokenManager.getAccessToken() ?: return@launch
                            val courseId = tokenManager.getCourseId() ?: return@launch
                            viewModel.submitCourseSummary(courseId, token)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, shape = RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "🧾 Згенерувати зведений розхід",
                        color = Color.White,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))  // Зменшено висоту спейсера
            }

            item {
                Button(
                    onClick = {
                        navController.navigate("summary_history")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SoftAccent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),  // Зменшено падінг
                ) {
                    Text("🕓 Історія зведених розходів", color = CyberText)
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))  // Зменшено висоту спейсера
            }

            item {
                val resultMessage by viewModel.summarySubmitResult.collectAsState()

                if (!resultMessage.isNullOrEmpty()) {
                    Text(
                        text = resultMessage ?: "",
                        color = if (resultMessage!!.startsWith("✅")) Color(0xFF00E676) else Color.Red,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    LaunchedEffect(resultMessage) {
                        kotlinx.coroutines.delay(3000)
                        viewModel.clearSummaryMessage()
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            tokenManager.clearTokens()
                            navController.navigate("login") {
                                popUpTo("login") { inclusive = true }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB22222)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, shape = RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Вийти",
                        color = Color.White,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
