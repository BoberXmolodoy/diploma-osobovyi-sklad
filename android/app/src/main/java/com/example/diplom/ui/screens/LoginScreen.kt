package com.example.diplom.ui.screens

import android.util.Base64
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.diplom.data.TokenManager
import com.example.diplom.data.network.RetrofitClient
import com.example.diplom.data.repository.AuthRepository
import com.example.diplom.ui.theme.*
import com.example.diplom.viewmodel.AuthViewModel
import com.example.diplom.viewmodel.AuthViewModelFactory
import com.example.myapplication.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.HttpException

@Composable
fun LoginScreen(
    navController: NavController,
    authRepository: AuthRepository,
    tokenManager: TokenManager
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val apiService = RetrofitClient.apiService

    val authViewModel: AuthViewModel = viewModel(factory = remember {
        AuthViewModelFactory(AuthRepository(apiService, tokenManager), tokenManager)
    })

    var login by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(500)
        tokenManager.getAccessToken()?.let { token ->
            val role = tokenManager.getUserRole()
            if (role == "admin") {
                navController.navigate("admin") {
                    popUpTo("login") { inclusive = true }
                }
            } else {
                navController.navigate("main") {
                    popUpTo("login") { inclusive = true }
                }
            }
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

        Card(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
                .align(Alignment.Center)
                .shadow(10.dp, shape = RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CyberCard)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Вхід до системи",
                    color = SoftAccent,
                    fontFamily = FontFamily.SansSerif,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                message?.let {
                    Text(
                        text = it,
                        color = Color(0xFFFF6B6B), // Яскраво-червоний
                        fontSize = 13.sp,
                        fontFamily = FontFamily.SansSerif,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 6.dp, bottom = 8.dp)
                    )
                }

                OutlinedTextField(
                    value = login,
                    onValueChange = { login = it },
                    label = { Text("Логін", color = CyberText) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(1.dp, RoundedCornerShape(10.dp)),
                    shape = RoundedCornerShape(10.dp),
                    textStyle = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 14.sp
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = CyberText,
                        unfocusedTextColor = CyberText,
                        focusedContainerColor = CyberCard,
                        unfocusedContainerColor = CyberCard,
                        focusedIndicatorColor = SoftAccent,
                        unfocusedIndicatorColor = CyberText,
                        cursorColor = SoftAccent
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Пароль", color = CyberText) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(1.dp, RoundedCornerShape(10.dp)),
                    shape = RoundedCornerShape(10.dp),
                    textStyle = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 14.sp
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = CyberText,
                        unfocusedTextColor = CyberText,
                        focusedContainerColor = CyberCard,
                        unfocusedContainerColor = CyberCard,
                        focusedIndicatorColor = SoftAccent,
                        unfocusedIndicatorColor = CyberText,
                        cursorColor = SoftAccent
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        coroutineScope.launch(Dispatchers.Main) {
                            isLoading = true
                            message = null
                            try {
                                val response = authRepository.login(login, password)
                                tokenManager.saveTokens(response.accessToken, response.refreshToken)
                                RetrofitClient.setTokenManager(tokenManager) // ✅ ОБОВ'ЯЗКОВО

                                val groupId = response.user.groupId
                                val groupNumber = if (groupId != null) {
                                    try {
                                        authRepository.getGroupNumberByGroupId(groupId)
                                    } catch (e: Exception) {
                                        Log.e("LoginScreen", "❌ Не вдалося отримати номер групи", e)
                                        null
                                    }
                                } else null
                                tokenManager.saveGroupInfo(groupId, groupNumber)


                                tokenManager.saveGroupInfo(groupId, groupNumber)


                                Log.d("LoginScreen", "✅ Збережено groupId=$groupId, groupNumber=$groupNumber")

                                val role = tokenManager.getUserRole()

                                when (role) {
                                    "admin" -> navController.navigate("admin") {
                                        popUpTo(0) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                    "nf" -> navController.navigate("faculty") {
                                        popUpTo(0) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                    "nk" -> navController.navigate("nk") {
                                        popUpTo(0) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                    "kg" -> navController.navigate("submit_attendance") {
                                        popUpTo(0) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                    "nkf" -> navController.navigate("submit_attendance") {
                                        popUpTo(0) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                    "cl" -> navController.navigate("location_commander") {
                                        popUpTo(0) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                    else -> navController.navigate("main") {
                                        popUpTo(0) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                }



                            } catch (e: HttpException) {
                                message = when (e.code()) {
                                    401 -> "❌ Невірний логін або пароль"
                                    else -> "❌ Помилка сервера: ${e.message()}"
                                }
                            } catch (e: Exception) {
                                message = "❌ Помилка з'єднання"
                            } finally {
                                isLoading = false
                            }
                        }
                    },

                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(3.dp, RoundedCornerShape(12.dp)),
                    enabled = !isLoading,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SoftAccent)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = CyberCard, modifier = Modifier.size(24.dp))
                    } else {
                        Text(
                            "Увійти",
                            color = CyberBackground,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

fun getUserRole(token: String): String? {
    return try {
        val payload = token.split(".")[1]
        val decoded = String(Base64.decode(payload, Base64.DEFAULT))
        val jsonObject = JSONObject(decoded)
        jsonObject.getString("role")
    } catch (e: Exception) {
        Log.e("LoginScreen", "❌ Не вдалося розшифрувати токен", e)
        null
    }
}
