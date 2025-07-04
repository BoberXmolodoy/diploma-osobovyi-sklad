package com.example.diplom.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.diplom.data.TokenManager
import com.example.diplom.data.model.CreateUserRequest
import com.example.diplom.data.model.FacultyItem
import com.example.diplom.data.model.User
import com.example.diplom.data.repository.AuthRepository
import com.example.diplom.data.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import android.util.Log

class AdminViewModel(
    private val tokenManager: TokenManager,
    private val authRepository: AuthRepository // ✅ Додано репозиторій
) : ViewModel() {

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users

    private val _faculties = MutableStateFlow<List<FacultyItem>>(emptyList()) // ✅ Додано список факультетів
    val faculties: StateFlow<List<FacultyItem>> = _faculties

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun loadUsers(role: String) {
        viewModelScope.launch {
            try {
                val token = tokenManager.getAccessToken()
                if (token == null) {
                    _error.value = "❌ Токен не знайдено, авторизуйтесь знову."
                    return@launch
                }
                _users.value = RetrofitClient.apiService.getUsersByRole(role, "Bearer $token")
                Log.d("AdminViewModel", "✅ Завантажено користувачів: ${_users.value.size}")
            } catch (e: Exception) {
                _error.value = "❌ Не вдалося завантажити користувачів: ${e.message}"
                Log.e("AdminViewModel", "Помилка завантаження користувачів", e)
            }
        }
    }

    fun loadFaculties() {
        viewModelScope.launch {
            try {
                val data = authRepository.getFaculties()
                if (data != null) {
                    _faculties.value = data
                    Log.d("AdminViewModel", "✅ Завантажено факультетів: ${data.size}")
                } else {
                    _error.value = "❌ Не вдалося отримати список факультетів"
                }
            } catch (e: Exception) {
                _error.value = "❌ Помилка завантаження факультетів: ${e.message}"
                Log.e("AdminViewModel", "Помилка отримання факультетів", e)
            }
        }
    }

    fun addUser(
        name: String,
        login: String,
        password: String,
        role: String,
        rank: String,
        groupId: String? = null,
        parentId: Int? = null,
        facultyId: Int? = null // ✅ Додано підтримку для faculty_id
    ) {
        viewModelScope.launch {
            try {
                val token = tokenManager.getAccessToken()
                if (token == null) {
                    _error.value = "❌ Токен не знайдено, авторизуйтесь знову."
                    return@launch
                }

                val request = CreateUserRequest(
                    name = name,
                    login = login,
                    password = password,
                    role = role,
                    rank = rank,
                    groupNumber = groupId,
                    parent_id = parentId,
                    faculty_id = if (role == "начальник_факультету" || role == "nf") facultyId else null // ✅ лише для НФ
                )

                val user = RetrofitClient.apiService.addUser("Bearer $token", request)
                _users.value = _users.value + user
                Log.d("AdminViewModel", "✅ Користувач доданий: $name")
            } catch (e: Exception) {
                _error.value = "❌ Не вдалося додати користувача: ${e.message}"
                Log.e("AdminViewModel", "Помилка додавання користувача", e)
            }
        }
    }

    fun deleteUser(userId: Int) {
        viewModelScope.launch {
            try {
                val token = tokenManager.getAccessToken()
                if (token == null) {
                    _error.value = "❌ Токен не знайдено, авторизуйтесь знову."
                    return@launch
                }
                RetrofitClient.apiService.deleteUser(userId, "Bearer $token")
                _users.value = _users.value.filter { it.id != userId }
                Log.d("AdminViewModel", "✅ Користувача з ID $userId видалено")
            } catch (e: Exception) {
                _error.value = "❌ Не вдалося видалити користувача: ${e.message}"
                Log.e("AdminViewModel", "Помилка видалення користувача", e)
            }
        }
    }
}
