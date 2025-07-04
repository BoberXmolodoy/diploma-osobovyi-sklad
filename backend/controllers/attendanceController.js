const pool = require('../config/database');
const fs = require("fs");
const path = require("path");
const { Document, Packer, Paragraph, TextRun, Table, TableRow, TableCell, WidthType } = require("docx");
exports.submitAttendanceReport = async (req, res) => {
  const { group_id, department_id, total_count, present_count, absences } = req.body;
  const submitted_by = req.user.id;
  const userRole = req.user.role;

  if (typeof total_count !== 'number' || typeof present_count !== 'number') {
    return res.status(400).json({ error: '❌ total_count та present_count мають бути числами' });
  }

  if (userRole === 'kg' && !group_id) {
    return res.status(400).json({ error: '❌ Не передано group_id для командира групи' });
  }

  if (userRole === 'nkf' && !department_id) {
    return res.status(400).json({ error: '❌ Не передано department_id для начальника кафедри' });
  }

  try {
    const now = new Date();
    const localDate = new Date(now.getTime() - now.getTimezoneOffset() * 60000)
      .toISOString()
      .replace('T', ' ')
      .slice(0, 19);
    const reportDateOnly = localDate.split(' ')[0];

    const insertedIds = [];

    if (userRole === 'kg') {
  // 🔍 Отримати додаткові дані про групу
  const groupRes = await pool.query(`
    SELECT course_id, group_number FROM groups WHERE id = $1
  `, [group_id]);

  if (groupRes.rows.length === 0) {
    return res.status(404).json({ error: '❌ Групу не знайдено' });
  }

  const course_id = groupRes.rows[0].course_id;
  const group_number = groupRes.rows[0].group_number;
  const location_id = parseInt(String(group_number).charAt(0));

  // 🔍 Чи існує звіт?
  const existingRes = await pool.query(
    `SELECT id FROM attendance_reports 
     WHERE group_id = $1 AND report_date::date = $2::date`,
    [group_id, reportDateOnly]
  );

  let reportId;

  if (existingRes.rows.length > 0) {
    // 🔁 Оновлення існуючого
    reportId = existingRes.rows[0].id;

    await pool.query(`
      UPDATE attendance_reports
      SET course_id = $2,
          location_id = $3,
          total_count = $4,
          present_count = $5,
          updated_at = NOW(),
          was_updated = TRUE,
          submitted_by = $6
      WHERE id = $1
    `, [
      reportId,
      course_id,
      location_id,
      total_count,
      present_count,
      submitted_by
    ]);

    // 🧹 Очистити старі absences
    await pool.query(`DELETE FROM attendance_absences WHERE report_id = $1`, [reportId]);

  } else {
    // 🆕 Новий звіт
    const resInsert = await pool.query(`
      INSERT INTO attendance_reports 
        (group_id, course_id, location_id, total_count, present_count, report_date, submitted_by)
      VALUES ($1, $2, $3, $4, $5, $6, $7)
      RETURNING id
    `, [
      group_id,
      course_id,
      location_id,
      total_count,
      present_count,
      localDate,
      submitted_by
    ]);
    reportId = resInsert.rows[0].id;
  }

  insertedIds.push(reportId);

} else if (userRole === 'nkf') {
  // 🔍 Перевірити чи вже є звіт від НКФ на сьогодні
  const existingRes = await pool.query(
    `SELECT id FROM attendance_reports 
     WHERE department_id = $1 AND report_date::date = $2::date`,
    [department_id, reportDateOnly]
  );

  let reportId;

  if (existingRes.rows.length > 0) {
    // 🔁 Оновлення існуючого
    reportId = existingRes.rows[0].id;

    await pool.query(`
      UPDATE attendance_reports
      SET total_count = $1,
          present_count = $2,
          updated_at = NOW(),
          was_updated = TRUE,
          submitted_by = $3
      WHERE id = $4
    `, [
      total_count,
      present_count,
      submitted_by,
      reportId
    ]);

    await pool.query(`DELETE FROM attendance_absences WHERE report_id = $1`, [reportId]);

  } else {
    // 🆕 Новий звіт
    const resInsert = await pool.query(`
      INSERT INTO attendance_reports 
        (department_id, total_count, present_count, report_date, submitted_by)
      VALUES ($1, $2, $3, $4, $5)
      RETURNING id
    `, [
      department_id,
      total_count,
      present_count,
      localDate,
      submitted_by
    ]);
    reportId = resInsert.rows[0].id;
  }

  insertedIds.push(reportId);

} else {
  return res.status(403).json({ error: '❌ Ця роль не має права подавати розхід' });
}

// 🧾 Додати відсутніх
for (const reportId of insertedIds) {
  for (const absence of absences) {
    const { full_name, reason } = absence;
    if (full_name && reason) {
      await pool.query(
        'INSERT INTO attendance_absences (report_id, full_name, reason) VALUES ($1, $2, $3)',
        [reportId, full_name, reason]
      );
    }
  }
}


    res.status(201).json({ message: '✅ Розхід успішно подано або оновлено' });

  } catch (error) {
    console.error('❌ Помилка при подачі розходу:', error);
    res.status(500).json({ error: '❌ Помилка при подачі розходу' });
  }
};



