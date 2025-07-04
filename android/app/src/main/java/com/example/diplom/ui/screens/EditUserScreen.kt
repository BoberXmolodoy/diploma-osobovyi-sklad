package com.example.diplom.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.diplom.data.TokenManager
import com.example.diplom.data.model.DepartmentItem
import com.example.diplom.data.model.User
import com.example.diplom.data.network.RetrofitClient
import com.example.diplom.data.repository.AuthRepository
import com.example.diplom.viewmodel.AuthViewModel
import com.example.diplom.viewmodel.AuthViewModelFactory
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditUserScreen(navController: NavController, userId: Int, tokenManager: TokenManager) {
    val context = LocalContext.current
    val apiService = RetrofitClient.apiService
    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(AuthRepository(apiService, tokenManager), tokenManager)
    )

    val roleMapping = mapOf(
        "Начальник факультету" to "nf",
        "Начальник курсу" to "nk",
        "Командир групи" to "kg",
        "Командир відділення" to "kv",
        "Черговий факультету" to "cf",
        "Черговий локації" to "cl"
    )

    var user by remember { mutableStateOf<User?>(null) }
    var loading by remember { mutableStateOf(true) }

    var name by remember { mutableStateOf("") }
    var login by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("") }
    var rank by remember { mutableStateOf("") }
    var parentId by remember { mutableStateOf<Int?>(null) }

    var nameError by remember { mutableStateOf(false) }
    var loginError by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf(false) }
    var roleError by remember { mutableStateOf(false) }
    var rankError by remember { mutableStateOf(false) }


    var departments by remember { mutableStateOf<List<DepartmentItem>>(emptyList()) }
    var selectedDepartmentId by remember { mutableStateOf<Int?>(null) }
    var departmentExpanded by remember { mutableStateOf(false) }

    var faculties by remember { mutableStateOf<List<DepartmentItem>>(emptyList()) }
    var selectedFacultyId by remember { mutableStateOf<Int?>(null) }
    var facultyExpanded by remember { mutableStateOf(false) }


    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(userId) {
        authViewModel.getUserById(userId) { response ->
            if (response != null) {
                user = response
                name = response.name
                login = response.login
                rank = response.rank

                val displayRole = roleMapping.entries.firstOrNull { it.value == response.role }?.key ?: response.role
                role = displayRole
            } else {
                Toast.makeText(context, "Користувача не знайдено", Toast.LENGTH_SHORT).show()
                navController.popBackStack()
            }
            loading = false
        }

    }

    Scaffold(topBar = { TopAppBar(title = { Text("Деталі користувача") }) }) { paddingValues ->
        if (loading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = false
                    },
                    label = { Text("Ім'я") },
                    isError = nameError,
                    modifier = Modifier.fillMaxWidth()
                )
                if (nameError) {
                    Text("Ім’я має бути від 2 до 30 символів", color = MaterialTheme.colorScheme.error)
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = login,
                    onValueChange = {
                        login = it
                        loginError = false
                    },
                    label = { Text("Логін") },
                    isError = loginError,
                    modifier = Modifier.fillMaxWidth()
                )
                if (loginError) {
                    Text("Логін має бути від 3 до 20 символів", color = MaterialTheme.colorScheme.error)
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        passwordError = false
                    },
                    label = { Text("Новий пароль (необов'язково)") },
                    isError = passwordError,
                    modifier = Modifier.fillMaxWidth()
                )
                if (passwordError) {
                    Text("Пароль має бути від 6 до 50 символів", color = MaterialTheme.colorScheme.error)
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = role,
                    onValueChange = {
                        role = it
                        roleError = false
                    },
                    label = { Text("Роль") },
                    isError = roleError,
                    modifier = Modifier.fillMaxWidth()
                )
                if (roleError) {
                    Text("Роль має бути від 2 до 20 символів", color = MaterialTheme.colorScheme.error)
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = rank,
                    onValueChange = {
                        rank = it
                        rankError = false
                    },
                    label = { Text("Звання") },
                    isError = rankError,
                    modifier = Modifier.fillMaxWidth()
                )
                if (rankError) {
                    Text("Звання має бути від 2 до 50 символів", color = MaterialTheme.colorScheme.error)
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        nameError = name.length !in 2..30
                        loginError = login.length !in 3..20
                        passwordError = password.isNotBlank() && password.length !in 6..50
                        roleError = role.length !in 2..20
                        rankError = rank.length !in 2..50

                        val hasError = nameError || loginError || passwordError || roleError || rankError

                        if (!hasError) {
                            val backendRole = roleMapping[role] ?: role
                            coroutineScope.launch {
                                authViewModel.updateUser(
                                    id = userId,
                                    name = name,
                                    login = login,
                                    password = password,
                                    role = backendRole,
                                    rank = rank,
                                    isActive = true,
                                    parentId = parentId // 👈 Передано!
                                ) { success, errorMessage ->
                                    if (success) {
                                        Toast.makeText(context, "Користувача оновлено!", Toast.LENGTH_SHORT).show()
                                        navController.popBackStack()
                                    } else {
                                        Toast.makeText(context, errorMessage ?: "Помилка оновлення", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        } else {
                            Toast.makeText(context, "Будь ласка, перевірте поля!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Оновити користувача")
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        coroutineScope.launch {
                            authViewModel.deleteUser(userId) { success ->
                                if (success) {
                                    Toast.makeText(context, "Користувача видалено", Toast.LENGTH_SHORT).show()
                                    navController.popBackStack()
                                } else {
                                    Toast.makeText(context, "Помилка видалення користувача", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Видалити користувача")
                }
            }
        }
    }
}
