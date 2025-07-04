package com.example.diplom.data.model

data class AttendanceReportDetail(
    val report_id: Int,
    val report_date: String,
    val group_number: Int?,          // Залишаємо як nullable
    val department_name: String?,   // Додаємо для кафедр
    val total_count: Int,
    val present_count: Int,
    val absences: List<AbsenceItem>
)

data class AbsenceItem(
    val full_name: String,
    val reason: String
)
