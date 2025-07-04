    package com.example.diplom.data.repository

    import android.util.Log
    import com.example.diplom.data.model.AbsenceEntry
    import com.example.diplom.data.model.AttendanceReportDetail
    import com.example.diplom.data.model.AttendanceReportItem
    import com.example.diplom.data.model.AttendanceSubmitRequest
    import com.example.diplom.data.model.CourseSummaryReport
    import com.example.diplom.data.model.FacultySummaryRequest
    import com.example.diplom.data.model.LocationSummaryReport
    import com.example.diplom.data.model.MissingCourse
    import com.example.diplom.data.model.MissingGroup
    import com.example.diplom.data.model.Report
    import com.example.diplom.data.model.SubmitSummaryResponse
    import com.example.diplom.data.model.SummaryAbsence
    import com.example.diplom.data.model.TodayAttendanceReport
    import com.example.diplom.data.model.TodaySummaryReport
    import com.example.diplom.data.network.ApiService
    import retrofit2.Response

    class AttendanceRepository(private val apiService: ApiService) {


        suspend fun getReportDetails(
            reportId: Int,
            token: String
        ): Response<AttendanceReportDetail> {
            return apiService.getReportDetails(reportId, "Bearer $token")
        }


        suspend fun getGroupIdByNumber(
            groupNumber: Int,
            token: String
        ): Response<Map<String, Int>> {
            return apiService.getGroupIdByNumber(groupNumber, "Bearer $token")
        }

        suspend fun getReportsByGroup(
            groupId: Int,
            token: String
        ): Response<List<AttendanceReportItem>> {
            return apiService.getReportsByGroup(groupId, "Bearer $token")
        }

        suspend fun getMissingGroups(courseId: Int, token: String): Response<List<MissingGroup>> {
            return apiService.getGroupsWithoutReport(courseId, "Bearer $token")
        }
        suspend fun getTodayReportsByCourse(
            courseId: Int,
            token: String
        ): Response<List<TodayAttendanceReport>> {
            return apiService.getTodayReportsByCourse(courseId, "Bearer $token")
        }


        suspend fun submitCourseSummary(
            courseId: Int,
            token: String
        ): Response<SubmitSummaryResponse> {
            return apiService.submitCourseSummary(courseId, "Bearer $token")
        }

        suspend fun getCourseSummaries(
            courseId: Int,
            token: String
        ): Response<List<CourseSummaryReport>> {
            return apiService.getCourseSummaries(courseId, "Bearer $token")
        }

        suspend fun fetchSummaryAbsences(summaryId: Int, token: String): List<SummaryAbsence> {
            val result = apiService.getSummaryAbsences(summaryId, "Bearer $token")
            Log.d("REPO", "🔽 Отримано ${result.size} відсутніх для summaryId=$summaryId")
            return result
        }

        suspend fun getTodaySummariesByFaculty(
            facultyId: Int,
            token: String
        ): Response<List<TodaySummaryReport>> {
            return apiService.getTodaySummariesByFaculty(facultyId, "Bearer $token")
        }

        suspend fun getMissingCourses(
            facultyId: Int,
            token: String
        ): Response<List<MissingCourse>> {
            return apiService.getMissingCoursesByFaculty(facultyId, "Bearer $token")
        }

        suspend fun getCourseSummary(summaryId: Int, token: String): Response<CourseSummaryReport> {
            return apiService.getCourseSummary(summaryId, "Bearer $token")
        }

        suspend fun submitFacultySummary(
            facultyId: Int,
            token: String,
            request: FacultySummaryRequest
        ): Response<SubmitSummaryResponse> {
            return apiService.submitFacultySummary(facultyId, "Bearer $token", request)
        }


        suspend fun submitLocationSummary(
            token: String,
            locationId: Int
        ): Response<Unit> {
            return apiService.submitLocationSummary("Bearer $token", locationId)
        }



        suspend fun getSummariesByFaculty(
            facultyId: Int,
            token: String
        ): Response<List<CourseSummaryReport>> {
            return apiService.getSummariesByFaculty(facultyId, "Bearer $token")
        }
        suspend fun getLocationSummaryHistory(
            locationId: Int,
            token: String
        ): Response<List<LocationSummaryReport>> {
            return apiService.getLocationSummaryHistory(locationId, "Bearer $token")
        }

        suspend fun getDepartmentReports(departmentId: Int, token: String): Response<List<AttendanceReportItem>> {
            return apiService.getReportsByDepartment(departmentId, "Bearer $token")
        }

        suspend fun getMissingGroupsByLocation(locationId: Int, token: String): Response<List<MissingGroup>> {
            return apiService.getMissingGroupsByLocation(locationId, "Bearer $token")
        }



        suspend fun getMissingDepartmentsByFaculty(facultyId: Int, token: String) =
            apiService.getMissingDepartmentsByFaculty(facultyId, "Bearer $token")

        suspend fun getSubmittedReportsByLocation(locationId: Int, token: String): Response<List<Report>> {
            return apiService.getSubmittedReportsByLocation(locationId, "Bearer $token")
        }

        suspend fun exportCourseSummaryToWord(
            summaryId: Int,
            token: String
        ): Response<okhttp3.ResponseBody> {
            return apiService.exportCourseSummaryToWord(summaryId, "Bearer $token")
        }



        suspend fun submitAttendanceReportFlexible(
            groupId: Int?,
            departmentId: Int?,
            totalCount: Int,
            presentCount: Int,
            absences: List<Map<String, String>>,
            token: String
        ): Response<Map<String, String>> {
            val absenceEntries = absences.map {
                mapOf(
                    "reason" to (it["reason"] ?: ""),
                    "full_name" to (it["full_name"] ?: "")
                )
            }

            val request = mutableMapOf<String, Any>(
                "total_count" to totalCount,
                "present_count" to presentCount,
                "absences" to absenceEntries
            )

            groupId?.let { request["group_id"] = it }
            departmentId?.let { request["department_id"] = it }

            return apiService.submitFlexibleAttendanceReport(request, "Bearer $token")
        }
    }
