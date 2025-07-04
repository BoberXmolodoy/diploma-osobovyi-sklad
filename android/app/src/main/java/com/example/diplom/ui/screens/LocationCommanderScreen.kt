package com.example.diplom.ui.screens

import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
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
import com.example.diplom.data.model.MissingGroup
import com.example.diplom.data.network.RetrofitClient
import com.example.diplom.data.repository.AttendanceRepository
import com.example.diplom.ui.theme.CyberBackground
import com.example.diplom.ui.theme.CyberCard
import com.example.diplom.ui.theme.CyberText
import com.example.diplom.ui.theme.SoftAccent
import com.example.diplom.viewmodel.AttendanceViewModel
import com.example.diplom.viewmodel.AttendanceViewModelFactory
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationCommanderScreen(
    tokenManager: TokenManager,
    navController: NavController
) {
    val apiService = RetrofitClient.apiService
    val repository = AttendanceRepository(apiService)
    val viewModel: AttendanceViewModel = viewModel(factory = AttendanceViewModelFactory(repository))
    val coroutineScope = rememberCoroutineScope()

    val context = LocalContext.current
    val messageState = remember { mutableStateOf<String?>(null) }

    var missingGroups by remember { mutableStateOf<List<MissingGroup>>(emptyList()) }

    val locationId = tokenManager.getLocationId()
    val token = tokenManager.getAccessToken()

    val submittedReports by viewModel.submittedReports.collectAsState()

    LaunchedEffect(locationId) {
        if (locationId != null && token != null) {
            viewModel.fetchMissingGroupsByLocation(locationId, token) { groups ->
                missingGroups = groups
            }
            viewModel.fetchSubmittedReportsByLocation(locationId, token)
        }
    }

    // 🎯 Показати повідомлення через Toast
    LaunchedEffect(messageState.value) {
        messageState.value?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            messageState.value = null
        }
    }

    Scaffold(
        containerColor = CyberBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text("Сторінка чергового локації", color = CyberText)
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // 📌 Подані розходи
            if (submittedReports.isNotEmpty()) {
                Text(
                    text = "Останні подані розходи:",
                    color = SoftAccent,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.SansSerif,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                submittedReports.forEach { report ->
                    val group = "Група ${report.group_number}"
                    val present = report.present_count
                    val total = report.total_count
                    val absent = total - present

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = CyberCard),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = group,
                                color = CyberText,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
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

                            if (report.was_updated == true && !report.updated_at.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "📝 Змінено о ${formatDateTimeUkr(report.updated_at)}",
                                    fontSize = 14.sp,
                                    color = CyberText
                                )
                            }


                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // 🔻 Групи без подання
            if (missingGroups.isEmpty()) {
                Text(
                    text = "✅ Усі групи подали розхід!",
                    color = Color(0xFF00E676),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (locationId != null && token != null) {
                            viewModel.submitLocationSummary(locationId, token) { success, message ->
                                messageState.value = message
                                if (success) {
                                    viewModel.fetchSubmittedReportsByLocation(locationId, token)
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .shadow(4.dp, shape = RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "📤 Подати зведення по локації",
                        color = Color.White,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                }

            } else {
                Text(
                    text = "⚠️ Групи, які ще не подали розхід:",
                    color = Color.Yellow,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                missingGroups.forEach { group ->
                    Text(
                        text = "🔸 Група ${group.groupNumber}",
                        color = Color.Yellow,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    navController.navigate("location_summary_history")
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .shadow(4.dp, shape = RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "📖 Переглянути історію зведень",
                    color = Color.White,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
            }


            Spacer(modifier = Modifier.height(16.dp))

            // 🔘 Вийти
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

