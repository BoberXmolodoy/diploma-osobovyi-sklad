package com.example.diplom.ui.screens

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.diplom.data.TokenManager
import com.example.diplom.viewmodel.AttendanceViewModel
import com.example.diplom.ui.theme.CyberBackground
import com.example.diplom.ui.theme.CyberCard
import com.example.diplom.ui.theme.CyberText
import com.example.diplom.ui.theme.SoftAccent
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.util.*
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun SummaryHistoryScreen(
    navController: NavController,
    tokenManager: TokenManager,
    viewModel: AttendanceViewModel
) {
    val token = tokenManager.getAccessToken()
    val role = tokenManager.getUserRole()
    val courseId = tokenManager.getCourseId()
    val facultyId = tokenManager.getFacultyId()

    val coroutineScope = rememberCoroutineScope()

    val courseSummaries by viewModel.courseSummaries.collectAsState()
    val facultySummaries by viewModel.facultySummaries.collectAsState()

    // ⬇️ Для НФ — беремо тільки зведення, які НЕ мають course_id (тобто створені по факультету)
    val summaries = when (role) {
        "nk" -> courseSummaries
        "nf" -> facultySummaries.filter { it.course_id == null || it.course_id == 0 }
        else -> emptyList()
    }

    LaunchedEffect(role, courseId, facultyId, token) {
        Log.d("SUMMARY_SCREEN", "🪪 role=$role, courseId=$courseId, facultyId=$facultyId")
        if (!token.isNullOrEmpty()) {
            when (role) {
                "nk" -> courseId?.let { viewModel.fetchCourseSummaries(it, token) }
                "nf" -> facultyId?.let { viewModel.fetchFacultySummaries(it, token) }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Історія зведених розходів", color = CyberText) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Назад",
                            tint = CyberText
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CyberBackground)
            )
        },
        containerColor = CyberBackground

    ) { padding ->
        if (summaries.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Немає зведених звітів", color = CyberText, fontSize = 18.sp)
            }
        } else {
            summaries.forEach {
                Log.d("SUMMARY_CHECK", "📦 summary_id=${it.id}, course_id=${it.course_id}, faculty_id=${it.faculty_id}")
                Log.d("SUMMARY_CHECK", "📝 was_updated=${it.was_updated}, updated_at=${it.updated_at}")

            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(summaries) { summary ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CyberCard)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "📅 ${formatDate(summary.summary_date)}",
                                fontSize = 18.sp,
                                color = CyberText
                            )

                            Text(
                                text = "👥 Присутніх: ${summary.present_count} / ${summary.total_count} (${summary.absent_count} відсутніх)",
                                fontSize = 16.sp,
                                color = CyberText
                            )

                            if (summary.reasons.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Divider(color = CyberText.copy(alpha = 0.3f))
                                Spacer(modifier = Modifier.height(4.dp))
                                summary.reasons.forEach {
                                    Text(
                                        text = "• ${it.reason} – ${it.count}",
                                        fontSize = 14.sp,
                                        color = CyberText
                                    )
                                }

                                if (summary.was_updated == true && !summary.updated_at.isNullOrBlank()) {
                                    val formattedUpdate = try {
                                        val instant = Instant.parse(summary.updated_at)
                                        val zoned = instant.atZone(ZoneId.of("Europe/Kyiv"))
                                        val formatter = DateTimeFormatter.ofPattern("📝 Змінено о HH:mm", Locale("uk"))
                                        zoned.format(formatter)
                                    } catch (e: Exception) {
                                        "📝 Змінено"
                                    }

                                    Text(
                                        text = formattedUpdate,
                                        fontSize = 14.sp,
                                        color = CyberText
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                            }
                            }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Button(
                                onClick = {
                                    Log.d("SUMMARY_HISTORY", "➡️ Обрано summary: id=${summary.id}, дата=${summary.summary_date}")
                                    viewModel.setSelectedSummary(summary)
                                    coroutineScope.launch {
                                        kotlinx.coroutines.delay(100)
                                        navController.navigate("summary_detail/${summary.id}")
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SoftAccent)
                            ) {
                                Text("Детальніше", color = CyberText)
                            }
                        }

                    }
                    }
                }
            }
        }
    }

@RequiresApi(Build.VERSION_CODES.O)
fun formatDate(iso: String): String {
    return try {
        val parsedDate = Instant.parse(iso)
            .atZone(ZoneId.of("Europe/Kyiv"))
            .toLocalDate()
        parsedDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("uk")))
    } catch (e: Exception) {
        iso
    }
}
