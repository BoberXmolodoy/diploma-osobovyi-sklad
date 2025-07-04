package com.example.diplom.ui.screens

import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import com.example.diplom.data.TokenManager
import com.example.diplom.viewmodel.AttendanceViewModel
import com.example.diplom.ui.theme.CyberBackground
import com.example.diplom.ui.theme.CyberText
import com.example.diplom.ui.theme.SoftAccent
import com.example.myapplication.R
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun SummaryDetailScreen(
    summaryId: Int,
    navController: NavController,
    tokenManager: TokenManager,
    viewModel: AttendanceViewModel
) {
    val token = tokenManager.getAccessToken()
    val absences by viewModel.summaryAbsences.collectAsState()
    val summaryState by viewModel.selectedSummary.collectAsState()

    LaunchedEffect(summaryId) {
        if (!token.isNullOrEmpty()) {
            viewModel.fetchCourseSummary(summaryId, token)
            viewModel.fetchSummaryAbsences(summaryId, token)
        }
    }

    val summary = summaryState

    if (summary == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val absentCount = summary.absent_count
    val formattedDate = formatReportDateDetail1(summary.summary_date)

    Log.d("SUMMARY_DETAIL", "✅ Summary є: ID=${summary.id}, дата=${summary.summary_date}")
    Log.d("SUMMARY_DETAIL", "📋 Absences розмір: ${absences.size}")
    absences.forEachIndexed { index, it ->
        Log.d("SUMMARY_DETAIL", "🔸 [$index] ${it.full_name} — ${it.reason}")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Деталі зведеного розходу", color = CyberText) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад", tint = CyberText)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CyberBackground)
            )
        },
        containerColor = CyberBackground
    ) { padding ->
        val context = LocalContext.current
        val wordExportResult by viewModel.wordExportResult.collectAsState()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Image(
                painter = painterResource(id = R.drawable.institute_logo),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.08f)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 48.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("📅 Дата: $formattedDate", fontSize = 20.sp, color = CyberText)
                Text(
                    "👥 Присутніх: ${summary.present_count} / ${summary.total_count} ($absentCount відсутніх)",
                    fontSize = 20.sp,
                    color = CyberText
                )

                Divider()

                Text("📋 Відсутні:", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = CyberText)

                when {
                    absentCount > 0 && absences.isEmpty() -> {
                        CircularProgressIndicator()
                    }
                    absences.isEmpty() -> {
                        Text("✅ Немає відсутніх", fontSize = 20.sp, color = CyberText)
                    }
                    else -> {
                        val groupedAbsences = absences.groupBy { it.reason }

                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            groupedAbsences.forEach { (reason, group) ->
                                item {
                                    val names = group.joinToString(", ") { it.full_name }
                                    Text("🔹 $reason: $names", fontSize = 18.sp, color = CyberText)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (!token.isNullOrEmpty()) {
                            viewModel.exportSummaryToWord(summaryId, token)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SoftAccent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.CenterHorizontally)
                ) {
                    Text("⬇ Експортувати у Word")
                }

                wordExportResult?.let { responseBody ->
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            try {
                                val fileName = "summary_${summaryId}.docx"
                                val file = File(context.cacheDir, fileName)
                                responseBody.byteStream().use { input ->
                                    file.outputStream().use { output ->
                                        input.copyTo(output)
                                    }
                                }

                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.provider",
                                    file
                                )

                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }

                                context.startActivity(Intent.createChooser(intent, "Поділитися через:"))
                                viewModel.clearWordExportResult()

                            } catch (e: Exception) {
                                Log.e("SEND_FILE", "❌ Помилка при надсиланні файлу: ${e.message}")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SoftAccent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.CenterHorizontally)
                    ) {
                        Text("📤 Надіслати через Signal")
                    }
                }

            }
        }
    }


}

@RequiresApi(Build.VERSION_CODES.O)
fun formatReportDateDetail1(isoDate: String): String {
    return try {
        val instant = Instant.parse(isoDate)
        val kyivZone = ZoneId.of("Europe/Kyiv")
        val zonedDateTime = instant.atZone(kyivZone)
        val formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale("uk"))
        zonedDateTime.format(formatter)
    } catch (e: Exception) {
        isoDate
    }
}
