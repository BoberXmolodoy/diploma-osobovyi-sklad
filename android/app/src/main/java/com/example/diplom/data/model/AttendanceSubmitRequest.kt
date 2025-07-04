package com.example.diplom.data.model

import com.google.gson.annotations.SerializedName

data class AttendanceSubmitRequest(
    @SerializedName("group_id")
    val groupId: Int,

    @SerializedName("total_count")
    val totalCount: Int,

    @SerializedName("present_count")
    val presentCount: Int,

    val absences: List<AbsenceEntry>
)

data class AbsenceEntry(
    val reason: String,

    @SerializedName("full_name")
    val fullName: String
)
