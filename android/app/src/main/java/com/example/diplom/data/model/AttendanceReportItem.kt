package com.example.diplom.data.model

import com.google.gson.annotations.SerializedName

data class AttendanceReportItem(
    @SerializedName("id") val reportId: Int,
    @SerializedName("report_date") val reportDate: String,
    @SerializedName("group_number") val groupNumber: Int,
    @SerializedName("total_count") val totalCount: Int,
    @SerializedName("present_count") val presentCount: Int,
    @SerializedName("department_id") val departmentId: Int? = null,
    @SerializedName("submitted_by") val submittedBy: Int? = null,

    // 🆕 Нові поля
    @SerializedName("was_updated") val wasUpdated: Boolean? = false,
    @SerializedName("updated_at") val updatedAt: String? = null
)
