package com.example.diplom.data.model

import com.google.gson.annotations.SerializedName

data class User(
    val id: Int,
    val name: String,
    val login: String,
    val role: String,
    val rank: String,
    @SerializedName("is_active") val is_active: Boolean,
    @SerializedName("group_id") val groupId: Int? = null,
    @SerializedName("course_id") val course_id: Int? = null,
    val path: String? = null,
    @SerializedName("parent_id") val parent_id: Int? = null
)
