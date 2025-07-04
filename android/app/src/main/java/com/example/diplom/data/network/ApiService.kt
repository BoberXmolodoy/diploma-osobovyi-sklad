    package com.example.diplom.data.network

    import com.example.diplom.data.model.AttendanceReportDetail
    import com.example.diplom.data.model.AttendanceReportItem
    import com.example.diplom.data.model.AttendanceSubmitRequest
    import com.example.diplom.data.model.AuthResponse
    import com.example.diplom.data.model.CourseSummaryReport
    import com.example.diplom.data.model.CreateUserRequest
    import com.example.diplom.data.model.DepartmentItem
    import com.example.diplom.data.model.DepartmentReport
    import com.example.diplom.data.model.FacultyItem
    import com.example.diplom.data.model.FacultySummaryRequest
    import com.example.diplom.data.model.Location
    import com.example.diplom.data.model.LocationSummaryReport
    import com.example.diplom.data.model.MissingCourse
    import com.example.diplom.data.model.MissingGroup
    import com.example.diplom.data.model.Report
    import com.example.diplom.data.model.SubmitSummaryResponse
    import com.example.diplom.data.model.SummaryAbsence
    import com.example.diplom.data.model.TodayAttendanceReport
    import com.example.diplom.data.model.TodaySummaryReport
    import com.example.diplom.data.model.UpdateUserRequest
    import com.example.diplom.data.model.User
    import okhttp3.ResponseBody
    import retrofit2.Response
    import retrofit2.http.*

    interface ApiService {

        @Headers("Content-Type: application/json")
        @POST("api/auth/login")
        suspend fun login(@Body request: Map<String, String>): AuthResponse

        @Headers("Content-Type: application/json")
        @POST("api/auth/refresh")
        suspend fun refreshToken(@Body request: Map<String, String>): AuthResponse

        @GET("api/users")
        suspend fun getUsers(
            @Header("Authorization") accessToken: String
        ): List<User>

        @GET("api/users/role/{role}")
        suspend fun getUsersByRole(
            @Path("role") role: String,
            @Header("Authorization") accessToken: String
        ): List<User>

        @GET("api/users/{id}")
        suspend fun getUserById(
            @Path("id") userId: Int,
            @Header("Authorization") accessToken: String
        ): User

        @Headers("Content-Type: application/json")
        @POST("api/users/create")
        suspend fun addUser(
            @Header("Authorization") accessToken: String,
            @Body request: CreateUserRequest
        ): User

        @Headers("Content-Type: application/json")
        @PUT("api/users/{id}")
        suspend fun updateUser(
            @Path("id") userId: Int,
            @Header("Authorization") accessToken: String,
            @Body request: UpdateUserRequest
        ): User

        @DELETE("api/users/{id}")
        suspend fun deleteUser(
            @Path("id") userId: Int,
            @Header("Authorization") accessToken: String
        )

        @GET("api/users/course/{course_id}/reports")
        suspend fun getReportsByCourse(
            @Path("course_id") courseId: Int,
            @Header("Authorization") accessToken: String
        ): Response<List<Report>>

        @GET("api/attendance/report/{report_id}")
        suspend fun getReportDetails(
            @Path("report_id") reportId: Int,
            @Header("Authorization") accessToken: String
        ): Response<AttendanceReportDetail>

        @GET("api/users/groups/number/{groupNumber}")
        suspend fun getGroupIdByNumber(
            @Path("groupNumber") groupNumber: Int,
            @Header("Authorization") accessToken: String
        ): Response<Map<String, Int>>

        @GET("api/users/groups/{groupId}/number")
        suspend fun getGroupNumberById(
            @Path("groupId") groupId: Int,
            @Header("Authorization") accessToken: String
        ): Response<Map<String, Int>>

        @GET("api/attendance/group/{groupId}/reports")
        suspend fun getReportsByGroup(
            @Path("groupId") groupId: Int,
            @Header("Authorization") accessToken: String
        ): Response<List<AttendanceReportItem>>


        @Headers("Content-Type: application/json")
        @POST("api/attendance/submit")
        suspend fun submitAttendanceReport(
            @Header("Authorization") accessToken: String,
            @Body request: AttendanceSubmitRequest
        ): Response<Void>
        @GET("api/attendance/today/course/{course_id}")
        suspend fun getTodayReportsByCourse(
            @Path("course_id") courseId: Int,
            @Header("Authorization") token: String
        ): Response<List<TodayAttendanceReport>>


        @GET("api/attendance/today/course/{courseId}/missing")
        suspend fun getGroupsWithoutReport(
            @Path("courseId") courseId: Int,
            @Header("Authorization") token: String
        ): Response<List<MissingGroup>>

        @POST("api/attendance/course/{courseId}/submit-summary")
        suspend fun submitCourseSummary(
            @Path("courseId") courseId: Int,
            @Header("Authorization") token: String
        ): Response<SubmitSummaryResponse>

        @GET("api/attendance/course/{courseId}/summaries")
        suspend fun getCourseSummaries(
            @Path("courseId") courseId: Int,
            @Header("Authorization") accessToken: String
        ): Response<List<CourseSummaryReport>>

        @GET("api/attendance/summary/{summary_id}/absences")
        suspend fun getSummaryAbsences(
            @Path("summary_id") summaryId: Int,
            @Header("Authorization") token: String
        ): List<SummaryAbsence>

        @GET("api/attendance/faculty/{faculty_id}/summaries/today")
        suspend fun getTodaySummariesByFaculty(
            @Path("faculty_id") facultyId: Int,
            @Header("Authorization") token: String
        ): Response<List<TodaySummaryReport>>

        @GET("api/faculties")
        suspend fun getFaculties(
            @Header("Authorization") token: String
        ): Response<List<FacultyItem>>

        @GET("api/attendance/faculty/{faculty_id}/missing-courses")
        suspend fun getMissingCoursesByFaculty(
            @Path("faculty_id") facultyId: Int,
            @Header("Authorization") token: String
        ): Response<List<MissingCourse>>

        @GET("api/attendance/summary/{summary_id}")
        suspend fun getCourseSummary(
            @Path("summary_id") summaryId: Int,
            @Header("Authorization") token: String
        ): Response<CourseSummaryReport>

        @POST("api/attendance/faculty/{faculty_id}/submit-summary")
        suspend fun submitFacultySummary(
            @Path("faculty_id") facultyId: Int,
            @Header("Authorization") token: String,
            @Body request: FacultySummaryRequest
        ): Response<SubmitSummaryResponse>


        @GET("api/attendance/faculty/{faculty_id}/summaries")
        suspend fun getSummariesByFaculty(
            @Path("faculty_id") facultyId: Int,
            @Header("Authorization") token: String
        ): Response<List<CourseSummaryReport>>

        @GET("api/departments")
        suspend fun getDepartments(
            @Header("Authorization") token: String
        ): Response<List<DepartmentItem>>

        @GET("api/attendance/faculty/{faculty_id}/missing-departments")
        suspend fun getMissingDepartmentsByFaculty(
            @Path("faculty_id") facultyId: Int,
            @Header("Authorization") token: String
        ): Response<List<DepartmentItem>>

        @POST("api/attendance/submit")
        suspend fun submitFlexibleAttendanceReport(
            @Body request: Map<String, @JvmSuppressWildcards Any>,
            @Header("Authorization") token: String
        ): Response<Map<String, String>>

        @GET("api/attendance/department/{departmentId}/reports")
        suspend fun getReportsByDepartment(
            @Path("departmentId") departmentId: Int,
            @Header("Authorization") token: String
        ): Response<List<AttendanceReportItem>>

        @GET("api/attendance/faculty/{facultyId}/departments/reports/today")
        suspend fun getTodayDepartmentReports(
            @Path("facultyId") facultyId: Int,
            @Header("Authorization") token: String
        ): Response<List<DepartmentReport>>


        @GET("api/locations")
        suspend fun getLocations(
            @Header("Authorization") token: String
        ): Response<List<Location>>

        @GET("api/attendance/location/{locationId}/missing-groups")
        suspend fun getMissingGroupsByLocation(
            @Path("locationId") locationId: Int,
            @Header("Authorization") token: String
        ): Response<List<MissingGroup>>


        @GET("api/attendance/location/{locationId}/reports")
        suspend fun getSubmittedReportsByLocation(
            @Path("locationId") locationId: Int,
            @Header("Authorization") token: String
        ): Response<List<Report>>


        @POST("api/attendance/location/{locationId}/submit-summary")
        suspend fun submitLocationSummary(
            @Header("Authorization") token: String,
            @Path("locationId") locationId: Int
        ): Response<Unit>

        @GET("api/attendance/location/{locationId}/summaries")
        suspend fun getLocationSummaryHistory(
            @Path("locationId") locationId: Int,
            @Header("Authorization") token: String
        ): Response<List<LocationSummaryReport>>

        @GET("api/attendance/summary/{summary_id}/export-word")
        @Streaming
        suspend fun exportCourseSummaryToWord(
            @Path("summary_id") summaryId: Int,
            @Header("Authorization") token: String
        ): Response<ResponseBody>
    }
