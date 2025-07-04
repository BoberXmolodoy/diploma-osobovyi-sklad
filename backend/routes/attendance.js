  const express = require('express');
  const router = express.Router();
  const attendanceController = require('../controllers/attendanceController');
  const { authMiddleware, authorizeRole } = require('../middleware/authMiddleware');

  // 📤 Подання розходу (доступно всім авторизованим)
  router.post(
      '/submit',
      authMiddleware,
      attendanceController.submitAttendanceReport
  );

  // 📥 Перегляд розходів по курсу (тільки для начальника курсу)
  router.get(
      '/course/:course_id/reports',
      authMiddleware,
      authorizeRole(['nk']),
      attendanceController.getReportsByCourse
  );

  router.get(
      '/today/course/:course_id',
      authMiddleware,
      authorizeRole(['nk']),
      attendanceController.getTodayReportsByCourse
    );
    

  // 📆 Отримання розходів за сьогодні по курсу
  router.get(
      '/today/course/:course_id',
      authMiddleware,
      authorizeRole(['nk']),
      attendanceController.getTodayReportsByCourse
    );
    
  // 📋 Перегляд деталей розходу (з відсутніми)
 router.get(
  '/report/:report_id',
  authMiddleware,
  authorizeRole(['nk', 'kg', 'nkf']),
  attendanceController.getReportDetails
);

    
  // 🕓 Історія розходів по групі (для командира групи)
  router.get(
      '/group/:groupId/reports',
      authMiddleware,
      authorizeRole(['kg']),
      attendanceController.getReportsByGroup
  );

  // 📍 Групи, які не подали розхід за сьогодні
  router.get(
    '/today/course/:course_id/missing',
    authMiddleware,
    authorizeRole(['nk']),
    attendanceController.getMissingGroups
  );


  // 📤 Подати зведений розхід на НФ
  router.post(
    '/course/:course_id/submit-summary',
    authMiddleware,
    authorizeRole(['nk']),
    attendanceController.generateAndSubmitCourseSummary
  );

  // 🕓 Історія зведених розходів по курсу
  router.get(
    '/course/:course_id/summaries',
    authMiddleware,
    authorizeRole(['nk']),
    attendanceController.getCourseSummaryHistory
  );

  router.get(
    '/summary/:summary_id/absences',
    authMiddleware,
    authorizeRole(['nk', 'nf']),
    attendanceController.getCourseSummaryAbsences
  );

  router.get(
    '/faculty/:faculty_id/summaries',
    authMiddleware,
    authorizeRole(['nf']),
    attendanceController.getSummariesByFaculty
  );

  router.get(
    '/faculty/:faculty_id/summaries/today',
    authMiddleware,
    authorizeRole(['nf']),
    attendanceController.getTodaySummariesByFaculty
  );

  router.get(
    '/faculty/:faculty_id/missing-groups',
    authMiddleware,
    authorizeRole(['nf']),
    attendanceController.getMissingGroupsByFaculty
  );

  // 📍 Курси факультету, які не подали зведений розхід за сьогодні
  router.get(
    '/faculty/:faculty_id/missing-courses',
    authMiddleware,
    authorizeRole(['nf']),
    attendanceController.getMissingCoursesByFaculty
  );

  // 📄 Отримати одне зведення по ID
  router.get(
    '/summary/:summary_id',
    authMiddleware,
    authorizeRole(['nk', 'nf']),
    attendanceController.getCourseSummaryById
  );


  // 🧾 Створити зведення по факультету
  router.post(
    '/faculty/:faculty_id/submit-summary',
    authMiddleware,
    authorizeRole(['nf']),
    attendanceController.generateAndSubmitFacultySummary
  );

  // 📍 Кафедри, які не подали зведений розхід за сьогодні
  router.get(
    '/faculty/:faculty_id/missing-departments',
    authMiddleware,
    authorizeRole(['nf']),
    attendanceController.getMissingDepartmentsByFaculty
  );


  router.get(
    '/department/:department_id/reports',
    authMiddleware,
    authorizeRole(['nkf']),
    attendanceController.getReportsByDepartment
  );

// 📆 Отримання розходів по кафедрах за сьогодні (для НФ)
router.get(
  '/faculty/:faculty_id/departments/reports/today',
  authMiddleware,
  authorizeRole(['nf']),
  attendanceController.getTodayReportsByDepartments
);

// 📍 Групи локації, які не подали розхід за сьогодні
router.get(
  '/location/:location_id/missing-groups',
  authMiddleware,
  authorizeRole(['cl']), // тільки для чергового локації
  attendanceController.getMissingGroupsByLocation
);

// 📥 Подані розходи по локації (для чергового локації)
router.get(
  '/location/:location_id/reports',
  authMiddleware,
  authorizeRole(['cl']),
  attendanceController.getSubmittedReportsByLocation
);


// 📤 Подати зведений розхід по локації (для ЧЛ)
router.post(
  '/location/:location_id/submit-summary',
  authMiddleware,
  authorizeRole(['cl']),
  attendanceController.submitLocationSummary
);

// 🕓 Історія зведених розходів по локації (для ЧЛ)
router.get(
  '/location/:location_id/summaries',
  authMiddleware,
  authorizeRole(['cl']),
  attendanceController.getLocationSummaryHistory
);


// 🧾 Отримати всі зведення для Пункту управління
router.get(
  '/summaries/pu',
  authMiddleware,
  authorizeRole(['pu']), // тільки для ПУ
  attendanceController.getAllSummariesForPU
);


// 🧾 Експортувати зведення у Word
router.get(
  '/summary/:summary_id/export-word',
  authMiddleware,
  authorizeRole(['nk']),
  attendanceController.exportCourseSummaryToWord
);


  module.exports = router;
