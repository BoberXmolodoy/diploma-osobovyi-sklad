package com.example.diplom.data.model

import com.google.gson.annotations.SerializedName

data class UpdateUserRequest(
    val name: String,
    val login: String,
    val password: String? = null,
    val role: String,
    val rank: String,
    @SerializedName("is_active") val isActive: Boolean,
    val groupNumber: String? = null,
    val parent_id: Int? = null,
    val course_id: Int? = null // ✅ додано
)
