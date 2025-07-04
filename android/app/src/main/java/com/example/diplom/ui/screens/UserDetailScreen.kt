package com.example.diplom.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.diplom.data.TokenManager
import com.example.diplom.data.model.User
import com.example.diplom.data.network.RetrofitClient
import com.example.diplom.data.repository.AuthRepository
import com.example.diplom.ui.theme.*
import com.example.diplom.viewmodel.AuthViewModel
import com.example.diplom.viewmodel.AuthViewModelFactory
import com.example.myapplication.R
import kotlinx.coroutines.launch

fun getManagerRole(role: String): String? {
    return when (role) {
        "kg" -> "nk"
        "nk", "cf", "cl" -> "nf"
        else -> null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserDetailScreen(navController: NavController, userId: Int, tokenManager: TokenManager) {
    val context = LocalContext.current
    val apiService = RetrofitClient.apiService
    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(AuthRepository(apiService, tokenManager), tokenManager)
    )

    var user by remember { mutableStateOf<User?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    var name by remember { mutableStateOf("") }
    var login by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("") }
    var rank by remember { mutableStateOf("") }
    var groupNumber by remember { mutableStateOf("") }

    var parentUsers by remember { mutableStateOf<List<User>>(emptyList()) }
    var selectedParentId by remember { mutableStateOf<Int?>(null) }
    var expanded by remember { mutableStateOf(false) }

    var courseId by remember { mutableStateOf("") }


    val coroutineScope = rememberCoroutineScope()

    fun loadManagersForRole(role: String) {
        coroutineScope.launch {
            val managerRole = getManagerRole(role)
            parentUsers = managerRole?.let {
                authViewModel.getUsersByRole(it)
            } ?: emptyList()
        }
    }

    fun fetchGroupNumber(groupId: Int?) {
        Log.d("UserDetailScreen", "➡️ fetchGroupNumber groupId=$groupId")
        coroutineScope.launch {
            if (groupId == null) {
                Log.e("UserDetailScreen", "❌ groupId is null")
                return@launch
            }
            try {
                val number = authViewModel.getGroupNumberByGroupId(groupId)
                Log.d("UserDetailScreen", "✅ Отримано номер групи: $number")
                if (number != null) {
                    groupNumber = number
                } else {
                    Log.e("UserDetailScreen", "❌ groupNumber == null")
                }
            } catch (e: Exception) {
                Log.e("UserDetailScreen", "❌ Помилка при fetchGroupNumber: ${e.message}")
            }
        }
    }


    LaunchedEffect(userId) {
        authViewModel.getUserById(userId) { response ->
            if (response != null) {
                Log.d("UserDetailScreen", "✅ groupId отримано з бекенду: ${response.groupId}")

                user = response
                name = response.name
                login = response.login
                role = response.role
                rank = response.rank
                selectedParentId = response.parent_id

                if (response.role == "nk") {
                    courseId = response.course_id?.toString() ?: ""
                }

                if (response.role == "kg") {
                    fetchGroupNumber(response.groupId)
                }

                loadManagersForRole(response.role)
            } else {
                error = "Користувача не знайдено"
            }
            loading = false
        }
    }



    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBackground)
    ) {
        Image(
            painter = painterResource(id = R.drawable.institute_logo),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.08f)
        )

        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = SoftAccent
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Деталі користувача",
                    color = SoftAccent,
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 20.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (error != null) {
                    Text(error!!, color = Color.Red)
                } else {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Ім’я", color = CyberText) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.SansSerif),
                        colors = textFieldColors()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = login,
                        onValueChange = { login = it },
                        label = { Text("Логін", color = CyberText) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.SansSerif),
                        colors = textFieldColors()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Новий пароль", color = CyberText) },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.SansSerif),
                        colors = textFieldColors()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = role,
                        onValueChange = {
                            role = it
                            loadManagersForRole(it)
                        },
                        label = { Text("Роль", color = CyberText) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.SansSerif),
                        colors = textFieldColors()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = rank,
                        onValueChange = { rank = it },
                        label = { Text("Звання", color = CyberText) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.SansSerif),
                        colors = textFieldColors()
                    )

                    if (role == "nk") {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = courseId,
                            onValueChange = { courseId = it },
                            label = { Text("Курс", color = CyberText) },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.SansSerif),
                            colors = textFieldColors()
                        )
                    }


                    if (role == "kg") {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = groupNumber,
                            onValueChange = { groupNumber = it },
                            label = { Text("Номер групи", color = CyberText) },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.SansSerif),
                            colors = textFieldColors()
                        )
                    }

                    if (parentUsers.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded }
                        ) {
                            OutlinedTextField(
                                value = parentUsers.find { it.id == selectedParentId }?.name ?: "Оберіть керівника",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Керівник", color = CyberText) },
                                trailingIcon = {
                                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                                },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                colors = textFieldColors()
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                parentUsers.forEach { userItem ->
                                    DropdownMenuItem(
                                        text = { Text(userItem.name) },
                                        onClick = {
                                            selectedParentId = userItem.id
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                if ((role == "cf" || role == "cl") && selectedParentId == null) {
                                    Toast.makeText(context, "Оберіть керівника для $role", Toast.LENGTH_SHORT).show()
                                    return@launch
                                }
                                authViewModel.updateUser(
                                    id = userId,
                                    name = name,
                                    login = login,
                                    password = password,
                                    role = role,
                                    rank = rank,
                                    isActive = user!!.is_active,
                                    groupNumber = if (role == "kg") groupNumber else null,
                                    parentId = selectedParentId,
                                    courseId = if (role == "nk") courseId else null

                                ) { success, message ->
                                    if (success) {
                                        Toast.makeText(context, "Користувача оновлено!", Toast.LENGTH_SHORT).show()
                                        navController.previousBackStackEntry
                                            ?.savedStateHandle
                                            ?.set("user_updated", true)
                                        navController.popBackStack()
                                    }
                                    else {
                                        Toast.makeText(context, "Помилка: $message", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = SoftAccent),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Оновити користувача", color = CyberBackground, fontFamily = FontFamily.SansSerif)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                authViewModel.deleteUser(userId) { success ->
                                    if (success) {
                                        Toast.makeText(context, "Користувача видалено!", Toast.LENGTH_SHORT).show()
                                        navController.previousBackStackEntry
                                            ?.savedStateHandle
                                            ?.set("user_updated", true)
                                        navController.popBackStack()
                                    }
                                    else {
                                        Toast.makeText(context, "Помилка видалення", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Видалити користувача", fontFamily = FontFamily.SansSerif)
                    }
                }
            }
        }
    }
}

@Composable
private fun textFieldColors() = TextFieldDefaults.colors(
    focusedTextColor = CyberText,
    unfocusedTextColor = CyberText,
    focusedContainerColor = CyberCard,
    unfocusedContainerColor = CyberCard,
    focusedIndicatorColor = SoftAccent,
    unfocusedIndicatorColor = CyberText,
    cursorColor = SoftAccent
)