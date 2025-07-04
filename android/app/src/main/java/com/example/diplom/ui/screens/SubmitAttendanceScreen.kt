package com.example.diplom.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.example.diplom.ui.theme.CyberBackground
import com.example.diplom.ui.theme.CyberText
import com.example.diplom.ui.theme.SoftAccent
import com.example.diplom.viewmodel.AttendanceViewModel
import kotlinx.coroutines.launch
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubmitAttendanceScreen(
    navController: NavController,
    tokenManager: TokenManager,
    viewModel: AttendanceViewModel = viewModel()
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val submissionSuccess by viewModel.submissionSuccess.collectAsState()

    var totalCount by remember { mutableStateOf("") }
    var totalCountError by remember { mutableStateOf<String?>(null) }
    var sickError by remember { mutableStateOf<String?>(null) }
    var sickCount by remember { mutableStateOf("") }
    var sickNames by remember { mutableStateOf("") }
    var vacationCount by remember { mutableStateOf("") }
    var vacationNames by remember { mutableStateOf("") }
    var dutyCount by remember { mutableStateOf("") }
    var dutyNames by remember { mutableStateOf("") }
    var dismissalCount by remember { mutableStateOf("") }
    var dismissalNames by remember { mutableStateOf("") }
    var hospitalCount by remember { mutableStateOf("") }
    var hospitalNames by remember { mutableStateOf("") }
    var showOther by remember { mutableStateOf(false) }
    var otherReason by remember { mutableStateOf("") }
    var otherCount by remember { mutableStateOf("") }
    var otherNames by remember { mutableStateOf("") }

    val userRole = tokenManager.getUserRole() ?: ""
    val userName = tokenManager.getUserName() ?: ""
    val groupNumber = tokenManager.getGroupNumber()

    val displayRole = when (userRole) {
        "kg" -> "Командир групи"
        "nkf" -> "Начальник кафедри"
        "nk" -> "Начальник курсу"
        "nf" -> "Начальник факультету"
        else -> userRole
    }

    val userDisplay = if (userRole == "kg" && groupNumber != null) {
        "$displayRole $groupNumber — $userName"
    } else {
        "$displayRole — $userName"
    }

    fun validateAbsenceBlock(count: String, names: String, reason: String): String? {
        if ((count.toIntOrNull() ?: 0) > 0 && names.isBlank()) {
            return "У полі '$reason' вказано кількість, але не введено прізвища"
        }
        if (names.isNotBlank() && (count.toIntOrNull() == null || count.toInt() <= 0)) {
            return "У полі '$reason' вказано прізвища, але не введено кількість"
        }
        return null
    }



    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .background(CyberBackground)
            .padding(top = 24.dp, start = 20.dp, end = 20.dp, bottom = 20.dp)
    ) {
        Text("Ви увійшли як: $userDisplay", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = SoftAccent)
        Spacer(modifier = Modifier.height(12.dp))
        Text("Подання розходу", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = CyberText)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = totalCount,
            onValueChange = {
                val digitsOnly = it.filter { ch -> ch.isDigit() }.take(2)
                totalCount = digitsOnly

                totalCountError = when {
                    digitsOnly.isBlank() -> "Поле не може бути порожнім"
                    digitsOnly.toIntOrNull() == null || digitsOnly.toInt() < 0 -> "Введіть коректне число ≥ 0"
                    else -> null
                }
            },
            label = { Text("Загальна кількість", color = CyberText) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            isError = totalCountError != null,
            supportingText = {
                totalCountError?.let { Text(it, color = Color.Red) }
            },
            colors = attendanceTextFieldColors()
        )


        Spacer(modifier = Modifier.height(16.dp))
        Divider(color = SoftAccent)
        Spacer(modifier = Modifier.height(16.dp))

        Text("Причини відсутності", fontWeight = FontWeight.SemiBold, color = SoftAccent)

        ExpandableReasonBlock(
            title = "Хворі",
            count = sickCount,
            onCountChange = { sickCount = it },
            names = sickNames,
            onNamesChange = { sickNames = it },
            errorText = sickError
        )
        ExpandableReasonBlock("Відпустка", vacationCount, { vacationCount = it }, vacationNames, { vacationNames = it })
        ExpandableReasonBlock("Наряд", dutyCount, { dutyCount = it }, dutyNames, { dutyNames = it })
        ExpandableReasonBlock("Звільнення", dismissalCount, { dismissalCount = it }, dismissalNames, { dismissalNames = it })
        ExpandableReasonBlock("Шпиталь", hospitalCount, { hospitalCount = it }, hospitalNames, { hospitalNames = it })

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { showOther = !showOther },
            colors = ButtonDefaults.buttonColors(containerColor = SoftAccent)
        ) {
            Text(if (showOther) "➖ Сховати 'Інше'" else "➕ Додати 'Інше'", color = Color.Black)
        }

        if (showOther) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Інше", fontWeight = FontWeight.Bold, color = SoftAccent)
            OutlinedTextField(value = otherReason, onValueChange = { otherReason = it },
                label = { Text("Назва причини", color = CyberText) },
                modifier = Modifier.fillMaxWidth(),
                colors = attendanceTextFieldColors())
            OutlinedTextField(value = otherCount, onValueChange = { otherCount = it },
                label = { Text("Кількість", color = CyberText) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                colors = attendanceTextFieldColors())
            OutlinedTextField(value = otherNames, onValueChange = { otherNames = it },
                label = { Text("Прізвища", color = CyberText) },
                modifier = Modifier.fillMaxWidth(),
                colors = attendanceTextFieldColors())
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                try {
                    if (totalCount.isBlank()) {
                        totalCountError = "Поле не може бути порожнім"
                        return@Button
                    }

                    if (totalCount.contains(".") || totalCount.contains(",")) {
                        totalCountError = "Число має бути цілим"
                        return@Button
                    }

                    val totalInt = totalCount.toIntOrNull()
                    if (totalInt == null || totalInt < 0) {
                        totalCountError = "Введіть коректне число ≥ 0"
                        return@Button
                    }
                    totalCountError = null // очищення, якщо валідація пройдена

                    val token = tokenManager.getAccessToken() ?: throw Exception("Токен відсутній")
                    val reasonErrors = listOfNotNull(
                        validateAbsenceBlock(sickCount, sickNames, "Хворі"),
                        validateAbsenceBlock(vacationCount, vacationNames, "Відпустка"),
                        validateAbsenceBlock(dutyCount, dutyNames, "Наряд"),
                        validateAbsenceBlock(dismissalCount, dismissalNames, "Звільнення"),
                        validateAbsenceBlock(hospitalCount, hospitalNames, "Шпиталь"),
                        if (showOther) validateAbsenceBlock(otherCount, otherNames, otherReason.takeIf { it.isNotBlank() } ?: "Інше") else null
                    )

                    if (reasonErrors.isNotEmpty()) {
                        Toast.makeText(context, "❌ ${reasonErrors.first()}", Toast.LENGTH_LONG).show()
                        return@Button
                    }

                    val role = tokenManager.getUserRole()
                    val groupId = tokenManager.getGroupId()
                    val departmentId = tokenManager.getDepartmentId()

                    if (totalCount.isBlank()) {
                        Toast.makeText(context, "Введіть загальну кількість", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val absences = mutableListOf<Map<String, String>>()
                    if (sickCount.toIntOrNull() ?: 0 > 0) absences.add(mapOf("reason" to "Хворі", "full_name" to sickNames))
                    if (vacationCount.toIntOrNull() ?: 0 > 0) absences.add(mapOf("reason" to "Відпустка", "full_name" to vacationNames))
                    if (dutyCount.toIntOrNull() ?: 0 > 0) absences.add(mapOf("reason" to "Наряд", "full_name" to dutyNames))
                    if (dismissalCount.toIntOrNull() ?: 0 > 0) absences.add(mapOf("reason" to "Звільнення", "full_name" to dismissalNames))
                    if (hospitalCount.toIntOrNull() ?: 0 > 0) absences.add(mapOf("reason" to "Шпиталь", "full_name" to hospitalNames))
                    if (showOther && otherReason.isNotBlank() && otherCount.toIntOrNull() ?: 0 > 0)
                        absences.add(mapOf("reason" to otherReason, "full_name" to otherNames))

                    val total = totalCount.toIntOrNull() ?: throw Exception("Невірний формат загальної кількості")
                    val totalAbsent = listOf(
                        sickCount, vacationCount, dutyCount, dismissalCount, hospitalCount, otherCount
                    ).mapNotNull { it.toIntOrNull() }.sum()
                    val present = total - totalAbsent

                    if (role == "kg" && groupId == null) {
                        Toast.makeText(context, "❌ Не вказано groupId", Toast.LENGTH_LONG).show()
                        return@Button
                    }

                    if (role == "nkf" && departmentId == null) {
                        Toast.makeText(context, "❌ Не вказано departmentId", Toast.LENGTH_LONG).show()
                        return@Button
                    }

                    viewModel.submitAttendanceReportFlexible(
                        groupId = groupId,
                        departmentId = departmentId,
                        totalCount = total,
                        presentCount = present,
                        absences = absences,
                        token = token
                    ) { success, message ->
                        if (success) {
                            Toast.makeText(context, "✅ $message", Toast.LENGTH_LONG).show()

                            if (role == "kg" && groupId != null) {
                                viewModel.fetchReportsByGroup(groupId, token)
                            }
                            if (role == "nkf" && departmentId != null) {
                                viewModel.fetchReportsByDepartment(departmentId, token)
                            }


                        } else {
                            Toast.makeText(context, "❌ $message", Toast.LENGTH_LONG).show()
                        }
                    }


                } catch (e: Exception) {
                    Log.e("SubmitError", "❌ Помилка подачі розходу", e)
                    Toast.makeText(context, "❌ ${e.message}", Toast.LENGTH_LONG).show()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = SoftAccent)
        ) {
            Text("📤 Подати розхід", color = Color.White)
        }


        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = {
                when (userRole) {
                    "kg" -> navController.navigate("attendance_history")
                    "nkf" -> navController.navigate("department_history")
                    else -> {
                        Toast.makeText(context, "❌ Ця роль не має доступу до історії", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp, shape = RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SoftAccent)
        ) {
            Text(
                text = "📚 Історія розходів",
                color = Color.White,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            )
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

@Composable
fun ExpandableReasonBlock(
    title: String,
    count: String,
    onCountChange: (String) -> Unit,
    names: String,
    onNamesChange: (String) -> Unit,
    errorText: String? = null // ✅ додано
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
                    val sanitized = newValue.filter {
                        it.isLetter() || it == ' ' || it == '-' || it == '\'' || it == 'ʼ'
                    }
                    onNamesChange(sanitized)
                },
                label = { Text("Прізвища", color = CyberText) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                singleLine = false,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun attendanceTextFieldColors() = TextFieldDefaults.outlinedTextFieldColors(
    focusedBorderColor = SoftAccent,
    unfocusedBorderColor = CyberText,
    cursorColor = SoftAccent,
    focusedTextColor = CyberText,       // ✅ буде видно при фокусі
    unfocusedTextColor = CyberText,     // ✅ буде видно без фокусу
    errorBorderColor = Color.Red,
    errorCursorColor = Color.Red,
    errorLabelColor = Color.Red,
    errorTextColor = Color.Red
)

