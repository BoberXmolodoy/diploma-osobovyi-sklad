package com.example.diplom.ui.screens

import android.os.Build
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
import com.example.diplom.ui.theme.CyberBackground
import com.example.diplom.ui.theme.CyberCard
import com.example.diplom.ui.theme.CyberText
import com.example.diplom.viewmodel.AttendanceViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun LocationSummaryHistoryScreen(
    navController: NavController, // 🆕 додано
    tokenManager: TokenManager,
    viewModel: AttendanceViewModel
) {
    val token = tokenManager.getAccessToken()
    val locationId = tokenManager.getLocationId()

    val summaries by viewModel.locationSummaries.collectAsState()

    LaunchedEffect(Unit) {
        if (locationId != null && token != null) {
            viewModel.fetchLocationSummaryHistory(locationId, token)
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(summaries) { summary ->
                    val absent = summary.totalCount - summary.presentCount

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CyberCard)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "📅 ${formatDateq(summary.reportDate)}",
                                fontSize = 18.sp,
                                color = CyberText
                            )
                            Text(
                                text = "👥 Присутні: ${summary.presentCount} / ${summary.totalCount} ($absent відсутніх)",
                                fontSize = 16.sp,
                                color = CyberText
                            )
                            if (summary.wasUpdated == true && !summary.updatedAt.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "📝 Змінено о ${formatDateTimeUkr(summary.updatedAt)}",
                                    fontSize = 14.sp,
                                    color = CyberText
                                )
                            }
                        }
                    }
                }

            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun formatDateq(iso: String): String {
    return try {
        val parsedDate = Instant.parse(iso).atZone(ZoneId.of("Europe/Kyiv")).toLocalDate()
        parsedDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("uk")))
    } catch (e: Exception) {
        iso
    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun formatDateTimeUkr(dateTimeStr: String): String {
    return try {
        val instant = Instant.parse(dateTimeStr)
        val zoned = instant.atZone(ZoneId.of("Europe/Kyiv"))
        val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm", Locale("uk"))
        zoned.format(formatter)
    } catch (e: Exception) {
        "невідомо"
    }
}
