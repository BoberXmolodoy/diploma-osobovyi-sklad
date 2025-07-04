package com.example.diplom.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.navigation.NavController
import com.example.diplom.data.TokenManager
import com.example.diplom.ui.theme.CyberBackground
import com.example.diplom.ui.theme.CyberText
import com.example.diplom.viewmodel.AttendanceViewModel
import com.example.myapplication.R
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AttendanceDetailScreen(
    reportId: Int,
    navController: NavController, // ✅ додай цей параметр
    tokenManager: TokenManager,
    viewModel: AttendanceViewModel
) {
    val token = tokenManager.getAccessToken()
    val report by viewModel.selectedReport.collectAsState()

    LaunchedEffect(Unit) {
        android.util.Log.d("DETAIL_DEBUG", "📥 LaunchedEffect активувався з reportId=$reportId")
        if (!token.isNullOrEmpty()) {
            viewModel.fetchReportDetails(reportId, token)
        }
    }

    if (report == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val formattedDate = formatReportDateDetail(report!!.report_date)
    val absentCount = report!!.total_count - report!!.present_count

    val groupLabel = report!!.group_number?.let { "Група: $it" }
    val departmentLabel = report!!.department_name?.let { "Кафедра: $it" }
    val label = groupLabel ?: departmentLabel ?: "Підрозділ: —"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Деталі розходу", color = CyberText) },
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
                Text("🏷️ $label", fontSize = 20.sp, color = CyberText)
                Text(
                    "👥 Присутніх: ${report!!.present_count} / ${report!!.total_count} ($absentCount відсутніх)",
                    fontSize = 20.sp,
                    color = CyberText
                )

                Divider()

                Text("📋 Відсутні:", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = CyberText)

                if (report!!.absences.isEmpty()) {
                    Text("✅ Немає відсутніх", fontSize = 20.sp, color = CyberText)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(report!!.absences) { absence ->
                            Text("🔹 ${absence.full_name} — ${absence.reason}", fontSize = 18.sp, color = CyberText)
                        }
                    }
                }
            }
        }
    }

}

@RequiresApi(Build.VERSION_CODES.O)
fun formatReportDateDetail(isoDate: String): String {
    return try {
        val instant = Instant.parse(isoDate)
        val kyivZone = ZoneId.of("Europe/Kyiv")
        val zonedDateTime = instant.atZone(kyivZone)

        val formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy 'о' HH:mm", Locale("uk"))
        zonedDateTime.format(formatter)
    } catch (e: Exception) {
        isoDate
    }
}
