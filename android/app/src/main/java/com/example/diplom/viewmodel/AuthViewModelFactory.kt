package com.example.diplom.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.diplom.data.repository.AuthRepository
import com.example.diplom.data.TokenManager

class AuthViewModelFactory(
    private val repository: AuthRepository,
    private val tokenManager: TokenManager // ✅ Додаємо TokenManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            return AuthViewModel(repository, tokenManager) as T // ✅ Передаємо обидва параметри
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
