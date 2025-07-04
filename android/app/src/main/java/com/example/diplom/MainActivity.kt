package com.example.diplom

import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.diplom.data.TokenManager
import com.example.diplom.data.network.RetrofitClient
import com.example.diplom.data.repository.AuthRepository
import com.example.diplom.data.repository.AttendanceRepository
import com.example.diplom.ui.screens.*
import com.example.diplom.viewmodel.AttendanceViewModel
import com.example.diplom.viewmodel.AttendanceViewModelFactory
import org.json.JSONObject

class MainActivity : ComponentActivity() {

    private lateinit var tokenManager: TokenManager

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tokenManager = TokenManager(applicationContext)
        RetrofitClient.setTokenManager(tokenManager)

        val apiService = RetrofitClient.apiService
        val authRepository = AuthRepository(apiService, tokenManager)
        val attendanceRepository = AttendanceRepository(apiService)

        setContent {
            val navController = rememberNavController()
            val accessToken = tokenManager.getAccessToken()
            val userRole = getUserRole(accessToken)

            Log.d("MainActivity", "🔹 Отриманий токен: $accessToken")
            Log.d("MainActivity", "🔹 Розпізнана роль: $userRole")

            val factory = AttendanceViewModelFactory(attendanceRepository)
            val sharedAttendanceViewModel: AttendanceViewModel = viewModel(factory = factory)

            val startDestination = when (userRole) {
                "admin" -> "admin"
                "kg" -> "submit_attendance"
                "nk" -> "nk"
                "nf" -> "faculty"
                "nkf" -> "department_commander"
                "cl" -> "location_commander"
                else -> "login"
            }


            NavHost(navController = navController, startDestination = startDestination) {
                composable("login") {
                    LoginScreen(navController, authRepository, tokenManager)
                }
                composable("admin") {
                    AdminScreen(navController, tokenManager)
                }
                composable("admin_users/{role}") { backStackEntry ->
                    val role = backStackEntry.arguments?.getString("role") ?: ""
                    UserListScreen(navController, role, tokenManager)
                }
                composable("add_user/{role}") { backStackEntry ->
                    val role = backStackEntry.arguments?.getString("role") ?: ""
                    AddUserScreen(navController = navController, role = role, tokenManager = tokenManager)
                }
                composable("user_detail/{id}") { backStackEntry ->
                    val userId = backStackEntry.arguments?.getString("id")?.toIntOrNull()
                    if (userId != null) {
                        UserDetailScreen(navController = navController, userId = userId, tokenManager = tokenManager)
                    }
                }
                composable("edit_user/{id}") { backStackEntry ->
                    val userId = backStackEntry.arguments?.getString("id")?.toIntOrNull()
                    if (userId != null) {
                        EditUserScreen(navController = navController, userId = userId, tokenManager = tokenManager)
                    }
                }
                composable("submit_attendance") {
                    SubmitAttendanceScreen(navController, tokenManager, sharedAttendanceViewModel)
                }
                composable("attendance_history") {
                    LaunchedEffect(Unit) {
                        val groupId = tokenManager.getGroupId()
                        val token = tokenManager.getAccessToken()
                        if (groupId != null && !token.isNullOrEmpty()) {
                            sharedAttendanceViewModel.fetchReportsByGroup(groupId, token)
                        }
                    }

                    AttendanceHistoryScreen(
                        navController = navController,
                        reports = sharedAttendanceViewModel.groupReports.collectAsState().value,
                        onReportClick = {
                            navController.navigate("report_detail/${it.reportId}")
                        }
                    )

                }
                composable("location_commander") {
                    LocationCommanderScreen(
                        tokenManager = tokenManager,
                        navController = navController
                    )
                }
                composable("location_summary_history") {
                    LocationSummaryHistoryScreen(
                        navController = navController,
                        tokenManager = tokenManager,
                        viewModel = sharedAttendanceViewModel
                    )
                }


                composable("report_detail/{reportId}") { backStackEntry ->
                    val reportId = backStackEntry.arguments?.getString("reportId")?.toIntOrNull()
                    if (reportId != null) {
                        AttendanceDetailScreen(
                            reportId = reportId,
                            navController = navController, // ✅ додано
                            tokenManager = tokenManager,
                            viewModel = sharedAttendanceViewModel
                        )
                    }
                }

                composable("department_report_detail/{reportId}") { backStackEntry ->
                    val reportId = backStackEntry.arguments?.getString("reportId")?.toIntOrNull()
                    if (reportId != null) {
                        AttendanceDetailScreen(
                            reportId = reportId,
                            navController = navController, // ✅ додано
                            tokenManager = tokenManager,
                            viewModel = sharedAttendanceViewModel
                        )
                    }
                }




                composable("nk") {
                    CourseCommanderScreen(navController, tokenManager)
                }
                composable("faculty") {
                    FacultyCommanderScreen(
                        navController = navController,
                        tokenManager = tokenManager,
                    )
                }

                composable("department_commander") {
                    SubmitAttendanceScreen(
                        navController = navController,
                        tokenManager = tokenManager,
                        viewModel = sharedAttendanceViewModel
                    )
                }


                composable("summary_detail/{summaryId}") { backStackEntry ->
                    val summaryId = backStackEntry.arguments?.getString("summaryId")?.toIntOrNull()
                    if (summaryId != null) {
                        SummaryDetailScreen(
                            summaryId = summaryId,
                            navController = navController,
                            tokenManager = tokenManager,
                            viewModel = sharedAttendanceViewModel
                        )
                    }
                }

                composable("summary_history") {
                    SummaryHistoryScreen(
                        navController = navController,
                        tokenManager = tokenManager,
                        viewModel = sharedAttendanceViewModel
                    )
                }
                composable("department_history") {
                    DepartmentAttendanceHistoryScreen(
                        navController = navController, // ⬅️ ДОДАЙ сюди
                        tokenManager = tokenManager,
                        viewModel = sharedAttendanceViewModel,
                        onReportClick = {
                            navController.navigate("department_report_detail/${it.reportId}")
                        }
                    )
                }

            }
        }
    }

    private fun getUserRole(token: String?): String? {
        if (token.isNullOrEmpty()) return null
        return try {
            val parts = token.split(".")
            val payload = Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_WRAP)
            val jsonObject = JSONObject(String(payload))
            jsonObject.optString("role", null)
        } catch (e: Exception) {
            Log.e("MainActivity", "❌ Помилка декодування токена", e)
            null
        }
    }
}