// ✅ Перегляд розходів по курсу
exports.getReportsByCourse = async (req, res) => {
  const { course_id } = req.params;

  try {
    const result = await pool.query(`
      SELECT ar.id AS report_id, ar.report_date, g.number AS group_number, 
             ar.total_count, ar.present_count
      FROM attendance_reports ar
      JOIN groups g ON ar.group_id = g.id
      WHERE g.course_id = $1
      ORDER BY ar.report_date DESC
    `, [course_id]);

    res.json(result.rows);
  } catch (error) {
    console.error('❌ Помилка отримання розходів:', error);
    res.status(500).json({ error: '❌ Помилка при отриманні розходів' });
  }
};

// ✅ Детальний перегляд одного розходу (з відсутніми)
exports.getReportDetails = async (req, res) => {
  console.log('🧠 getReportDetails activated');

  const { report_id } = req.params;
  const user = req.user;

  try {
    const reportResult = await pool.query(`
      SELECT 
        ar.id AS report_id, 
        ar.report_date, 
        ar.total_count, 
        ar.present_count,
        ar.group_id,
        ar.department_id,
        ar.submitted_by,
        g.group_number,
        d.name AS department_name
      FROM attendance_reports ar
      LEFT JOIN groups g ON ar.group_id = g.id
      LEFT JOIN departments d ON ar.department_id = d.id
      WHERE ar.id = $1
    `, [report_id]);

    if (reportResult.rowCount === 0) {
      return res.status(404).json({ error: '❌ Розхід не знайдено' });
    }

    const report = reportResult.rows[0];

    console.log('📦 Звіт:', report);
    console.log('👤 Користувач:', user);

    if (user.role === 'kg' && String(report.group_id) !== String(user.group_id)) {
      console.log('❌ KG не має доступу до цієї групи');
      return res.status(403).json({ error: '❌ Недостатньо прав для перегляду цього звіту' });
    }

    if (user.role === 'nkf') {
      const hasAccessToDepartmentReport =
        report.department_id && String(report.department_id) === String(user.department_id);

      let hasAccessToGroupReport = false;
      if (report.group_id) {
        const groupCheck = await pool.query(`
          SELECT 1
          FROM groups g
          JOIN courses c ON g.course_id = c.id
          WHERE g.id = $1 AND c.faculty_id = $2
        `, [report.group_id, user.faculty_id]);
        hasAccessToGroupReport = groupCheck.rowCount > 0;
      }

      const isSubmittedByUser = String(report.submitted_by) === String(user.id);

      console.log('✅ Доступ до кафедрального звіту:', hasAccessToDepartmentReport);
      console.log('✅ Доступ до курсового звіту:', hasAccessToGroupReport);
      console.log('✅ Це його власний звіт:', isSubmittedByUser);

      if (!hasAccessToDepartmentReport && !hasAccessToGroupReport && !isSubmittedByUser) {
        console.log('🚫 Відмова доступу: всі умови false');
        return res.status(403).json({ error: '❌ Недостатньо прав для перегляду цього звіту' });
      }
    }

    const absencesResult = await pool.query(`
      SELECT full_name, reason
      FROM attendance_absences
      WHERE report_id = $1
    `, [report_id]);

    report.absences = absencesResult.rows;

    res.json(report);
  } catch (error) {
    console.error('❌ Помилка при отриманні деталей розходу:', error);
    res.status(500).json({ error: '❌ Помилка при отриманні деталей' });
  }
};




// ✅ Історія розходів по групі
exports.getReportsByGroup = async (req, res) => {
  const { groupId } = req.params;

  try {
    const result = await pool.query(
      `SELECT ar.id, ar.report_date, ar.total_count, ar.present_count,
              ar.was_updated, ar.updated_at,
              g.group_number
       FROM attendance_reports ar
       JOIN groups g ON ar.group_id = g.id
       WHERE ar.group_id = $1 
       ORDER BY ar.report_date DESC`,
      [groupId]
    );

    res.json(result.rows);
  } catch (error) {
    console.error('❌ Помилка отримання історії по групі:', error);
    res.status(500).json({ error: '❌ Помилка при отриманні історії' });
  }
};



// 📆 Отримання звітів за сьогодні по курсу
exports.getTodayReportsByCourse = async (req, res) => {
  const { course_id } = req.params;

  try {
    const today = new Date();
    const localDate = new Date(today.getTime() - today.getTimezoneOffset() * 60000)
      .toISOString()
      .split("T")[0]; // YYYY-MM-DD

    const result = await pool.query(`
      SELECT ar.id AS report_id, 
             ar.report_date, 
             g.group_number AS group_number, 
             ar.total_count, 
             ar.present_count,
             ar.was_updated,
             ar.updated_at
      FROM attendance_reports ar
      JOIN groups g ON ar.group_id = g.id
      WHERE g.course_id = $1 AND DATE(ar.report_date) = $2
      ORDER BY g.group_number
    `, [course_id, localDate]);

    res.json(result.rows);
  } catch (error) {
    console.error('❌ Помилка отримання розходів за сьогодні:', error);
    res.status(500).json({ error: 'Помилка при отриманні розходів за сьогодні' });
  }
};


