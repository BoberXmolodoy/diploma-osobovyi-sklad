const express = require('express');
const router = express.Router();
const pool = require('../config/database');

// 🔹 GET /api/locations
router.get('/', async (req, res) => {
  try {
    const result = await pool.query('SELECT id, name FROM locations ORDER BY id');
    res.json(result.rows);
  } catch (error) {
    console.error('❌ Помилка при отриманні локацій:', error);
    res.status(500).json({ error: 'Помилка сервера' });
  }
});

module.exports = router;
