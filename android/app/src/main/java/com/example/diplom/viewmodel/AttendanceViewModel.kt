package com.example.diplom.viewmodel

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import java.time.LocalDate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.diplom.data.model.AttendanceReportDetail
import com.example.diplom.data.model.AttendanceReportItem
import com.example.diplom.data.model.CourseSummaryReport
import com.example.diplom.data.model.DepartmentItem
import com.example.diplom.data.model.DepartmentReport
import com.example.diplom.data.model.FacultySummaryRequest
import com.example.diplom.data.model.LocationSummaryReport
import com.example.diplom.data.model.MissingCourse
import com.example.diplom.data.model.MissingGroup
import com.example.diplom.data.model.Report
import com.example.diplom.data.model.SummaryAbsence
import com.example.diplom.data.repository.AttendanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.example.diplom.data.model.TodayAttendanceReport
import com.example.diplom.data.model.TodaySummaryReport
import com.example.diplom.data.network.RetrofitClient.apiService
import okhttp3.ResponseBody



class AttendanceViewModel(private val repository: AttendanceRepository) : ViewModel() {

    // ⬆️ Додай у верхній частині класу (після інших StateFlow)
    private val _submissionSuccess = MutableStateFlow(false)
    val submissionSuccess: StateFlow<Boolean> = _submissionSuccess


    // 🔹 Звіти по курсу (для nk)
    private val _reports = MutableStateFlow<List<Report>>(emptyList())
    val reports: StateFlow<List<Report>> = _reports

    private val _selectedReport = MutableStateFlow<AttendanceReportDetail?>(null)
    val selectedReport: StateFlow<AttendanceReportDetail?> = _selectedReport

    private val _locationSummaries = MutableStateFlow<List<LocationSummaryReport>>(emptyList())
    val locationSummaries: StateFlow<List<LocationSummaryReport>> = _locationSummaries

    // 🔹 Звіти по групі (для kg — історія)
    private val _groupReports = MutableStateFlow<List<AttendanceReportItem>>(emptyList())
    val groupReports: StateFlow<List<AttendanceReportItem>> = _groupReports

    // 🔹 Отримати звіти по курсу

    // 🔹 Отримати звіти по групі
    fun fetchReportsByGroup(groupId: Int, token: String) {
        viewModelScope.launch {
            val response = repository.getReportsByGroup(groupId, token)
            if (response.isSuccessful) {
                _groupReports.value = response.body() ?: emptyList()
            }
        }
    }

    private val _departmentReports = MutableStateFlow<List<AttendanceReportItem>>(emptyList())
    val departmentReports: StateFlow<List<AttendanceReportItem>> = _departmentReports

