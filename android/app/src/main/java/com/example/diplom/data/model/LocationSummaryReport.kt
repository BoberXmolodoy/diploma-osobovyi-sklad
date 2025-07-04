package com.example.diplom.data.model

import com.google.gson.annotations.SerializedName

data class LocationSummaryReport(
    @SerializedName("id") val id: Int,
    @SerializedName("report_date") val reportDate: String,
    @SerializedName("total_count") val totalCount: Int,
    @SerializedName("present_count") val presentCount: Int,
    @SerializedName("absent_count") val absentCount: Int,
    @SerializedName("group_number") val groupNumber: String,
    @SerializedName("submitted_by_name") val submittedByName: String?,
    @SerializedName("was_updated") val wasUpdated: Boolean?,
    @SerializedName("updated_at") val updatedAt: String?
)
