package com.example.diplom.data.model

data class TodaySummaryReport(
    val id: Int,
    val summary_date: String,
    val total_count: Int,
    val present_count: Int,
    val absent_count: Int,
    val reasons: List<Reason>,
    val course_number: Int,
    val updated_at: String? = null // 🆕 ДОДАТИ ЦЕ
)


data class Reason(
    val reason: String,
    val count: Int
)
