package com.example.diplom.data.model

import com.google.gson.annotations.SerializedName

data class TodayAttendanceReport(
    @SerializedName("report_id") val reportId: Int,
    @SerializedName("report_date") val reportDate: String,
    @SerializedName("group_number") val groupNumber: Int,
    @SerializedName("total_count") val totalCount: Int,
    @SerializedName("present_count") val presentCount: Int,
    @SerializedName("was_updated") val wasUpdated: Boolean,
    @SerializedName("updated_at") val updatedAt: String?

)