// 📍 Групи, які не подали розхід за сьогодні
exports.getMissingGroups = async (req, res) => {
  const { course_id } = req.params;

  try {
    const today = new Date();
    const localDate = new Date(today.getTime() - today.getTimezoneOffset() * 60000)
      .toISOString()
      .split("T")[0]; // YYYY-MM-DD

    const result = await pool.query(`
      SELECT g.group_number
      FROM groups g
      WHERE g.course_id = $1
        AND g.commander_user_id IS NOT NULL
        AND g.id NOT IN (
          SELECT group_id
          FROM attendance_reports
          WHERE DATE(report_date) = $2
        )
      ORDER BY g.group_number
    `, [course_id, localDate]);

    res.json(result.rows);
  } catch (error) {
    console.error("❌ Помилка при отриманні груп без розходу:", error);
    res.status(500).json({ error: "Помилка при отриманні груп без розходу" });
  }
};



exports.getCourseSummaryHistory = async (req, res) => {
  const courseId = req.params.course_id;

  try {
    const result = await pool.query(`
      SELECT id, summary_date, total_count, present_count, absent_count, reasons, was_updated, updated_at
      FROM course_summaries
      WHERE course_id = $1 AND total_count IS NOT NULL
      ORDER BY summary_date DESC
    `, [courseId]);

    const parsed = result.rows.map(row => ({
      ...row,
      reasons: Array.isArray(row.reasons)
        ? row.reasons.map(r => ({
            reason: r.reason,
            count: typeof r.count === 'string' ? parseInt(r.count, 10) : r.count
          }))
        : [],
      was_updated: row.was_updated ?? false,
      updated_at: row.updated_at
    }));

    res.json(parsed);
  } catch (err) {
    console.error("❌ Помилка в getCourseSummaryHistory:", err);
    res.status(500).json({ error: 'Помилка при отриманні зведених звітів' });
  }
};



exports.generateAndSubmitCourseSummary = async (req, res) => {
  const { course_id } = req.params;
  const created_by = req.user?.id;

  try {
    const today = new Date();
    const localDate = new Date(today.getTime() - today.getTimezoneOffset() * 60000)
      .toISOString()
      .split("T")[0];

    // 📥 Отримати звіти по групах курсу
    const reportsResult = await pool.query(`
      SELECT ar.id AS report_id, ar.total_count, ar.present_count
      FROM attendance_reports ar
      JOIN groups g ON ar.group_id = g.id
      WHERE g.course_id = $1 AND DATE(ar.report_date) = $2
    `, [course_id, localDate]);

    const reports = reportsResult.rows;
    if (reports.length === 0) {
      return res.status(400).json({ error: '❌ Немає звітів для курсу за сьогодні' });
    }

    const reportIds = reports.map(r => r.report_id);
    const total = reports.reduce((sum, r) => sum + r.total_count, 0);
    const present = reports.reduce((sum, r) => sum + r.present_count, 0);
    const absent = total - present;

    // 📊 Підрахунок причин
    const reasonsResult = await pool.query(`
      SELECT reason, COUNT(*)::int AS count
      FROM attendance_absences
      WHERE report_id = ANY($1::int[])
      GROUP BY reason
    `, [reportIds]);

    const reasons = reasonsResult.rows.map(row => ({
      reason: row.reason,
      count: row.count
    }));

    // 🔍 Перевірка наявності зведення
    const existingSummary = await pool.query(`
      SELECT id FROM course_summaries
      WHERE course_id = $1 AND DATE(summary_date) = $2
    `, [course_id, localDate]);

    let summaryId;

    if (existingSummary.rowCount > 0) {
      summaryId = existingSummary.rows[0].id;

      await pool.query(`
        UPDATE course_summaries
        SET total_count = $1,
            present_count = $2,
            absent_count = $3,
            reasons = $4,
            was_updated = TRUE,
            updated_at = NOW()
        WHERE id = $5
      `, [total, present, absent, JSON.stringify(reasons), summaryId]);

      await pool.query(`DELETE FROM course_summary_absences WHERE summary_id = $1`, [summaryId]);
    } else {
      const summaryResult = await pool.query(`
        INSERT INTO course_summaries (
          course_id, summary_date, total_count, present_count, absent_count, reasons, created_by, created_at
        ) VALUES ($1, $2, $3, $4, $5, $6, $7, NOW())
        RETURNING id
      `, [course_id, localDate, total, present, absent, JSON.stringify(reasons), created_by]);

      summaryId = summaryResult.rows[0].id;
    }

    // 📝 Додати прізвища відсутніх
    const absencesResult = await pool.query(`
      SELECT full_name, reason
      FROM attendance_absences
      WHERE report_id = ANY($1::int[])
    `, [reportIds]);

    for (const { full_name, reason } of absencesResult.rows) {
      await pool.query(`
        INSERT INTO course_summary_absences (summary_id, full_name, reason)
        VALUES ($1, $2, $3)
      `, [summaryId, full_name, reason]);
    }

    // ✅ Відповідь
    res.status(201).json({
      message: existingSummary.rowCount > 0
        ? '📝 Зведення оновлено'
        : '✅ Зведений розхід подано на НК',
      summary_id: summaryId,
      total,
      present,
      absent,
      reasons
    });

  } catch (error) {
    console.error('❌ Помилка під час генерації зведеного розходу:', error);
    res.status(500).json({ error: '❌ Помилка під час генерації зведеного розходу' });
  }
};






