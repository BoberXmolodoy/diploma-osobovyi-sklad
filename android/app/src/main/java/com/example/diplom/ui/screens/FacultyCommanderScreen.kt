package com.example.diplom.ui.screens

import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.diplom.data.TokenManager
import com.example.diplom.data.model.DepartmentItem
import com.example.diplom.data.network.RetrofitClient
import com.example.diplom.data.repository.AttendanceRepository
import com.example.diplom.data.repository.AuthRepository
import com.example.diplom.ui.theme.*
import com.example.diplom.viewmodel.AttendanceViewModel
import com.example.diplom.viewmodel.AuthViewModel
import com.example.diplom.viewmodel.AuthViewModelFactory
import kotlinx.coroutines.launch
import androidx.compose.foundation.clickable
import com.example.diplom.data.model.FacultySummaryRequest
import com.example.diplom.data.model.OfficerAbsenceRequest


@OptIn(ExperimentalMaterial3Api::class)
@Composable
@RequiresApi(Build.VERSION_CODES.O)

fun FacultyCommanderScreen(
    navController: NavController,
    tokenManager: TokenManager,
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val apiService = RetrofitClient.apiService
    val attendanceRepository = AttendanceRepository(apiService)
    val authRepository = AuthRepository(apiService, tokenManager)

    val attendanceViewModel: AttendanceViewModel = viewModel(
        factory = AttendanceViewModel.Factory(attendanceRepository)
    )
    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(authRepository, tokenManager)
    )

    val facultySummaries by attendanceViewModel.todayFacultySummaries.collectAsState()
    val missingCourses by attendanceViewModel.missingCourses.collectAsState()
    val departmentReports by attendanceViewModel.todayDepartmentReports.collectAsState()
    var missingDepartments by remember { mutableStateOf<List<DepartmentItem>>(emptyList()) }

    val predefinedReasons = listOf("Хворі", "Відрядження", "Відпустка", "Наряд", "Звільнення", "Шпиталь")
    var selectedReason by remember { mutableStateOf(predefinedReasons.first()) }


    val filteredMissingDepartments = remember(missingDepartments, departmentReports) {
        missingDepartments.filter { missing ->
            departmentReports.none { report -> report.department_name == missing.name }
        }
    }

    LaunchedEffect(Unit) {
        val token = tokenManager.getAccessToken() ?: return@LaunchedEffect
        val facultyId = tokenManager.getFacultyId() ?: return@LaunchedEffect
        attendanceViewModel.fetchTodaySummariesByFaculty(facultyId, token)
        attendanceViewModel.fetchMissingCourses(facultyId, token)
        attendanceViewModel.fetchTodayDepartmentReports(facultyId, token)
        missingDepartments = authViewModel.getMissingDepartments(facultyId) ?: emptyList()
    }

    // 🟡 ДОДАЙ це у верхній частині Composable
    var showDialog by remember { mutableStateOf(false) }
    var presentOfficers by remember { mutableStateOf("") }
    var presentOfficersError by remember { mutableStateOf<String?>(null) }


    var newReason by remember { mutableStateOf("") }
    var newCount by remember { mutableStateOf("") }
    var newNames by remember { mutableStateOf("") }


    Scaffold(
        containerColor = CyberBackground,
        topBar = {
            TopAppBar(
                title = { Text("Розходи за сьогодні (Факультет)", color = CyberText) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()) // ✅ скрол
                .padding(16.dp)
        ) {
            Text(
                text = "Останні подані розходи:",
                color = SoftAccent,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.SansSerif,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            facultySummaries.forEach { summary ->
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
                            text = "Курс ${summary.course_number}",
                            color = CyberText,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Text("🟢 Присутні: ${summary.present_count}", color = Color(0xFF00E676))

                        // 📝 Додатковий надпис про зміну
                        if (summary.updated_at != null && summary.updated_at != summary.summary_date) {
                            val editedTime = summary.updated_at.substringAfter("T").substring(0, 5)
                            Text(
                                text = "📝 Змінено о $editedTime",
                                color = Color.Yellow,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        Text(
                            text = if (summary.absent_count == 0) "✅ Усі на місці"
                            else "🔴 Відсутні: ${summary.absent_count}",
                            color = if (summary.absent_count == 0) Color(0xFF00E676) else Color(0xFFFF5252)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                navController.navigate("summary_detail/${summary.id}")
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


            if (departmentReports.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "📚 Розходи по кафедрах:",
                    color = SoftAccent,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.SansSerif,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                departmentReports.forEach { report ->
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
                                text = "Кафедра: ${report.department_name}",
                                color = CyberText,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text("🟢 Присутні: ${report.present_count}", color = Color(0xFF00E676))

                            val absentCount = report.total_count - report.present_count
                            Text(
                                text = if (absentCount == 0) "✅ Усі на місці"
                                else "🔴 Відсутні: $absentCount",
                                color = if (absentCount == 0) Color(0xFF00E676) else Color(0xFFFF5252)
                            )

                            // 📝 Показ мітки про зміну, якщо була
                            if (report.was_updated == true && !report.updated_at.isNullOrBlank()) {
                                val editedTime = try {
                                    java.time.Instant.parse(report.updated_at)
                                        .atZone(java.time.ZoneId.of("Europe/Kyiv"))
                                        .toLocalTime()
                                        .toString()
                                        .substring(0, 5)
                                } catch (e: Exception) {
                                    null
                                }

                                editedTime?.let {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "📝 Змінено о $it",
                                        color = Color.Yellow,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = {
                                    navController.navigate("departmentReportDetail/${report.report_id}")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SoftAccent),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.align(Alignment.Start)
                            ) {
                                Text("🔍 Детальніше", color = CyberBackground)
                            }
                        }
                    }
                }



            }

            if (missingCourses.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "⚠️ Курси, які не подали розхід:",
                    color = Color.Yellow,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                missingCourses.forEach {
                    Text(
                        text = "🔸 Курс ${it.course_number}",
                        color = Color.Yellow,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                    )
                }
            }

            if (filteredMissingDepartments.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "⚠️ Кафедри, які не подали розхід:",
                    color = Color(0xFFFFA000),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                filteredMissingDepartments.forEach {
                    Text(
                        text = "🔸 ${it.name}",
                        color = Color(0xFFFFC107),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { showDialog = true }, // відкриває діалог
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(3.dp, shape = RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("📤 Подати зведений розхід по факультету", color = Color.White)
            }

            if (showDialog) {
                val reasonStates = remember(predefinedReasons) {
                    predefinedReasons.associateWith {
                        mutableStateOf("") to mutableStateOf("")
                    }
                }
                val reasonErrors = remember(predefinedReasons) {
                    predefinedReasons.associateWith { mutableStateOf<String?>(null) }
                }

                AlertDialog(
                    onDismissRequest = { showDialog = false },
                    confirmButton = {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val token = tokenManager.getAccessToken() ?: return@launch
                                    val facultyId = tokenManager.getFacultyId() ?: return@launch

                                    // 🛑 Валідація для присутніх офіцерів
                                    if (presentOfficers.isBlank()) {
                                        presentOfficersError = "Поле не може бути порожнім"
                                        return@launch
                                    }
                                    if (presentOfficers.toIntOrNull() == null || presentOfficers.toInt() < 0) {
                                        presentOfficersError = "Введіть коректне число ≥ 0"
                                        return@launch
                                    }
                                    presentOfficersError = null // ✅ Все гаразд

                                    val presentOfficersSnapshot = presentOfficers.toInt()

                                    // ✅ Додаткова валідація для всіх причин
                                    reasonStates.forEach { (reason, pair) ->
                                        val count = pair.first.value
                                        val names = pair.second.value

                                        if (count.isNotBlank() && names.isBlank()) {
                                            Toast.makeText(context, "Укажіть прізвища для причини: $reason", Toast.LENGTH_LONG).show()
                                            return@launch
                                        }

                                        if (names.isNotBlank() && count.isBlank()) {
                                            Toast.makeText(context, "Укажіть кількість для причини: $reason", Toast.LENGTH_LONG).show()
                                            return@launch
                                        }
                                    }

                                    // 🔄 Формуємо список об'єктів OfficerAbsenceRequest
                                    val absences = reasonStates.mapNotNull { (reason, pair) ->
                                        val count = pair.first.value
                                        val names = pair.second.value
                                        if (count.isNotBlank() && names.isNotBlank()) {
                                            OfficerAbsenceRequest(
                                                reason = reason,
                                                names = names,
                                                count = count.toIntOrNull() ?: 0
                                            )
                                        } else null
                                    }

                                    val request = FacultySummaryRequest(
                                        presentOfficersCount = presentOfficersSnapshot,
                                        absencesOfficers = absences
                                    )

                                    attendanceViewModel.submitFacultySummary(
                                        facultyId = facultyId,
                                        token = token,
                                        request = request
                                    ) { success, message ->
                                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                        if (success) showDialog = false
                                    }
                                }
                            }

                            ,
                            colors = ButtonDefaults.buttonColors(containerColor = SoftAccent),
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(2.dp, RoundedCornerShape(12.dp)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Подати розхід", color = CyberBackground)
                        }
                    }

                    ,
                    dismissButton = {
                        Button(
                            onClick = { showDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB22222)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(2.dp, RoundedCornerShape(12.dp)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Відмінити", color = Color.White)
                        }
                    },
                    title = {
                        Text(
                            "Подання зведеного розходу по факультету",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberText
                        )
                    },
                    text = {
                        Column(
                            modifier = Modifier
                                .verticalScroll(rememberScrollState())
                                .fillMaxWidth()
                        ) {
                            Text("📌 Подані кафедри:", color = SoftAccent, fontWeight = FontWeight.SemiBold)
                            departmentReports.forEach {
                                Text("✓ ${it.department_name} (${it.present_count} присутніх)", color = CyberText)
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text("👮 Офіцери факультету:", color = SoftAccent, fontWeight = FontWeight.SemiBold)

                            OutlinedTextField(
                                value = presentOfficers,
                                onValueChange = {
                                    val digitsOnly = it.filter { ch -> ch.isDigit() }.take(2)
                                    presentOfficers = digitsOnly
                                    presentOfficersError = null // очищення помилки
                                }
                                ,
                                label = { Text("Присутні офіцери") },
                                modifier = Modifier.fillMaxWidth(),
                                isError = presentOfficersError != null,
                                supportingText = {
                                    presentOfficersError?.let { Text(it, color = Color.Red) }
                                },
                                colors = TextFieldDefaults.outlinedTextFieldColors(
                                    focusedBorderColor = SoftAccent,
                                    unfocusedBorderColor = CyberText,
                                    focusedTextColor = CyberText,
                                    unfocusedTextColor = CyberText,
                                    cursorColor = SoftAccent,
                                    errorBorderColor = Color.Red,
                                    errorTextColor = Color.Red
                                ),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )


                            Spacer(modifier = Modifier.height(12.dp))

                            Text("Причини відсутності:", color = SoftAccent, fontWeight = FontWeight.SemiBold)

                            predefinedReasons.forEach { reason ->
                                val (countState, namesState) = reasonStates[reason]!!
                                ExpandableReasonBlockForOfficers(
                                    title = reason,
                                    count = countState.value,
                                    onCountChange = { countState.value = it },
                                    names = namesState.value,
                                    onNamesChange = { namesState.value = it },
                                    errorText = reasonErrors[reason]?.value // ✅ Ось це додаємо
                                )
                            }

                        }
                    },
                    containerColor = CyberCard,
                    shape = RoundedCornerShape(16.dp),
                    tonalElevation = 6.dp
                )
            }





            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { navController.navigate("summary_history") },
                colors = ButtonDefaults.buttonColors(containerColor = SoftAccent),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("🕓 Історія зведених розходів", color = CyberText)
            }

            Spacer(modifier = Modifier.height(16.dp))

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
                Text("Вийти", color = Color.White, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpandableReasonBlockForOfficers(
    title: String,
    count: String,
    onCountChange: (String) -> Unit,
    names: String,
    onNamesChange: (String) -> Unit,
    errorText: String? // ✅ Додано параметр для показу помилок
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, fontWeight = FontWeight.Bold, color = SoftAccent)
            Icon(
                imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = SoftAccent
            )
        }

        if (expanded) {
            OutlinedTextField(
                value = count,
                onValueChange = { newValue ->
                    val digitsOnly = newValue.filter { it.isDigit() }.take(2)
                    onCountChange(digitsOnly)
                },
                label = { Text("Кількість", color = CyberText) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                colors = attendanceTextFieldColors()
            )

            OutlinedTextField(
                value = names,
                onValueChange = { newValue ->
                    val sanitized = newValue.filter { it.isLetter() || it == ' ' || it == '-' || it == ',' }
                    onNamesChange(sanitized)
                },
                label = { Text("Прізвища", color = CyberText) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                colors = attendanceTextFieldColors()
            )

            if (!errorText.isNullOrBlank()) {
                Text(
                    text = errorText,
                    color = Color.Red,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}


