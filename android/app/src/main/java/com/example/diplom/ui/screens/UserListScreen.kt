package com.example.diplom.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.diplom.data.TokenManager
import com.example.diplom.data.model.User
import com.example.diplom.data.network.RetrofitClient
import com.example.diplom.data.repository.AuthRepository
import com.example.diplom.ui.theme.*
import com.example.diplom.viewmodel.AuthViewModel
import com.example.diplom.viewmodel.AuthViewModelFactory
import com.example.myapplication.R
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Composable
fun UserListScreen(navController: NavController, role: String, tokenManager: TokenManager) {
    val repository = AuthRepository(RetrofitClient.apiService, tokenManager)
    val coroutineScope = rememberCoroutineScope()

    var users by remember { mutableStateOf<List<User>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    val listState = rememberLazyListState()
    var scrollIndex by remember { mutableStateOf(0) }

    val roleMapping = mapOf(
        "начальник_факультету" to "nf",
        "начальник_курсу" to "nk",
        "командир_групи" to "kg",
        "черговий_локації" to "cl",
        "начальник_кафедри" to "nkf"
    )


    val displayNameMap = mapOf(
        "nf" to "Начальник факультету",
        "nk" to "Начальник курсу",
        "kg" to "Командир групи",
        "cl" to "Черговий локації",
        "nkf" to "Начальник кафедри"
    )

    val backendRole = roleMapping[role] ?: role
    val displayRole = displayNameMap[backendRole] ?: role
    val encodedRole = URLEncoder.encode(backendRole, StandardCharsets.UTF_8.toString())

    fun loadUsers(resetScroll: Boolean = false) {
        coroutineScope.launch {
            loading = true
            try {
                val response = repository.getUsersByRole(encodedRole)
                users = response ?: emptyList()

                if (resetScroll) {
                    listState.scrollToItem(0) // ⬅️ Сюди повертаємось після оновлення
                } else {
                    listState.scrollToItem(scrollIndex)
                }

            } catch (e: Exception) {
                error = "❌ Помилка завантаження користувачів: ${e.message}"
            } finally {
                loading = false
            }
        }
    }



    LaunchedEffect(Unit) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collectLatest {
                scrollIndex = it
            }
    }

    val currentBackStackEntry = navController.currentBackStackEntryAsState().value
    val savedStateHandle = currentBackStackEntry?.savedStateHandle

    LaunchedEffect(savedStateHandle?.get<Boolean>("user_added") ?: false,
        savedStateHandle?.get<Boolean>("user_updated") ?: false) {

        val userAdded = savedStateHandle?.get<Boolean>("user_added") ?: false
        val userUpdated = savedStateHandle?.get<Boolean>("user_updated") ?: false

        if (userAdded || userUpdated || users.isEmpty()) {
            loadUsers(resetScroll = true) // ⬅️ Примусове повернення на початок списку
            savedStateHandle?.set("user_added", false)
            savedStateHandle?.set("user_updated", false)
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    navController.navigate("add_user/$backendRole")
                },
                containerColor = SoftAccent,
                modifier = Modifier
                    .padding(end = 16.dp, bottom = 16.dp)
                    .navigationBarsPadding()
            ) {
                Icon(Icons.Default.Add, contentDescription = "Додати користувача")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CyberBackground)
                .padding(paddingValues)
        ) {
            Image(
                painter = painterResource(id = R.drawable.institute_logo),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.08f),
                contentScale = ContentScale.Crop
            )

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .statusBarsPadding()
                    .padding(top = 8.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Користувачі: $displayRole",
                        color = SoftAccent,
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                when {
                    loading -> item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    error != null -> item {
                        Text(error!!, color = Color.Red)
                    }

                    users.isEmpty() -> item {
                        Text("🔹 Немає користувачів у цій категорії")
                    }

                    else -> items(users) { user ->
                        UserItem(user, navController) {
                            loadUsers()
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(200.dp))
                }
            }
        }
    }
}

@Composable
fun UserItem(
    user: User,
    navController: NavController,
    onUserDeleted: () -> Unit
) {
    val context = LocalContext.current
    val tokenManager = TokenManager(context)
    val viewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(AuthRepository(RetrofitClient.apiService, tokenManager), tokenManager)
    )
    val coroutineScope = rememberCoroutineScope()
    var showDialog by remember { mutableStateOf(false) }

    val displayNameMap = mapOf(
        "nf" to "Начальник факультету",
        "nk" to "Начальник курсу",
        "kg" to "Командир групи",
        "cl" to "Черговий локації",
        "nkf" to "Начальник кафедри"
    )

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Підтвердження") },
            text = { Text("Ви дійсно хочете видалити користувача \"${user.name}\"?") },
            confirmButton = {
                TextButton(onClick = {
                    showDialog = false
                    coroutineScope.launch {
                        viewModel.deleteUser(user.id) { success ->
                            if (success) {
                                Toast.makeText(context, "Користувача видалено", Toast.LENGTH_SHORT).show()
                                onUserDeleted()
                            } else {
                                Toast.makeText(context, "Помилка при видаленні", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }) {
                    Text("Так")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Скасувати")
                }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(6.dp),
        colors = CardDefaults.cardColors(containerColor = CyberCard)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = user.name,
                fontFamily = FontFamily.SansSerif,
                fontSize = 18.sp,
                color = Color.White
            )
            Text(
                text = "🔹 Звання: ${user.rank}",
                fontFamily = FontFamily.SansSerif,
                fontSize = 14.sp,
                color = SoftAccent
            )
            Text(
                text = "🛡 Роль: ${displayNameMap[user.role] ?: user.role}",
                fontFamily = FontFamily.SansSerif,
                fontSize = 14.sp,
                color = CyberText
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = {
                    navController.navigate("user_detail/${user.id}")
                }) {
                    Text("Редагувати", color = SoftAccent)
                }

                Spacer(modifier = Modifier.width(8.dp))

                TextButton(onClick = {
                    showDialog = true
                }) {
                    Text("Видалити", color = Color.Red)
                }
            }
        }
    }
}
