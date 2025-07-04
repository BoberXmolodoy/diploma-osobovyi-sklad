package com.example.diplom.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.diplom.data.TokenManager
import com.example.diplom.data.model.DepartmentItem
import com.example.diplom.data.model.FacultyItem
import com.example.diplom.data.model.Location
import com.example.diplom.data.model.User
import com.example.diplom.data.network.RetrofitClient
import com.example.diplom.data.repository.AuthRepository
import com.example.diplom.ui.theme.*
import com.example.diplom.viewmodel.AuthViewModel
import com.example.diplom.viewmodel.AuthViewModelFactory
import com.example.myapplication.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddUserScreen(
    navController: NavController,
    role: String,
    tokenManager: TokenManager
) {
    val context = LocalContext.current
    val apiService = RetrofitClient.apiService
    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(AuthRepository(apiService, tokenManager), tokenManager)
    )

    var name by remember { mutableStateOf("") }
    var login by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var rank by remember { mutableStateOf("") }
    var groupIdText by remember { mutableStateOf("") }

    var nameError by remember { mutableStateOf(false) }
    var loginError by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf(false) }
    var rankError by remember { mutableStateOf(false) }
    var groupIdError by remember { mutableStateOf(false) }

    var isAdding by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    var parentUsers by remember { mutableStateOf<List<User>>(emptyList()) }
    var selectedParentId by remember { mutableStateOf<Int?>(null) }
    var expanded by remember { mutableStateOf(false) }

    var courseIdText by remember { mutableStateOf("") }
    var courseIdError by remember { mutableStateOf(false) }

    var faculties by remember { mutableStateOf<List<FacultyItem>>(emptyList()) }
    var selectedFacultyId by remember { mutableStateOf<Int?>(null) }
    var facultyExpanded by remember { mutableStateOf(false) }

    var departments by remember { mutableStateOf<List<DepartmentItem>>(emptyList()) }
    var selectedDepartmentId by remember { mutableStateOf<Int?>(null) }
    var departmentExpanded by remember { mutableStateOf(false) }

    var locations by remember { mutableStateOf<List<Location>>(emptyList()) }
    var selectedLocationId by remember { mutableStateOf<Int?>(null) }
    var locationExpanded by remember { mutableStateOf(false) }




    val normalizedRole = when (role) {
        "kg" -> "командир_групи"
        "nk" -> "начальник_курсу"
        "nf" -> "начальник_факультету"
        "nkf" -> "начальник_кафедри"
        "cl" -> "черговий_локації" // 🟢 ДОДАЙ ОЦЕ
        else -> role
    }

    LaunchedEffect(normalizedRole) {
        val managerRole = when (normalizedRole) {
            "начальник_курсу" -> "начальник_факультету"
            "командир_групи" -> "начальник_курсу"
            "черговий_локації", "начальник_кафедри" -> "начальник_факультету"
            else -> null
        }

        if (managerRole != null) {
            parentUsers = authViewModel.getUsersByRole(managerRole) ?: emptyList()
        }

        if (normalizedRole == "начальник_факультету") {
            faculties = authViewModel.getFaculties() ?: emptyList()
        }

        if (normalizedRole == "начальник_кафедри") {
            departments = authViewModel.getDepartments() ?: emptyList()
        }

        // 🆕 Додай ось це:
        if (normalizedRole == "черговий_локації") {
            locations = authViewModel.getLocations() ?: emptyList()
        }
        if (normalizedRole == "черговий_локації") {
            selectedParentId = null
        }

    }




    Box(modifier = Modifier.fillMaxSize().background(CyberBackground)) {
        Image(
            painter = painterResource(id = R.drawable.institute_logo),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().alpha(0.08f)
        )

        Card(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
                .align(Alignment.Center),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CyberCard),
            elevation = CardDefaults.cardElevation(10.dp)
        ) {


            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Назад",
                            tint = SoftAccent
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Додати нового користувача",
                        color = SoftAccent,
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 20.sp
                    )
                }

                CustomInputField("Ім'я", name, { name = it; nameError = false }, nameError)
                if (nameError) ErrorText("Ім’я має бути від 2 до 30 символів")

                Spacer(modifier = Modifier.height(10.dp))

                CustomInputField("Логін", login, { login = it; loginError = false }, loginError)
                if (loginError) ErrorText("Логін має бути від 3 до 20 символів")

                Spacer(modifier = Modifier.height(10.dp))

                CustomInputField(
                    "Пароль",
                    password,
                    { password = it; passwordError = false },
                    passwordError,
                    isPassword = true
                )
                if (passwordError) ErrorText("Пароль має бути від 6 до 50 символів")

                Spacer(modifier = Modifier.height(10.dp))

                CustomInputField(
                    "Військове звання",
                    rank,
                    { rank = it; rankError = false },
                    rankError
                )
                if (rankError) ErrorText("Звання має бути від 2 до 50 символів")

                if (normalizedRole  == "начальник_курсу") {
                    Spacer(modifier = Modifier.height(10.dp))
                    CustomInputField(
                        "Курс",
                        courseIdText,
                        { courseIdText = it; courseIdError = false },
                        courseIdError
                    )
                    if (courseIdError) ErrorText("Курс обов’язковий для начальника курсу")
                }


                if (normalizedRole  == "командир_групи") {
                    Spacer(modifier = Modifier.height(10.dp))
                    CustomInputField(
                        "Номер групи",
                        groupIdText,
                        { groupIdText = it; groupIdError = false },
                        groupIdError
                    )
                    if (groupIdError) ErrorText("Група обов’язкова для командира групи")
                }

                if (parentUsers.isNotEmpty() && normalizedRole != "черговий_локації") {
                    Spacer(modifier = Modifier.height(10.dp))
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = parentUsers.find { it.id == selectedParentId }?.name
                                ?: "Оберіть керівника",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Керівник", color = CyberText) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = CyberCard,
                                unfocusedContainerColor = CyberCard,
                                focusedTextColor = CyberText,
                                unfocusedTextColor = CyberText,
                                focusedIndicatorColor = SoftAccent,
                                unfocusedIndicatorColor = CyberText
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            parentUsers.forEach { user ->
                                DropdownMenuItem(
                                    text = { Text(user.name, fontFamily = FontFamily.SansSerif) },
                                    onClick = {
                                        selectedParentId = user.id
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                if ((normalizedRole  == "начальник_факультету" || normalizedRole  == "nf") && faculties.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    ExposedDropdownMenuBox(
                        expanded = facultyExpanded,
                        onExpandedChange = { facultyExpanded = !facultyExpanded }
                    ) {
                        OutlinedTextField(
                            value = faculties.find { it.id == selectedFacultyId }?.name
                                ?: "Оберіть факультет",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Факультет", color = CyberText) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = facultyExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = CyberCard,
                                unfocusedContainerColor = CyberCard,
                                focusedTextColor = CyberText,
                                unfocusedTextColor = CyberText,
                                focusedIndicatorColor = SoftAccent,
                                unfocusedIndicatorColor = CyberText
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = facultyExpanded,
                            onDismissRequest = { facultyExpanded = false }
                        ) {
                            faculties.forEach { faculty ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            faculty.name,
                                            fontFamily = FontFamily.SansSerif
                                        )
                                    },
                                    onClick = {
                                        selectedFacultyId = faculty.id
                                        facultyExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                if (normalizedRole == "начальник_кафедри" && departments.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    ExposedDropdownMenuBox(
                        expanded = departmentExpanded,
                        onExpandedChange = { departmentExpanded = !departmentExpanded }
                    ) {
                        OutlinedTextField(
                            value = departments.find { it.id == selectedDepartmentId }?.name ?: "Оберіть кафедру",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Кафедра", color = CyberText) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = departmentExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = CyberCard,
                                unfocusedContainerColor = CyberCard,
                                focusedTextColor = CyberText,
                                unfocusedTextColor = CyberText,
                                focusedIndicatorColor = SoftAccent,
                                unfocusedIndicatorColor = CyberText
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = departmentExpanded,
                            onDismissRequest = { departmentExpanded = false }
                        ) {
                            departments.forEach { department ->
                                DropdownMenuItem(
                                    text = { Text(department.name, fontFamily = FontFamily.SansSerif) },
                                    onClick = {
                                        selectedDepartmentId = department.id
                                        departmentExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                if (normalizedRole == "черговий_локації" && locations.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    ExposedDropdownMenuBox(
                        expanded = locationExpanded,
                        onExpandedChange = { locationExpanded = !locationExpanded }
                    ) {
                        OutlinedTextField(
                            value = locations.find { it.id == selectedLocationId }?.name ?: "Оберіть локацію",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Локація", color = CyberText) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = locationExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = CyberCard,
                                unfocusedContainerColor = CyberCard,
                                focusedTextColor = CyberText,
                                unfocusedTextColor = CyberText,
                                focusedIndicatorColor = SoftAccent,
                                unfocusedIndicatorColor = CyberText
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = locationExpanded,
                            onDismissRequest = { locationExpanded = false }
                        ) {
                            locations.forEach { location ->
                                DropdownMenuItem(
                                    text = { Text(location.name, fontFamily = FontFamily.SansSerif) },
                                    onClick = {
                                        selectedLocationId = location.id
                                        locationExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }




                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        nameError = name.length !in 2..30
                        loginError = login.length !in 3..20
                        passwordError = password.length !in 6..50
                        rankError = rank.length !in 2..50
                        groupIdError = normalizedRole  == "командир_групи" && groupIdText.isBlank()
                        courseIdError = normalizedRole  == "начальник_курсу" && courseIdText.isBlank()

                        val hasError = nameError || loginError || passwordError || rankError || groupIdError || courseIdError
                        val parentError = (normalizedRole  == "черговий_факультету" ||  normalizedRole  == "командир_групи") && selectedParentId == null
                        val facultyError = normalizedRole  == "начальник_факультету" && selectedFacultyId == null

                        if (parentError || facultyError) {
                            Toast.makeText(context, "Не заповнені обов'язкові поля", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        if (!hasError) {
                            isAdding = true

                            if (normalizedRole  == "командир_групи" && selectedParentId != null) {
                                authViewModel.getUserById(selectedParentId!!) { nkUser ->
                                    val courseIdString = nkUser?.course_id?.toString()

                                    if (courseIdString == null) {
                                        isAdding = false
                                        Toast.makeText(context, "❌ Не вдалося отримати курс керівника", Toast.LENGTH_LONG).show()
                                        return@getUserById
                                    }

                                    authViewModel.addUser(
                                        name = name,
                                        login = login,
                                        password = password,
                                        role = role,
                                        rank = rank,
                                        groupNumber = groupIdText,
                                        parentId = selectedParentId,
                                        courseId = courseIdString,
                                        facultyId = selectedDepartmentId
                                    ) { success, errorMessage, _ ->
                                        isAdding = false
                                        if (success) {
                                            Toast.makeText(context, "Користувача додано!", Toast.LENGTH_SHORT).show()
                                            navController.previousBackStackEntry?.savedStateHandle?.set("user_added", true)
                                            navController.popBackStack()
                                        } else {
                                            Toast.makeText(context, "Помилка: $errorMessage", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }

                            } else if ((normalizedRole  == "начальник_факультету" || normalizedRole  == "nf") && selectedFacultyId != null) {
                                authViewModel.addUser(
                                    name = name,
                                    login = login,
                                    password = password,
                                    role = role,
                                    rank = rank,
                                    groupNumber = null,
                                    parentId = null,
                                    courseId = null,
                                    facultyId = selectedFacultyId
                                ) { success, errorMessage, _ ->
                                    isAdding = false
                                    if (success) {
                                        Toast.makeText(context, "Користувача додано!", Toast.LENGTH_SHORT).show()
                                        navController.previousBackStackEntry?.savedStateHandle?.set("user_added", true)
                                        navController.popBackStack()
                                    } else {
                                        Toast.makeText(context, "Помилка: $errorMessage", Toast.LENGTH_LONG).show()
                                    }
                                }

                            }
                            else if (normalizedRole  == "начальник_кафедри" && selectedParentId != null) {
                                authViewModel.addUser(
                                    name = name,
                                    login = login,
                                    password = password,
                                    role = role,
                                    rank = rank,
                                    groupNumber = null,
                                    parentId = selectedParentId,
                                    courseId = null,
                                    facultyId = null // буде встановлено автоматично на бекенді
                                ) { success, errorMessage, _ ->
                                    isAdding = false
                                    if (success) {
                                        Toast.makeText(context, "Користувача додано!", Toast.LENGTH_SHORT).show()
                                        navController.previousBackStackEntry?.savedStateHandle?.set("user_added", true)
                                        navController.popBackStack()
                                    } else {
                                        Toast.makeText(context, "Помилка: $errorMessage", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }

                            else if (normalizedRole  == "начальник_курсу" && selectedParentId != null) {
                                authViewModel.addUser(
                                    name = name,
                                    login = login,
                                    password = password,
                                    role = role,
                                    rank = rank,
                                    groupNumber = null,
                                    parentId = selectedParentId,
                                    courseId = courseIdText,
                                    facultyId = null
                                ) { success, errorMessage, _ ->
                                    isAdding = false
                                    if (success) {
                                        Toast.makeText(context, "Користувача додано!", Toast.LENGTH_SHORT).show()
                                        navController.previousBackStackEntry?.savedStateHandle?.set("user_added", true)
                                        navController.popBackStack()
                                    } else {
                                        Toast.makeText(context, "Помилка: $errorMessage", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }

                            else if (normalizedRole == "черговий_локації" && selectedLocationId != null) {
                                authViewModel.addUser(
                                    name = name,
                                    login = login,
                                    password = password,
                                    role = role,
                                    rank = rank,
                                    groupNumber = null,
                                    parentId = null,              // 🔹 не має керівника
                                    courseId = null,
                                    facultyId = null,
                                    departmentId = null,
                                    locationId = selectedLocationId // ✅ передаємо саме як locationId
                                ) { success, errorMessage, _ ->
                                    isAdding = false
                                    if (success) {
                                        Toast.makeText(context, "Користувача додано!", Toast.LENGTH_SHORT).show()
                                        navController.previousBackStackEntry?.savedStateHandle?.set("user_added", true)
                                        navController.popBackStack()
                                    } else {
                                        Toast.makeText(context, "Помилка: $errorMessage", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }



                        } else {
                            Toast.makeText(context, "Будь ласка, перевірте поля!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isAdding,
                    colors = ButtonDefaults.buttonColors(containerColor = SoftAccent),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isAdding) {
                        CircularProgressIndicator(color = CyberCard, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Додати", color = CyberBackground, fontFamily = FontFamily.SansSerif)
                    }
                }
            }
        }
    }
}

@Composable
fun CustomInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean,
    isPassword: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = {
            Text(label, color = CyberText, fontFamily = FontFamily.SansSerif)
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        isError = isError,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.SansSerif),
        colors = TextFieldDefaults.colors(
            focusedTextColor = CyberText,
            unfocusedTextColor = CyberText,
            focusedContainerColor = CyberCard,
            unfocusedContainerColor = CyberCard,
            focusedIndicatorColor = SoftAccent,
            unfocusedIndicatorColor = CyberText,
            cursorColor = SoftAccent,
            errorContainerColor = CyberCard,
            errorIndicatorColor = Color.Red,
            errorLabelColor = SoftAccent
        )
    )
}

@Composable
fun ErrorText(text: String) {
    Text(
        text = text,
        color = Color(0xFFFF6B6B),
        fontSize = 13.sp,
        fontFamily = FontFamily.SansSerif,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 6.dp)
    )
}
