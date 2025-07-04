package com.example.diplom.data.model

data class DepartmentReport(
    val report_id: Int,
    val report_date: String,
    val department_name: String,
    val total_count: Int,
    val present_count: Int,
    val was_updated: Boolean? = false,
    val updated_at: String? = null
)