exports.getCourseSummaryAbsences = async (req, res) => {
  const { summary_id } = req.params;

  try {
    const result = await pool.query(`
      SELECT full_name, reason
      FROM course_summary_absences
      WHERE summary_id = $1
    `, [summary_id]);

    res.json(result.rows);
  } catch (err) {
    console.error("❌ Помилка при отриманні відсутніх по зведенню:", err);
    res.status(500).json({ error: "Помилка при отриманні відсутніх" });
  }
};


exports.getSummariesByFaculty = async (req, res) => {
  const { faculty_id } = req.params;

  try {
    const result = await pool.query(`
      SELECT 
        cs.id, 
        cs.summary_date, 
        cs.total_count, 
        cs.present_count, 
        cs.absent_count, 
        cs.reasons,
        cs.course_id,
        cs.faculty_id,
        cs.updated_at,
        cs.was_updated,              -- Додаємо поле was_updated
        c.number AS course_number
      FROM course_summaries cs
      LEFT JOIN courses c ON cs.course_id = c.id
      WHERE cs.faculty_id = $1
      ORDER BY cs.summary_date DESC
    `, [faculty_id]);

    const parsed = result.rows.map(row => ({
      ...row,
      reasons: Array.isArray(row.reasons)
        ? row.reasons.map(r => ({
            reason: r.reason,
            count: typeof r.count === 'string' ? parseInt(r.count, 10) : r.count
          }))
        : []
    }));

    res.json(parsed);
  } catch (error) {
    console.error('❌ Помилка отримання зведень для факультету:', error);
    res.status(500).json({ error: 'Помилка при отриманні зведених розходів' });
  }
};




// 📆 Зведення по факультету за сьогодні (НФ)
exports.getTodaySummariesByFaculty = async (req, res) => {
  const { faculty_id } = req.params;

  try {
    const today = new Date();
    const localDate = new Date(today.getTime() - today.getTimezoneOffset() * 60000)
      .toISOString()
      .split("T")[0]; // YYYY-MM-DD

    const result = await pool.query(`
      SELECT cs.id, cs.summary_date, cs.total_count, cs.present_count, cs.absent_count, cs.reasons, c.number AS course_number
      FROM course_summaries cs
      JOIN courses c ON cs.course_id = c.id
      WHERE c.faculty_id = $1 AND DATE(cs.summary_date) = $2
      ORDER BY cs.summary_date DESC
    `, [faculty_id, localDate]);

    res.json(result.rows);
  } catch (error) {
    console.error('❌ Помилка отримання зведених розходів за сьогодні:', error);
    res.status(500).json({ error: 'Помилка при отриманні зведених розходів за сьогодні' });
  }
};


// 📍 Групи факультету, які не подали розхід за сьогодні
exports.getMissingGroupsByFaculty = async (req, res) => {
  const { faculty_id } = req.params;

  try {
    const today = new Date();
    const localDate = new Date(today.getTime() - today.getTimezoneOffset() * 60000)
      .toISOString()
      .split("T")[0]; // YYYY-MM-DD

    const result = await pool.query(`
      SELECT g.group_number, g.id AS group_id, c.number AS course_number
      FROM groups g
      JOIN courses c ON g.course_id = c.id
      WHERE c.faculty_id = $1
        AND g.commander_user_id IS NOT NULL
        AND g.id NOT IN (
          SELECT group_id
          FROM attendance_reports
          WHERE DATE(report_date) = $2
        )
      ORDER BY course_number, g.group_number
    `, [faculty_id, localDate]);

    res.json(result.rows);
  } catch (error) {
    console.error("❌ Помилка при отриманні груп факультету без розходу:", error);
    res.status(500).json({ error: "Помилка при отриманні груп без розходу" });
  }
};


exports.getMissingCoursesByFaculty = async (req, res) => {
  const { faculty_id } = req.params;

  try {
    const result = await pool.query(`
      SELECT c.id, c.number AS course_number
      FROM courses c
      WHERE c.faculty_id = $1
        AND NOT EXISTS (
          SELECT 1
          FROM groups g
          JOIN attendance_reports ar ON ar.group_id = g.id
          WHERE g.course_id = c.id AND DATE(ar.report_date) = CURRENT_DATE
        )
      ORDER BY c.number
    `, [faculty_id]);

    res.json(result.rows);
  } catch (error) {
    console.error("❌ Помилка при отриманні курсів без розходу:", error);
    res.status(500).json({ error: "Серверна помилка" });
  }
};

