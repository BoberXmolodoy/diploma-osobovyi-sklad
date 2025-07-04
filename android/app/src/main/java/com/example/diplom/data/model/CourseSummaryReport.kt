package com.example.diplom.data.model

data class CourseSummaryReport(
    val id: Int,
    val summary_date: String,
    val total_count: Int,
    val present_count: Int,
    val absent_count: Int,
    val reasons: List<ReasonItem>,
    val course_id: Int?,     // ✅ додай це
    val faculty_id: Int?,     // ✅ і це, бо ти працюєш з даними по факультету
    val was_updated: Boolean?,         // ⬅️ нове поле
    val updated_at: String?
)

data class ReasonItem(
    val reason: String,
    val count: Int
)
