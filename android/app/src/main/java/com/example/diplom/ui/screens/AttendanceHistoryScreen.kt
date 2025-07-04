package com.example.diplom.ui.screens

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.navigation.NavController
import com.example.diplom.data.model.AttendanceReportItem
import com.example.diplom.ui.theme.*
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceHistoryScreen(
    navController: NavController, // 🔹 Додай це
    reports: List<AttendanceReportItem>,
    onReportClick: (AttendanceReportItem) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Історія розходів",
                        color = CyberText,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CyberBackground)
        ) {
            if (reports.isEmpty()) {
                EmptyHistoryState(modifier = Modifier.padding(padding))
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(reports) { report ->
                        AttendanceReportCard(report = report, onClick = {
                            Log.d("HISTORY_CLICK", "➡️ Натиснули на reportId = ${report.reportId}")
                            Log.d("CARD_DEBUG", "📝 wasUpdated = ${report.wasUpdated}, updatedAt = ${report.updatedAt}")
                            onReportClick(report) // ця функція вже буде обгорнута в DepartmentAttendanceHistoryScreen

                        })

                    }
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AttendanceReportCard(
    report: AttendanceReportItem,
    onClick: () -> Unit
) {
    val formattedDate = formatReportDate(report.reportDate)

    val absentCount = report.totalCount - report.presentCount
    val attendanceText = if (report.totalCount == 0) {
        "—"
    } else {
        "${report.presentCount} / ${report.totalCount} ($absentCount відсутн${if (absentCount == 1) "ій" else "іх"})"
    }

    val updatedLabel = remember(report.wasUpdated, report.updatedAt) {
        if (report.wasUpdated == true && !report.updatedAt.isNullOrBlank()) {
            try {
                val updatedTime = java.time.Instant.parse(report.updatedAt)
                    .atZone(java.time.ZoneId.of("Europe/Kyiv"))
                    .format(DateTimeFormatter.ofPattern("HH:mm", Locale("uk")))
                "📝 Змінено о $updatedTime"
            } catch (e: Exception) {
                null
            }
        } else null
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(6.dp),
        colors = CardDefaults.cardColors(containerColor = CyberCard)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // 🔹 Дата розходу
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "📅", fontSize = 18.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = formattedDate,
                    color = CyberText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (updatedLabel != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = updatedLabel,
                    color = Color.Yellow,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 🔹 Присутність + відсутні
            Row {
                Text(
                    text = "👥 Присутніх: ",
                    color = SoftAccent,
                    fontSize = 14.sp
                )
                Text(
                    text = attendanceText,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Divider(color = CyberText.copy(alpha = 0.3f), thickness = 1.dp)

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onClick,
                modifier = Modifier.align(Alignment.End),
                colors = ButtonDefaults.buttonColors(containerColor = SoftAccent)
            ) {
                Text("Детальніше", color = Color.Black)
            }
        }
    }
}

@Composable
fun EmptyHistoryState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("📭", fontSize = 42.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Ще не подано жодного розходу",
                color = CyberText.copy(alpha = 0.6f),
                fontSize = 16.sp
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun formatReportDate(isoDate: String): String {
    return try {
        val instant = java.time.Instant.parse(isoDate)
        val kyivZone = java.time.ZoneId.of("Europe/Kyiv")
        val zonedDateTime = instant.atZone(kyivZone)

        val formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy 'о' HH:mm", Locale("uk"))
        zonedDateTime.format(formatter)
    } catch (e: Exception) {
        isoDate
    }
}
