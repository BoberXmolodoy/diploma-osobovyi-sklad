package com.example.diplom.data.model

data class FacultySummaryRequest(
    val presentOfficersCount: Int,
    val absencesOfficers: List<OfficerAbsenceRequest>
)

data class OfficerAbsenceRequest(
    val reason: String,
    val names: String,
    val count: Int
)