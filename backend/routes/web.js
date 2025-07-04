const express = require('express');
const router = express.Router();
const authController = require('../controllers/authController');
const requirePUAuth = require('../middleware/requirePUAuth');

// 🔐 Сторінка логіну
router.get('/login', (req, res) => {
  res.render('login', { error: null, layout: false });
});

router.post('/login', authController.handleLogin);

// 🚪 Вихід
router.get('/logout', (req, res) => {
  req.session.destroy(() => {
    res.redirect('/login');
  });
});

// 🧭 Головна сторінка ПУ
router.get('/', requirePUAuth, async (req, res) => {
  try {
    const pool = require('../config/database');
    const result = await pool.query(`
      SELECT cs.id, cs.summary_date, cs.total_count, cs.present_count, cs.absent_count,
             cs.reasons, f.name AS faculty_name, l.name AS location_name
      FROM course_summaries cs
      LEFT JOIN faculties f ON cs.faculty_id = f.id
      LEFT JOIN locations l ON cs.location_id = l.id
      ORDER BY cs.summary_date DESC, cs.created_at DESC
    `);

    const summaries = result.rows.map(row => ({
      ...row,
      summary_date: row.summary_date, // ✅ залишаємо як є, без UTC-зсувів
      reasons: Array.isArray(row.reasons)
        ? row.reasons
        : JSON.parse(row.reasons || '[]')
    }));

    res.render('pu_home', {
      user: req.session.user,
      summaries,
      extraStyles: ['/css/pu_home.css']
    });
  } catch (err) {
    console.error("❌ Помилка при завантаженні сторінки ПУ:", err);
    res.status(500).send("Помилка сервера");
  }
});

module.exports = router;