exports.getMissingDepartmentsByFaculty = async (req, res) => {
  const { faculty_id } = req.params;

  try {
    const result = await pool.query(`
      SELECT d.id, d.name
      FROM users u
      JOIN departments d ON u.department_id = d.id
      WHERE u.role = 'nkf'
        AND u.faculty_id = $1
        AND NOT EXISTS (
          SELECT 1
          FROM course_summaries cs
          WHERE cs.summary_date = CURRENT_DATE
            AND cs.created_by = u.id
        )
      ORDER BY d.name
    `, [faculty_id]);

    res.json(result.rows);
  } catch (err) {
    console.error("❌ Помилка в getMissingDepartmentsByFaculty:", err);
    res.status(500).json({ error: 'Помилка при отриманні кафедр без розходу' });
  }
};



// ✅ Отримати одне зведення по ID
exports.getCourseSummaryById = async (req, res) => {
  const { summary_id } = req.params;

  try {
    const result = await pool.query(`
      SELECT id, summary_date, total_count, present_count, absent_count, reasons
      FROM course_summaries
      WHERE id = $1
    `, [summary_id]);

    if (result.rowCount === 0) {
      return res.status(404).json({ error: '❌ Зведення не знайдено' });
    }

    res.json(result.rows[0]);
  } catch (err) {
    console.error("❌ Помилка при отриманні зведення:", err);
    res.status(500).json({ error: "Помилка при отриманні зведення" });
  }
};

exports.generateAndSubmitFacultySummary = async (req, res) => {
  const { faculty_id } = req.params;
  const { presentOfficersCount = 0, absencesOfficers = [] } = req.body;
  const created_by = req.user.id;

  try {
    const now = new Date();
    const localDate = new Date(now.getTime() - now.getTimezoneOffset() * 60000)
      .toISOString()
      .split("T")[0];

    // 🔍 Перевірка, чи вже існує зведення по факультету (не по курсу, не по локації)
    const existing = await pool.query(`
      SELECT id FROM course_summaries
      WHERE faculty_id = $1 AND DATE(summary_date) = $2
        AND course_id IS NULL AND location_id IS NULL
    `, [faculty_id, localDate]);

    let total = 0;
    let present = 0;
    const reportIds = [];

    // 📥 Дані по кафедрах
    const deptReports = await pool.query(`
      SELECT ar.id, ar.total_count, ar.present_count
      FROM attendance_reports ar
      JOIN users u ON ar.submitted_by = u.id
      WHERE u.faculty_id = $1
        AND u.role = 'nkf'
        AND ar.department_id IS NOT NULL
        AND DATE(ar.report_date) = $2
    `, [faculty_id, localDate]);

    for (const report of deptReports.rows) {
      total += report.total_count;
      present += report.present_count;
      reportIds.push(report.id);
    }

    // 📊 Причини по кафедрах
    const reasonCounts = new Map();

    const reasonsDb = await pool.query(`
      SELECT reason, COUNT(*)::int AS count
      FROM attendance_absences
      WHERE report_id = ANY($1::int[])
      GROUP BY reason
    `, [reportIds]);

    for (const row of reasonsDb.rows) {
      reasonCounts.set(row.reason, row.count);
    }

    // 📊 Причини по офіцерах
    let totalOfficersAbsent = 0;
    for (const abs of absencesOfficers) {
      const { reason, count } = abs;
      if (!reason || !count) continue;
      const parsed = parseInt(count, 10);
      const current = reasonCounts.get(reason) || 0;
      reasonCounts.set(reason, current + parsed);
      totalOfficersAbsent += parsed;
    }

    // ✅ Підрахунок підсумків
    const totalOfficers = presentOfficersCount + totalOfficersAbsent;
    total += totalOfficers;
    present += presentOfficersCount;
    const absent = total - present;

    const reasons = Array.from(reasonCounts.entries()).map(([reason, count]) => ({ reason, count }));

    let summaryId;
    console.log("🔎 existing summaries:", existing.rowCount, existing.rows);


    if (existing.rowCount > 0) {
      // 🔄 Оновлення існуючого зведення
      summaryId = existing.rows[0].id;

      await pool.query(`
        UPDATE course_summaries
        SET total_count = $1,
            present_count = $2,
            absent_count = $3,
            reasons = $4,
            was_updated = TRUE,
            updated_at = NOW()
        WHERE id = $5
      `, [total, present, absent, JSON.stringify(reasons), summaryId]);

      await pool.query(`DELETE FROM course_summary_absences WHERE summary_id = $1`, [summaryId]);

    } else {
      // 🆕 Створення нового зведення
      const insertResult = await pool.query(`
        INSERT INTO course_summaries (
          summary_date, total_count, present_count, absent_count,
          reasons, faculty_id, created_by, created_at
        ) VALUES ($1, $2, $3, $4, $5, $6, $7, NOW())
        RETURNING id
      `, [localDate, total, present, absent, JSON.stringify(reasons), faculty_id, created_by]);

      summaryId = insertResult.rows[0].id;
    }

    // 📝 Додати відсутніх по кафедрах
    const absencesDb = await pool.query(`
      SELECT full_name, reason
      FROM attendance_absences
      WHERE report_id = ANY($1::int[])
    `, [reportIds]);

    for (const row of absencesDb.rows) {
      await pool.query(`
        INSERT INTO course_summary_absences (summary_id, full_name, reason)
        VALUES ($1, $2, $3)
      `, [summaryId, row.full_name, row.reason]);
    }

    // 📝 Додати відсутніх офіцерів
    for (const abs of absencesOfficers) {
      const { reason, names } = abs;
      if (!reason || !names) continue;
      const list = names.split(',').map(n => n.trim()).filter(Boolean);
      for (const name of list) {
        await pool.query(`
          INSERT INTO course_summary_absences (summary_id, full_name, reason)
          VALUES ($1, $2, $3)
        `, [summaryId, name, reason]);
      }
    }

    return res.status(201).json({
      message: '✅ Зведення по факультету подано або оновлено',
      summary_id: summaryId,
      total,
      present,
      absent,
      reasons
    });

  } catch (error) {
    console.error("❌ Помилка при створенні факультетського зведення:", error);
    res.status(500).json({ error: "Серверна помилка при створенні зведення" });
  }
};





