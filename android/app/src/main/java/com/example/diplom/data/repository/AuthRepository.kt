package com.example.diplom.data.repository

import android.util.Log
import com.example.diplom.data.TokenManager
import com.example.diplom.data.model.*
import com.example.diplom.data.network.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class AuthRepository(
    private val apiService: ApiService,
    private val tokenManager: TokenManager
) {
    suspend fun login(login: String, password: String): AuthResponse {
        val response = apiService.login(mapOf("login" to login, "password" to password))
        Log.d("AuthRepository", "📩 Отримані токени: accessToken=${response.accessToken}, refreshToken=${response.refreshToken}")

        tokenManager.saveTokens(response.accessToken, response.refreshToken)

        // ⬇️ Отримуємо groupId із відповіді
        val groupId = response.user.groupId

        // ⬇️ Підтягуємо номер групи по ID (якщо є)
        val groupNumber = if (groupId != null) {
            try {
                getGroupNumberByGroupId(groupId)
            } catch (e: Exception) {
                Log.e("AuthRepository", "❌ Помилка отримання groupNumber: ${e.message}")
                null
            }
        } else null

        // ⬇️ Зберігаємо groupId і groupNumber у TokenManager
        tokenManager.saveGroupInfo(groupId, groupNumber)

        return response
    }



    suspend fun refreshToken(): Boolean {
        val refreshToken = tokenManager.getRefreshToken() ?: return false
        return try {
            val response = apiService.refreshToken(mapOf("refreshToken" to refreshToken))
            val newAccessToken = response.accessToken
            val newRefreshToken = response.refreshToken ?: refreshToken
            tokenManager.saveTokens(newAccessToken, newRefreshToken)
            Log.d("AuthRepository", "✅ Токен оновлено")
            true
        } catch (e: Exception) {
            Log.e("AuthRepository", "❌ Помилка оновлення токена: ${e.message}")
            withContext(Dispatchers.IO) { tokenManager.clearTokens() }
            false
        }
    }


    suspend fun getUsersByRole(role: String): List<User>? {
        var token = tokenManager.getAccessToken() ?: return null
        var authHeader = "Bearer $token"

        Log.d("AuthRepository", "📩 Отримуємо користувачів для ролі: $role")

        return try {
            val users = apiService.getUsersByRole(role, authHeader)
            Log.d("AuthRepository", "✅ Отримано користувачів: ${users.size} для ролі: $role")
            users
        } catch (e: HttpException) {
            if (e.code() == 401 && refreshToken()) {
                token = tokenManager.getAccessToken() ?: return null
                authHeader = "Bearer $token"
                return apiService.getUsersByRole(role, authHeader)
            }
            Log.e("AuthRepository", "❌ Помилка отримання користувачів: ${e.message}")
            null
        }
    }

    suspend fun updateUser(
        id: Int,
        name: String,
        login: String,
        password: String,
        role: String,
        rank: String,
        isActive: Boolean,
        groupNumber: String? = null,
        parentId: Int? = null,
        courseId: String? = null // ✅ ДОДАНО
    ): Boolean {
        var token = tokenManager.getAccessToken() ?: return false
        var authHeader = "Bearer $token"

        val updateRequest = UpdateUserRequest(
            name = name,
            login = login,
            password = if (password.isNotEmpty()) password else null,
            role = role,
            rank = rank,
            isActive = isActive,
            groupNumber = if (role == "командир_групи" || role == "kg") groupNumber else null,
            parent_id = parentId,
            course_id = if (role == "начальник_курсу" || role == "nk") courseId?.toIntOrNull() else null // ✅ передаємо лише якщо це nk
        )

        return try {
            apiService.updateUser(id, authHeader, updateRequest)
            Log.d("AuthRepository", "✅ Користувача оновлено успішно")
            true
        } catch (e: HttpException) {
            if (e.code() == 401 && refreshToken()) {
                token = tokenManager.getAccessToken() ?: return false
                authHeader = "Bearer $token"
                apiService.updateUser(id, authHeader, updateRequest)
                return true
            }
            Log.e("AuthRepository", "❌ Помилка оновлення користувача: ${e.message}")
            false
        }
    }



    @JvmOverloads
    suspend fun addUser(
        name: String,
        login: String,
        password: String,
        role: String,
        rank: String,
        groupNumber: String? = null,
        parentId: Int? = null,
        courseId: String? = null,
        facultyId: Int? = null,
        departmentId: Int? = null, // ✅ додано
        locationId: Int? = null,   // ✅ додано
        callback: (Boolean, String?, List<User>?) -> Unit
    ) {
        var token = tokenManager.getAccessToken() ?: return callback(false, "Токен недійсний", null)
        var authHeader = "Bearer $token"

        val request = CreateUserRequest(
            name = name,
            login = login,
            password = password,
            role = role,
            rank = rank,
            groupNumber = if (role == "командир_групи" || role == "kg") groupNumber else null,
            parent_id = parentId,
            course_id = courseId?.toIntOrNull(),
            faculty_id = facultyId,
            department_id = departmentId, // 🆕
            location_id = locationId      // 🆕
        )

        Log.d("AuthRepository", "📩 Відправка CreateUserRequest: $request")

        try {
            val response = apiService.addUser(authHeader, request)
            Log.d("AuthRepository", "✅ Користувач створений: $response")

            val updatedUsers = getUsersByRole(role)
            callback(true, null, updatedUsers)

        } catch (e: HttpException) {
            if (e.code() == 401 && refreshToken()) {
                token = tokenManager.getAccessToken() ?: return callback(false, "Не вдалося оновити токен", null)
                authHeader = "Bearer $token"

                return try {
                    val retryUser = apiService.addUser(authHeader, request)
                    Log.d("AuthRepository", "✅ Користувач створений (повторно): $retryUser")
                    val updatedUsers = getUsersByRole(role)
                    callback(true, null, updatedUsers)
                } catch (retryError: HttpException) {
                    Log.e("AuthRepository", "❌ Повторна помилка: ${retryError.message}")
                    callback(false, retryError.message, null)
                }
            }

            Log.e("AuthRepository", "❌ Помилка створення користувача: ${e.message}")
            callback(false, e.message, null)
        }
    }

    suspend fun getUserById(userId: Int): User? {
        var token = tokenManager.getAccessToken() ?: return null
        var authHeader = "Bearer $token"

        return try {
            apiService.getUserById(userId, authHeader)
        } catch (e: HttpException) {
            if (e.code() == 401 && refreshToken()) {
                token = tokenManager.getAccessToken() ?: return null
                authHeader = "Bearer $token"
                return apiService.getUserById(userId, authHeader)
            }
            Log.e("AuthRepository", "❌ Помилка при отриманні користувача: ${e.message}")
            null
        }
    }

    suspend fun getGroupNumberByGroupId(groupId: Int): String? {
        val token = tokenManager.getAccessToken() ?: return null
        val authHeader = "Bearer $token"

        return try {
            val response = apiService.getGroupNumberById(groupId, authHeader)
            val number = response.body()?.get("number")?.toString()
            Log.d("AuthRepository", "✅ Отримано groupNumber: $number")
            number
        } catch (e: Exception) {
            Log.e("AuthRepository", "❌ Помилка при отриманні номера групи: ${e.message}")
            null
        }
    }

    suspend fun getFaculties(): List<FacultyItem>? {
        var token = tokenManager.getAccessToken() ?: return null
        var authHeader = "Bearer $token"

        return try {
            val response = apiService.getFaculties(authHeader)
            if (response.isSuccessful) {
                Log.d("AuthRepository", "✅ Отримано факультети: ${response.body()?.size}")
                response.body()
            } else {
                Log.e("AuthRepository", "❌ Помилка отримання факультетів: ${response.code()}")
                null
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "❌ Виняток при отриманні факультетів: ${e.message}")
            null
        }
    }

    suspend fun deleteUser(userId: Int): Boolean {
        var token = tokenManager.getAccessToken() ?: return false
        var authHeader = "Bearer $token"

        return try {
            apiService.deleteUser(userId, authHeader)
            Log.d("AuthRepository", "✅ Користувача з ID $userId видалено")
            true
        } catch (e: HttpException) {
            if (e.code() == 401 && refreshToken()) {
                token = tokenManager.getAccessToken() ?: return false
                authHeader = "Bearer $token"
                apiService.deleteUser(userId, authHeader)
                return true
            }
            Log.e("AuthRepository", "❌ Помилка при видаленні користувача: ${e.message}")
            false
        }
    }

    suspend fun getDepartments(): List<DepartmentItem>? {
        val token = tokenManager.getAccessToken() ?: return null
        val authHeader = "Bearer $token"

        return try {
            val response = apiService.getDepartments(authHeader)
            if (response.isSuccessful) {
                Log.d("AuthRepository", "✅ Отримано кафедри: ${response.body()?.size}")
                response.body()
            } else {
                Log.e("AuthRepository", "❌ Помилка отримання кафедр: ${response.code()}")
                null
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "❌ Виняток при отриманні кафедр: ${e.message}")
            null
        }
    }

    suspend fun getMissingDepartments(facultyId: Int): List<DepartmentItem>? {
        var token = tokenManager.getAccessToken() ?: return null
        var authHeader = "Bearer $token"

        return try {
            val response = apiService.getMissingDepartmentsByFaculty(facultyId, authHeader)
            if (response.isSuccessful) {
                Log.d("AuthRepository", "✅ Кафедри без розходу: ${response.body()?.size}")
                response.body()
            } else {
                Log.e("AuthRepository", "❌ Код помилки при отриманні кафедр: ${response.code()}")
                null
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "❌ Виняток при отриманні кафедр: ${e.message}")
            null
        }
    }

    suspend fun getMissingGroupsByLocation(locationId: Int): List<MissingGroup>? {
        var token = tokenManager.getAccessToken() ?: return null
        var authHeader = "Bearer $token"

        return try {
            val response = apiService.getMissingGroupsByLocation(locationId, authHeader)
            if (response.isSuccessful) {
                Log.d("AuthRepository", "✅ Групи без розходу по локації: ${response.body()?.size}")
                response.body()
            } else {
                Log.e("AuthRepository", "❌ Помилка при отриманні груп по локації: ${response.code()}")
                null
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "❌ Виняток при отриманні груп по локації: ${e.message}")
            null
        }
    }


    suspend fun getLocations(): List<Location>? {
        var token = tokenManager.getAccessToken() ?: return null
        var authHeader = "Bearer $token"

        return try {
            val response = apiService.getLocations(authHeader)
            if (response.isSuccessful) {
                Log.d("AuthRepository", "✅ Отримано локації: ${response.body()?.size}")
                response.body()
            } else {
                Log.e("AuthRepository", "❌ Помилка отримання локацій: ${response.code()}")
                null
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "❌ Виняток при отриманні локацій: ${e.message}")
            null
        }
    }


}

