package com.example.diplom.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.diplom.data.TokenManager
import com.example.diplom.data.repository.AuthRepository

class AdminViewModelFactory(
    private val tokenManager: TokenManager,
    private val authRepository: AuthRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AdminViewModel::class.java)) {
            return AdminViewModel(tokenManager, authRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
