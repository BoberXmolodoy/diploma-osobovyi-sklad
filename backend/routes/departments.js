const express = require('express');
const router = express.Router();
const pool = require('../config/database');

// GET /api/departments
router.get('/', async (req, res) => {
  try {
    const result = await pool.query(`SELECT id, name FROM departments ORDER BY id`);
    res.json(result.rows);
  } catch (error) {
    console.error('❌ Помилка при отриманні кафедр:', error);
    res.status(500).json({ error: 'Помилка сервера при отриманні кафедр' });
  }
});

module.exports = router;