exports.getReportsByDepartment = async (req, res) => {
  const { department_id } = req.params;

  try {
    const result = await pool.query(
      `SELECT id, report_date, total_count, present_count, was_updated, updated_at
       FROM attendance_reports 
       WHERE department_id = $1
       ORDER BY report_date DESC`,
      [department_id]
    );

    res.json(result.rows);
  } catch (error) {
    console.error('❌ Помилка отримання історії по кафедрі:', error);
    res.status(500).json({ error: '❌ Помилка при отриманні історії' });
  }
};

  // 📆 Отримання розходів по кафедрах за сьогодні (для НФ)
exports.getTodayReportsByDepartments = async (req, res) => {
  const { faculty_id } = req.params;

  try {
    const today = new Date();
    const localDate = new Date(today.getTime() - today.getTimezoneOffset() * 60000)
      .toISOString()
      .split("T")[0]; // YYYY-MM-DD

    const result = await pool.query(`
      SELECT 
        ar.id AS report_id,
        ar.report_date,
        d.name AS department_name,
        ar.total_count,
        ar.present_count,
        ar.was_updated,
        ar.updated_at
      FROM attendance_reports ar
      JOIN departments d ON ar.department_id = d.id
      JOIN users u ON ar.submitted_by = u.id
      WHERE u.faculty_id = $1
        AND u.role = 'nkf'
        AND DATE(ar.report_date) = $2
      ORDER BY d.name
    `, [faculty_id, localDate]);

    res.json(result.rows);
  } catch (error) {
    console.error('❌ Помилка при отриманні розходів по кафедрах за сьогодні:', error);
    res.status(500).json({ error: 'Помилка при отриманні розходів по кафедрах' });
  }
};



exports.getMissingGroupsByLocation = async (req, res) => {
  const { location_id } = req.params;

  try {
    const today = new Date();
    const localDate = new Date(today.getTime() - today.getTimezoneOffset() * 60000)
      .toISOString()
      .split("T")[0]; // YYYY-MM-DD

    const result = await pool.query(`
      SELECT g.id AS group_id, g.group_number
      FROM groups g
      WHERE LEFT(g.group_number::text, 1) = $1
        AND g.commander_user_id IS NOT NULL
        AND g.id NOT IN (
          SELECT group_id
          FROM attendance_reports
          WHERE DATE(report_date) = $2
            AND group_id IS NOT NULL
        )
      ORDER BY g.group_number
    `, [String(location_id), localDate]);

    res.json(result.rows);
  } catch (error) {
    console.error("❌ Помилка при отриманні груп по локації без розходу:", error);
    res.status(500).json({ error: "Помилка при отриманні груп без розходу" });
  }
};


exports.getSubmittedReportsByLocation = async (req, res) => {
  const { location_id } = req.params;

  try {
    const today = new Date();
    const localDate = new Date(today.getTime() - today.getTimezoneOffset() * 60000)
      .toISOString()
      .split("T")[0]; // YYYY-MM-DD

    const result = await pool.query(`
      SELECT ar.id AS report_id,
             ar.report_date,
             ar.total_count,
             ar.present_count,
             ar.was_updated,
             ar.updated_at,
             g.group_number,
             u.name AS submitted_by_name
      FROM attendance_reports ar
      LEFT JOIN groups g ON ar.group_id = g.id
      LEFT JOIN users u ON ar.submitted_by = u.id
      WHERE ar.location_id = $1
        AND DATE(ar.report_date) = $2
        AND ar.group_id IS NOT NULL -- 🔒 лише звіти від груп
      ORDER BY ar.report_date DESC
    `, [location_id, localDate]);

    res.json(result.rows);
  } catch (error) {
    console.error("❌ Помилка при отриманні поданих розходів по локації:", error);
    res.status(500).json({ error: "Помилка при отриманні розходів по локації" });
  }
};


