package com.example.diplom.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.diplom.viewmodel.AttendanceViewModel
import com.example.diplom.data.TokenManager
import com.example.diplom.data.model.AttendanceReportItem

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DepartmentAttendanceHistoryScreen(
    navController: NavController,
    viewModel: AttendanceViewModel = viewModel(),
    tokenManager: TokenManager,
    onReportClick: (AttendanceReportItem) -> Unit
) {
    val reportsState = viewModel.departmentReports.collectAsState()
    val reports = reportsState.value

    val departmentId = tokenManager.getDepartmentId()
    val token = tokenManager.getAccessToken()

    var shouldRefresh by remember { mutableStateOf(true) }

    // Слухаємо зміни з попереднього екрану
    LaunchedEffect(Unit) {
        navController.currentBackStackEntry
            ?.savedStateHandle
            ?.getLiveData<Boolean>("refreshDepartment")
            ?.observeForever {
                shouldRefresh = it
            }
    }

    // Якщо треба — оновлюємо список
    LaunchedEffect(departmentId, token, shouldRefresh) {
        if (shouldRefresh && departmentId != null && token != null) {
            viewModel.fetchReportsByDepartment(departmentId, token)
            shouldRefresh = false
        }
    }

    AttendanceHistoryScreen(
        navController = navController,
        reports = reports,
        onReportClick = onReportClick
    )
}

