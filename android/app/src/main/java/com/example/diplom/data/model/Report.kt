package com.example.diplom.data.model


data class Report(
    val report_id: Int,
    val report_date: String,
    val group_number: Int,
    val total_count: Int,
    val present_count: Int,
    val submitted_by_name: String,
    val was_updated: Boolean?,   // 🆕 нове поле
    val updated_at: String?      // 🆕 нове поле
)