exports.submitLocationSummary = async (req, res) => {
  const location_id = parseInt(req.params.location_id);
  const submitted_by = req.user?.id;

  if (!location_id || !submitted_by) {
    return res.status(400).json({ message: 'location_id і submitted_by обовʼязкові' });
  }

  try {
    const today = new Date().toISOString().split('T')[0];

    const { rows: groups } = await pool.query(`
      SELECT id FROM groups
      WHERE group_number::text LIKE $1
    `, [`${location_id}%`]);

    if (!groups.length) {
      return res.status(400).json({ message: 'Немає груп для цієї локації' });
    }

    const groupIds = groups.map(g => g.id);
    const pgArray = `{${groupIds.join(',')}}`;

    const { rows: reports } = await pool.query(`
      SELECT id FROM attendance_reports
      WHERE group_id = ANY($1::int[]) AND report_date::date = $2
    `, [pgArray, today]);

    if (!reports.length) {
      return res.status(400).json({ message: 'Жодна група не подала розхід сьогодні' });
    }

    const { rows: summaryRows } = await pool.query(`
      SELECT SUM(total_count)::int AS total, SUM(present_count)::int AS present
      FROM attendance_reports
      WHERE group_id = ANY($1::int[]) AND report_date::date = $2
    `, [pgArray, today]);

    const total = summaryRows[0].total ?? 0;
    const present = summaryRows[0].present ?? 0;
    const absent = total - present;

    const { rows: absences } = await pool.query(`
      SELECT reason, COUNT(*)::int AS count
      FROM attendance_absences
      WHERE report_id IN (
        SELECT id FROM attendance_reports
        WHERE group_id = ANY($1::int[]) AND report_date::date = $2
      )
      GROUP BY reason
    `, [pgArray, today]);

    const reasons = absences.map(r => ({
      reason: r.reason,
      count: r.count
    }));

    // ❗ Чи вже є зведення по course_summaries
    const { rows: existing } = await pool.query(`
      SELECT id FROM course_summaries
      WHERE location_id = $1 AND summary_date = $2
    `, [location_id, today]);

    // ❗ Чи вже є запис у attendance_reports (по локації, без group/course/department)
    const { rows: attendanceExisting } = await pool.query(`
      SELECT id FROM attendance_reports
      WHERE location_id = $1 AND report_date = $2
        AND group_id IS NULL AND course_id IS NULL AND department_id IS NULL
    `, [location_id, today]);

    if (existing.length > 0) {
      // 🔁 Оновлюємо course_summaries
      await pool.query(`
        UPDATE course_summaries
        SET total_count = $1,
            present_count = $2,
            absent_count = $3,
            reasons = $4,
            was_updated = TRUE,
            updated_at = NOW()
        WHERE id = $5
      `, [total, present, absent, JSON.stringify(reasons), existing[0].id]);

      // 🔁 Оновлюємо або додаємо в attendance_reports
      if (attendanceExisting.length > 0) {
        await pool.query(`
          UPDATE attendance_reports
          SET total_count = $1,
              present_count = $2,
              submitted_by = $3
          WHERE id = $4
        `, [total, present, submitted_by, attendanceExisting[0].id]);
      } else {
        await pool.query(`
          INSERT INTO attendance_reports (location_id, report_date, total_count, present_count, submitted_by)
          VALUES ($1, $2, $3, $4, $5)
        `, [location_id, today, total, present, submitted_by]);
      }

      return res.status(200).json({ message: '📝 Зведення по локації оновлено' });
    }

    // 🆕 Якщо зведення ще не існує — вставляємо нові записи
    await pool.query(`
      INSERT INTO attendance_reports (location_id, report_date, total_count, present_count, submitted_by)
      VALUES ($1, $2, $3, $4, $5)
    `, [location_id, today, total, present, submitted_by]);

    await pool.query(`
      INSERT INTO course_summaries (summary_date, total_count, present_count, absent_count, reasons, location_id, created_by, created_at)
      VALUES ($1, $2, $3, $4, $5, $6, $7, NOW())
    `, [
      today,
      total,
      present,
      absent,
      JSON.stringify(reasons),
      location_id,
      submitted_by
    ]);

    return res.status(201).json({ message: '✅ Зведення по локації подано успішно' });

  } catch (error) {
    console.error('❌ Помилка під час подання зведення ЧЛ:', error);
    return res.status(500).json({ message: '❌ Внутрішня помилка сервера', error: error.message });
  }
};





exports.getLocationSummaryHistory = async (req, res) => {
  const location_id = parseInt(req.params.location_id);

  if (!location_id) {
    return res.status(400).json({ message: 'location_id обовʼязковий' });
  }

  try {
    const { rows } = await pool.query(`
      SELECT 
        id, 
        report_date, 
        total_count, 
        present_count,
        (total_count - present_count) AS absent_count,
        was_updated,
        updated_at
      FROM attendance_reports
      WHERE location_id = $1 AND group_id IS NULL
      ORDER BY report_date DESC
    `, [location_id]);

    return res.json(rows);
  } catch (error) {
    console.error('Помилка при отриманні історії зведень по локації:', error);
    return res.status(500).json({ message: 'Внутрішня помилка сервера' });
  }
};

