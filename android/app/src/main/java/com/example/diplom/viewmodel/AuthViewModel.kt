    package com.example.diplom.viewmodel

    import android.util.Log
    import androidx.lifecycle.ViewModel
    import androidx.lifecycle.viewModelScope
    import com.example.diplom.data.TokenManager
    import com.example.diplom.data.model.AuthResponse
    import com.example.diplom.data.model.DepartmentItem
    import com.example.diplom.data.model.FacultyItem
    import com.example.diplom.data.model.Location
    import com.example.diplom.data.model.MissingGroup
    import com.example.diplom.data.model.User
    import com.example.diplom.data.repository.AuthRepository
    import kotlinx.coroutines.launch

    class AuthViewModel(
        private val authRepository: AuthRepository,
        private val tokenManager: TokenManager
    ) : ViewModel() {

        fun login(login: String, password: String, onResult: (AuthResponse?) -> Unit) {
            viewModelScope.launch {
                try {
                    val response = authRepository.login(login, password)
                    onResult(response)
                } catch (e: Exception) {
                    onResult(null)
                }
            }
        }

        fun getToken(): String? {
            return tokenManager.getAccessToken()
        }

        suspend fun getUsersByRole(role: String): List<User>? {
            return try {
                authRepository.getUsersByRole(role)
            } catch (e: Exception) {
                null
            }
        }
        suspend fun getFaculties(): List<FacultyItem>? {
            return try {
                authRepository.getFaculties()
            } catch (e: Exception) {
                Log.e("AuthViewModel", "❌ Помилка при отриманні факультетів: ${e.message}")
                null
            }
        }

        suspend fun getDepartments(): List<DepartmentItem>? {
            return try {
                authRepository.getDepartments()
            } catch (e: Exception) {
                null
            }
        }
        suspend fun getLocations(): List<Location>? {
            return try {
                authRepository.getLocations()
            } catch (e: Exception) {
                null
            }
        }


        suspend fun getMissingDepartments(facultyId: Int): List<DepartmentItem>? {
            return try {
                authRepository.getMissingDepartments(facultyId)
            } catch (e: Exception) {
                Log.e("AuthViewModel", "❌ Помилка при отриманні кафедр: ${e.message}")
                null
            }
        }

        fun updateUser(
            id: Int,
            name: String,
            login: String,
            password: String,
            role: String,
            rank: String,
            isActive: Boolean,
            groupNumber: String? = null,
            parentId: Int? = null,
            courseId: String? = null, // ✅ додано
            onResult: (Boolean, String?) -> Unit
        ) {
            viewModelScope.launch {
                try {
                    val success = authRepository.updateUser(
                        id = id,
                        name = name,
                        login = login,
                        password = password,
                        role = role,
                        rank = rank,
                        isActive = isActive,
                        groupNumber = groupNumber,
                        parentId = parentId,
                        courseId = courseId // ✅ передано
                    )
                    if (success) {
                        onResult(true, null)
                    } else {
                        onResult(false, "Не вдалося оновити користувача.")
                    }
                } catch (e: Exception) {
                    Log.e("AuthViewModel", "❌ Помилка оновлення: ${e.message}")
                    onResult(false, e.message)
                }
            }
        }

        fun getUserById(userId: Int, onResult: (User?) -> Unit) {
            viewModelScope.launch {
                try {
                    val user = authRepository.getUserById(userId)
                    onResult(user)
                } catch (e: Exception) {
                    onResult(null)
                }
            }
        }

        fun deleteUser(userId: Int, onResult: (Boolean) -> Unit) {
            viewModelScope.launch {
                try {
                    val success = authRepository.deleteUser(userId)
                    onResult(success)
                } catch (e: Exception) {
                    onResult(false)
                }
            }
        }

        fun addUser(
            name: String,
            login: String,
            password: String,
            role: String,
            rank: String,
            groupNumber: String? = null,
            parentId: Int? = null,
            courseId: String? = null,
            facultyId: Int? = null,
            departmentId: Int? = null, // 🆕
            locationId: Int? = null,   // 🆕
            onResult: (Boolean, String?, List<User>?) -> Unit
        ) {
            viewModelScope.launch {
                authRepository.addUser(
                    name = name,
                    login = login,
                    password = password,
                    role = role,
                    rank = rank,
                    groupNumber = groupNumber,
                    parentId = parentId,
                    courseId = courseId,
                    facultyId = facultyId,
                    departmentId = departmentId,
                    locationId = locationId
                ) { success, errorMessage, updatedUsers ->
                    if (success) {
                        Log.d("AuthViewModel", "✅ Успішно додано користувача")
                        onResult(true, null, updatedUsers)
                    } else {
                        Log.e("AuthViewModel", "❌ Помилка при додаванні: $errorMessage")
                        onResult(false, errorMessage, null)
                    }
                }
            }
        }

        suspend fun getMissingGroupsByLocation(locationId: Int): List<MissingGroup>? {
            return try {
                authRepository.getMissingGroupsByLocation(locationId)
            } catch (e: Exception) {
                Log.e("AuthViewModel", "❌ Помилка при отриманні груп без розходу: ${e.message}")
                null
            }
        }




        // ✅ ДОДАНО: Підтягування groupNumber по groupId
        suspend fun getGroupNumberByGroupId(groupId: Int): String? {
            return try {
                authRepository.getGroupNumberByGroupId(groupId)
            } catch (e: Exception) {
                Log.e("AuthViewModel", "❌ Помилка при отриманні номера групи: ${e.message}")
                null
            }
        }
    }