    fun fetchReportsByDepartment(departmentId: Int, token: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                val response = repository.getDepartmentReports(departmentId, token)
                if (response.isSuccessful) {
                    _departmentReports.value = response.body() ?: emptyList()
                }
            } catch (e: Exception) {
                Log.e("VM", "❌ Error fetching department reports", e)
            } finally {
                onComplete()
            }
        }
    }



    // 🔹 Отримати деталі звіту
    fun fetchReportDetails(reportId: Int, token: String) {
        viewModelScope.launch {
            android.util.Log.d("DETAIL_DEBUG", "➡️ Починаю запит на звіт ID=$reportId")
            try {
                val response = repository.getReportDetails(reportId, token)
                if (response.isSuccessful) {
                    val body = response.body()
                    android.util.Log.d("DETAIL_DEBUG", "✅ Успішно отримано звіт: $body")
                    _selectedReport.value = body
                } else {
                    android.util.Log.e("DETAIL_DEBUG", "❌ Неуспішний запит: код ${response.code()}")
                }
            } catch (e: Exception) {
                android.util.Log.e("DETAIL_DEBUG", "❌ Виключення при запиті деталей", e)
            }
        }
    }



    private val _todayReports = MutableStateFlow<List<TodayAttendanceReport>>(emptyList())
    val todayReports: StateFlow<List<TodayAttendanceReport>> = _todayReports

    fun fetchTodayReportsByCourse(courseId: Int, token: String) {
        viewModelScope.launch {
            try {
                val response = repository.getTodayReportsByCourse(courseId, token)
                if (response.isSuccessful) {
                    val result = response.body()
                    android.util.Log.d("TODAY_REPORTS", "✅ Отримано ${result?.size ?: 0} звітів")
                    _todayReports.value = result ?: emptyList()
                } else {
                    android.util.Log.e("TODAY_REPORTS", "❌ Код помилки: ${response.code()}")
                }
            } catch (e: Exception) {
                android.util.Log.e("TODAY_REPORTS", "❌ Виключення: ${e.message}")
            }
        }
    }

    private val _todayDepartmentReports = MutableStateFlow<List<DepartmentReport>>(emptyList())
    val todayDepartmentReports: StateFlow<List<DepartmentReport>> = _todayDepartmentReports

    fun fetchTodayDepartmentReports(facultyId: Int, token: String) {
        viewModelScope.launch {
            try {
                val response = apiService.getTodayDepartmentReports(facultyId, "Bearer $token")
                if (response.isSuccessful) {
                    _todayDepartmentReports.value = response.body() ?: emptyList()
                    Log.d("TODAY_DEPT_REPORTS", "✅ Отримано ${_todayDepartmentReports.value.size} звітів по кафедрах")
                } else {
                    Log.e("TODAY_DEPT_REPORTS", "❌ Код помилки: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("TODAY_DEPT_REPORTS", "❌ Виняток: ${e.message}")
            }
        }
    }




    private val _missingGroups = MutableStateFlow<List<MissingGroup>>(emptyList())
    val missingGroups: StateFlow<List<MissingGroup>> = _missingGroups

    fun fetchMissingGroups(courseId: Int, token: String) {
        viewModelScope.launch {
            val response = repository.getMissingGroups(courseId, token)
            if (response.isSuccessful) {
                _missingGroups.value = response.body() ?: emptyList()
            }
        }
    }
    private val _summarySubmitResult = MutableStateFlow<String?>(null)
    val summarySubmitResult: StateFlow<String?> = _summarySubmitResult

    fun submitCourseSummary(courseId: Int, token: String) {
        viewModelScope.launch {
            try {
                val response = repository.submitCourseSummary(courseId, token)
                if (response.isSuccessful) {
                    _summarySubmitResult.value = response.body()?.message

                    // 🔁 ОНОВИТИ СПИСОК ЗВЕДЕНЬ — ЩОБ З'ЯВИЛОСЯ 📝
                    fetchCourseSummaries(courseId, token)

                } else {
                    _summarySubmitResult.value = "❌ Помилка при поданні зведеного розходу"
                }
            } catch (e: Exception) {
                _summarySubmitResult.value = "❌ Виняток: ${e.message}"
            }
        }
    }


    fun clearSummaryMessage() {
        _summarySubmitResult.value = null
    }

    private val _courseSummaries = MutableStateFlow<List<CourseSummaryReport>>(emptyList())
    val courseSummaries: StateFlow<List<CourseSummaryReport>> = _courseSummaries


    fun fetchCourseSummaries(courseId: Int, token: String) {
        viewModelScope.launch {
            try {
                val response = repository.getCourseSummaries(courseId, token)
                if (response.isSuccessful) {
                    _courseSummaries.value = response.body() ?: emptyList()
                    Log.d("SUMMARY_HISTORY", "✅ Отримано ${_courseSummaries.value.size} записів")
                } else {
                    Log.e("SUMMARY_HISTORY", "❌ Помилка: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("SUMMARY_HISTORY", "❌ Виняток: ${e.message}")
            }
        }
    }



    private val _summaryAbsences = MutableStateFlow<List<SummaryAbsence>>(emptyList())
    val summaryAbsences: StateFlow<List<SummaryAbsence>> = _summaryAbsences


    fun fetchSummaryAbsences(summaryId: Int, token: String) {
        viewModelScope.launch {
            try {
                val absences = repository.fetchSummaryAbsences(summaryId, token)
                Log.d("VIEWMODEL", "✅ Завантажено ${absences.size} відсутніх")
                _summaryAbsences.value = absences
            } catch (e: Exception) {
                Log.e("VIEWMODEL", "❌ Помилка отримання відсутніх: ${e.message}")
            }
        }
    }

    fun fetchCourseSummary(summaryId: Int, token: String) {
        viewModelScope.launch {
            try {
                val response = repository.getCourseSummary(summaryId, token)
                if (response.isSuccessful) {
                    _selectedSummary.value = response.body()
                    Log.d("VIEWMODEL", "✅ Отримано summary: ${response.body()}")
                } else {
                    Log.e("VIEWMODEL", "❌ Код помилки: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("VIEWMODEL", "❌ Виняток у fetchCourseSummary: ${e.message}")
            }
        }
    }

    fun fetchLocationSummaryHistory(locationId: Int, token: String) {
        viewModelScope.launch {
            try {
                val response = repository.getLocationSummaryHistory(locationId, token)
                if (response.isSuccessful) {
                    _locationSummaries.value = response.body() ?: emptyList()
                    Log.d("VM_LOCATION_SUM", "✅ Отримано ${_locationSummaries.value.size} записів зведень")
                } else {
                    Log.e("VM_LOCATION_SUM", "❌ Помилка: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("VM_LOCATION_SUM", "❌ Виняток: ${e.message}")
            }
        }
    }


    private val _selectedSummary = MutableStateFlow<CourseSummaryReport?>(null)
    val selectedSummary: StateFlow<CourseSummaryReport?> = _selectedSummary


    fun setSelectedSummary(summary: CourseSummaryReport) {
        _selectedSummary.value = summary
    }

    private val _todayFacultySummaries = MutableStateFlow<List<TodaySummaryReport>>(emptyList())
    val todayFacultySummaries: StateFlow<List<TodaySummaryReport>> = _todayFacultySummaries

    fun fetchTodaySummariesByFaculty(facultyId: Int, token: String) {
        viewModelScope.launch {
            try {
                val response = repository.getTodaySummariesByFaculty(facultyId, token)
                if (response.isSuccessful) {
                    _todayFacultySummaries.value = response.body() ?: emptyList()
                } else {
                    Log.e("VM", "Помилка отримання зведень: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("VM", "❌ Виняток: ${e.message}")
            }
        }
    }



    private val _missingCourses = MutableStateFlow<List<MissingCourse>>(emptyList())
    val missingCourses: StateFlow<List<MissingCourse>> = _missingCourses

    fun fetchMissingCourses(facultyId: Int, token: String) {
        viewModelScope.launch {
            try {
                val response = repository.getMissingCourses(facultyId, token)
                if (response.isSuccessful) {
                    _missingCourses.value = response.body() ?: emptyList()
                } else {
                    android.util.Log.e("VM", "❌ Код помилки при fetchMissingCourses: ${response.code()}")
                }
            } catch (e: Exception) {
                android.util.Log.e("VM", "❌ Виняток у fetchMissingCourses: ${e.message}")
            }
        }
    }

    private val _facultySummaries = MutableStateFlow<List<CourseSummaryReport>>(emptyList())
    val facultySummaries: StateFlow<List<CourseSummaryReport>> = _facultySummaries

    fun fetchFacultySummaries(facultyId: Int, token: String) {
        viewModelScope.launch {
            try {
                val response = repository.getSummariesByFaculty(facultyId, token)
                if (response.isSuccessful) {
                    _facultySummaries.value = response.body() ?: emptyList()
                    Log.d("SUMMARY_HISTORY", "✅ Отримано ${_facultySummaries.value.size} зведень по НФ")
                } else {
                    Log.e("SUMMARY_HISTORY", "❌ Помилка: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("SUMMARY_HISTORY", "❌ Виняток: ${e.message}")
            }
        }
    }


    fun submitFacultySummary(
        facultyId: Int,
        token: String,
        request: FacultySummaryRequest,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val response = repository.submitFacultySummary(facultyId, token, request)
                if (response.isSuccessful) {
                    onResult(true, response.body()?.message ?: "✅ Зведення подано")
                } else {
                    onResult(false, "❌ Помилка: ${response.code()}")
                }
            } catch (e: Exception) {
                onResult(false, "❌ Виняток: ${e.message}")
            }
        }
    }



    private val _missingDepartments = MutableStateFlow<List<DepartmentItem>>(emptyList())
    val missingDepartments: StateFlow<List<DepartmentItem>> = _missingDepartments


    fun fetchMissingDepartments(facultyId: Int, token: String) {
        viewModelScope.launch {
            try {
                val response = repository.getMissingDepartmentsByFaculty(facultyId, token)
                if (response.isSuccessful) {
                    _missingDepartments.value = response.body() ?: emptyList()
                    Log.d("VM", "✅ Кафедри без розходу: ${_missingDepartments.value.size}")
                } else {
                    Log.e("VM", "❌ Помилка при fetchMissingDepartments: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("VM", "❌ Виняток у fetchMissingDepartments: ${e.message}")
            }
        }
    }

    private val _submittedReports = MutableStateFlow<List<Report>>(emptyList())
    val submittedReports: StateFlow<List<Report>> = _submittedReports

    fun fetchSubmittedReportsByLocation(locationId: Int, token: String) {
        viewModelScope.launch {
            try {
                val response = repository.getSubmittedReportsByLocation(locationId, token)
                if (response.isSuccessful) {
                    _submittedReports.value = response.body() ?: emptyList()
                    Log.d("AttendanceVM", "✅ Подані розходи по локації: ${_submittedReports.value.size}")
                } else {
                    Log.e("AttendanceVM", "❌ Помилка: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("AttendanceVM", "❌ Виняток при отриманні поданих розходів: ${e.message}")
            }
        }
    }


    fun submitLocationSummary(locationId: Int, token: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val response = repository.submitLocationSummary(token, locationId)
                if (response.isSuccessful) {
                    onResult(true, "✅ Зведення по локації подано успішно")
                } else {
                    onResult(false, "❌ Помилка: ${response.code()}")
                }
            } catch (e: Exception) {
                onResult(false, "❌ Виняток: ${e.message}")
            }
        }
    }


    // 🔹 Подати розхід (submit)
    fun submitAttendanceReportFlexible(
        groupId: Int?,
        departmentId: Int?,
        totalCount: Int,
        presentCount: Int,
        absences: List<Map<String, String>>,
        token: String,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val response = repository.submitAttendanceReportFlexible(
                    groupId = groupId,
                    departmentId = departmentId,
                    totalCount = totalCount,
                    presentCount = presentCount,
                    absences = absences,
                    token = token
                )

                if (response.isSuccessful) {
                    _submissionSuccess.value = true
                    onResult(true, "✅ Розхід успішно подано")
                } else {
                    onResult(false, "❌ Помилка: ${response.code()}")
                }
            } catch (e: Exception) {
                onResult(false, "❌ Виняток: ${e.message}")
            }
        }
    }

    private val _wordExportResult = MutableStateFlow<ResponseBody?>(null)
    val wordExportResult: StateFlow<ResponseBody?> = _wordExportResult

    fun exportSummaryToWord(summaryId: Int, token: String) {
        viewModelScope.launch {
            try {
                val response = repository.exportCourseSummaryToWord(summaryId, token)
                if (response.isSuccessful) {
                    _wordExportResult.value = response.body()
                    Log.d("EXPORT_WORD", "✅ Успішно отримано Word-файл для summaryId=$summaryId")
                } else {
                    Log.e("EXPORT_WORD", "❌ Не вдалося експортувати: ${response.code()}")
                    _wordExportResult.value = null
                }
            } catch (e: Exception) {
                Log.e("EXPORT_WORD", "❌ Виняток під час експорту: ${e.message}")
                _wordExportResult.value = null
            }
        }
    }

    fun clearWordExportResult() {
        _wordExportResult.value = null
    }



    fun fetchMissingGroupsByLocation(locationId: Int, token: String, onResult: (List<MissingGroup>) -> Unit) {
        viewModelScope.launch {
            try {
                val response = repository.getMissingGroupsByLocation(locationId, token)
                if (response.isSuccessful) {
                    val groups = response.body()
                    if (groups != null) {
                        onResult(groups)
                    } else {
                        Log.e("AttendanceVM", "❌ Групи не отримано або null")
                        onResult(emptyList())
                    }
                } else {
                    Log.e("AttendanceVM", "❌ Помилка отримання груп по локації: ${response.code()}")
                    onResult(emptyList())
                }
            } catch (e: Exception) {
                Log.e("AttendanceVM", "❌ Виняток при отриманні груп по локації: ${e.message}")
                onResult(emptyList())
            }
        }
    }






    // 🔹 Фабрика для ViewModel
    class Factory(private val repository: AttendanceRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AttendanceViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return AttendanceViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