exports.getAllSummariesForPU = async (req, res) => {
  try {
    // ✅ Отримати сьогоднішню дату у форматі YYYY-MM-DD
    const today = new Date();
    const localDate = new Date(today.getTime() - today.getTimezoneOffset() * 60000)
      .toISOString()
      .split('T')[0];

    // 📥 Запит тільки зведень за сьогодні
    const result = await pool.query(`
      SELECT cs.id, cs.summary_date, cs.total_count, cs.present_count, cs.absent_count,
             cs.reasons,
             cs.faculty_id,
             cs.location_id,
             f.name AS faculty_name,
             l.name AS location_name
      FROM course_summaries cs
      LEFT JOIN faculties f ON cs.faculty_id = f.id
      LEFT JOIN locations l ON cs.location_id = l.id
      WHERE (cs.faculty_id IS NOT NULL OR cs.location_id IS NOT NULL)
        AND DATE(cs.summary_date) = $1
      ORDER BY cs.created_at DESC
    `, [localDate]);

    // 🧾 Парсинг і форматування
    const summaries = result.rows.map(row => ({
      id: row.id,
      summary_date: row.summary_date.toISOString().split('T')[0],
      present_count: row.present_count,
      absent_count: row.absent_count,
      faculty_name: row.faculty_name || null,
      location_name: row.location_name || null,
      faculty_id: row.faculty_id || null,
      location_id: row.location_id || null,
      reasons: Array.isArray(row.reasons)
        ? row.reasons
        : JSON.parse(row.reasons || '[]')
    }));

    // 🔁 Рендер
    res.render('pu_home', {
      title: 'Сьогоднішні зведення',
      user: req.user,
      summaries,
      layout: 'layout'
    });

  } catch (error) {
    console.error("❌ Помилка при отриманні зведень для ПУ:", error);
    res.status(500).send("Серверна помилка при відображенні зведень");
  }
};




exports.exportCourseSummaryToWord = async (req, res) => {
  const { summary_id } = req.params;

  try {
    const result = await pool.query(`
      SELECT id, summary_date, total_count, present_count, absent_count, reasons
      FROM course_summaries
      WHERE id = $1
    `, [summary_id]);

    if (result.rowCount === 0) {
      return res.status(404).json({ error: '❌ Зведення не знайдено' });
    }

    const summary = result.rows[0];

    const reasons = Array.isArray(summary.reasons)
      ? summary.reasons
      : JSON.parse(summary.reasons || '[]');

    const dateStr = new Date(summary.summary_date).toLocaleDateString("uk-UA");

    const tableRows = [
      new TableRow({
        children: [
          new TableCell({
            children: [new Paragraph({ children: [new TextRun({ text: "Дата", bold: true })] })],
            width: { size: 30, type: WidthType.PERCENTAGE },
          }),
          new TableCell({ children: [new Paragraph(dateStr)] }),
        ],
      }),
      new TableRow({
        children: [
          new TableCell({ children: [new Paragraph({ children: [new TextRun({ text: "Усього", bold: true })] })] }),
          new TableCell({ children: [new Paragraph(String(summary.total_count))] }),
        ],
      }),
      new TableRow({
        children: [
          new TableCell({ children: [new Paragraph({ children: [new TextRun({ text: "Присутні", bold: true })] })] }),
          new TableCell({ children: [new Paragraph(String(summary.present_count))] }),
        ],
      }),
      new TableRow({
        children: [
          new TableCell({ children: [new Paragraph({ children: [new TextRun({ text: "Відсутні", bold: true })] })] }),
          new TableCell({ children: [new Paragraph(String(summary.absent_count))] }),
        ],
      }),
    ];

    // Додаємо причини як окремі рядки
    reasons.forEach((r, index) => {
      tableRows.push(
        new TableRow({
          children: [
            new TableCell({
              children: [
                new Paragraph(index === 0
                  ? { children: [new TextRun({ text: "Причини", bold: true })] }
                  : new Paragraph("")),
              ],
            }),
            new TableCell({
              children: [new Paragraph(`${r.reason}: ${r.count}`)],
            }),
          ],
        })
      );
    });

    const doc = new Document({
      sections: [
        {
          children: [
            new Paragraph({
              children: [new TextRun({ text: "Зведення по курсу", bold: true, size: 28 })],
              spacing: { after: 200 },
            }),
            new Table({
              width: {
                size: 100,
                type: WidthType.PERCENTAGE,
              },
              rows: tableRows,
            }),
          ],
        },
      ],
    });

    const buffer = await Packer.toBuffer(doc);
    const fileName = `summary_course_${summary.id}.docx`;

    res.setHeader("Content-Disposition", `attachment; filename=${fileName}`);
    res.setHeader("Content-Type", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
    res.send(buffer);
  } catch (err) {
    console.error("❌ Помилка при експорті Word:", err);
    res.status(500).json({ error: "Помилка при генерації Word-файлу" });
  }
};







