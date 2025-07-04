package com.example.diplom.data.model

data class AuthResponse(
    val accessToken: String,  // 🔹 Основний токен доступу
    val refreshToken: String, // 🔹 Токен оновлення
    val user: User            // 🔹 Дані користувача
)
