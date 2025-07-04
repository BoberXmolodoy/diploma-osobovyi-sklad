package com.example.diplom.data.model

import com.google.gson.annotations.SerializedName

data class CreateUserRequest(
    val name: String,
    val login: String,
    val password: String,
    val role: String,
    val rank: String,
    val groupNumber: String? = null,

    @SerializedName("parent_id")
    val parent_id: Int? = null,

    @SerializedName("course_id")
    val course_id: Int? = null,

    @SerializedName("faculty_id")
    val faculty_id: Int? = null,

    @SerializedName("department_id")
    val department_id: Int? = null,

    @SerializedName("location_id")
    val location_id: Int? = null
)
