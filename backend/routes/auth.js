const express = require('express');
const bcrypt = require('bcrypt');
const jwt = require('jsonwebtoken');
const pool = require('../config/database');
const { authMiddleware } = require('../middleware/authMiddleware');

const router = express.Router();
const SECRET_KEY = process.env.SECRET_KEY;
const REFRESH_SECRET = process.env.REFRESH_SECRET;

// 🔹 Генерація access та refresh токенів
const generateTokens = (user) => {
  const payload = {
    id: user.id,
    role: user.role,
    login: user.login,
    rank: user.rank || '',
    group_id: user.group_id ?? null,
    group_number: user.group_number ?? null,
    course_id: user.course_id ?? null,
    faculty_id: user.faculty_id ?? null,
    department_id: user.department_id ?? null,
    location_id: user.location_id ?? null // ✅ Додано
  };

  const accessToken = jwt.sign(payload, SECRET_KEY, { expiresIn: '30m' });
  const refreshToken = jwt.sign({ id: user.id }, REFRESH_SECRET, { expiresIn: '7d' });

  return { accessToken, refreshToken };
};



// 🔹 Логін
router.post('/login', async (req, res) => {
  console.log('📥 POST /api/auth/login');
  try {
    const { login, password } = req.body;

    if (!login || !password) {
      return res.status(400).json({ error: '❌ Введіть логін і пароль' });
    }

    const result = await pool.query(
      `SELECT 
         u.id, u.name, u.role, u.login,
         COALESCE(u.rank, '') AS rank,
         u.group_id,
         g.group_number,
         u.course_id,
         u.faculty_id,
         u.department_id,
         u.location_id,
         u.password_hash
       FROM users u
       LEFT JOIN groups g ON u.group_id = g.id
       WHERE u.login = $1`,
      [login]
    );

    if (result.rows.length === 0) {
      return res.status(401).json({ error: '❌ Невірний логін або пароль' });
    }

    const user = result.rows[0];
    const isMatch = await bcrypt.compare(password, user.password_hash);
    if (!isMatch) {
      return res.status(401).json({ error: '❌ Невірний логін або пароль' });
    }

    const { accessToken, refreshToken } = generateTokens(user);

    await pool.query(
      'UPDATE users SET refresh_token = $1 WHERE id = $2',
      [refreshToken, user.id]
    );

    // ✅ Якщо це Пункт управління — редирект на EJS-сторінку
    if (user.role === 'pu') {
      req.session.user = {
        id: user.id,
        login: user.login,
        role: user.role
      };
      return res.redirect('/pu');
    }

    // ✅ Інакше — API відповідь (для React, Android і т.д.)
    res.json({
      accessToken,
      refreshToken,
      user: {
        id: user.id,
        name: user.name,
        role: user.role,
        rank: user.rank,
        group_id: user.group_id ?? null,
        group_number: user.group_number ?? null,
        course_id: user.course_id ?? null,
        faculty_id: user.faculty_id ?? null,
        department_id: user.department_id ?? null,
        location_id: user.location_id ?? null
      }
    });

  } catch (err) {
    console.error('❌ Помилка логіну:', err);
    res.status(500).json({ error: '❌ Помилка сервера' });
  }
});



// 🔹 Refresh токена
router.post('/refresh', async (req, res) => {
  try {
    const { refreshToken } = req.body;
    if (!refreshToken) {
      return res.status(401).json({ error: "❌ RefreshToken відсутній" });
    }

    const result = await pool.query(
      `SELECT 
         u.id, u.name, u.role, u.login,
         COALESCE(u.rank, '') AS rank,
         u.group_id,
         g.group_number,
         u.course_id,
         u.faculty_id,
         u.department_id,
         u.location_id              -- ✅ ДОДАНО
       FROM users u
       LEFT JOIN groups g ON u.group_id = g.id
       WHERE u.refresh_token = $1`,
      [refreshToken]
    );

    if (result.rows.length === 0) {
      return res.status(403).json({ error: "❌ RefreshToken недійсний" });
    }

    const user = result.rows[0];

    jwt.verify(refreshToken, REFRESH_SECRET, (err) => {
      if (err) {
        return res.status(403).json({ error: "❌ RefreshToken прострочений або недійсний" });
      }

      const newAccessToken = jwt.sign({
        id: user.id,
        role: user.role,
        login: user.login,
        rank: user.rank,
        group_id: user.group_id ?? null,
        group_number: user.group_number ?? null,
        course_id: user.course_id ?? null,
        faculty_id: user.faculty_id ?? null,
        department_id: user.department_id ?? null,
        location_id: user.location_id ?? null       // ✅ ДОДАНО в payload
      }, SECRET_KEY, { expiresIn: '15m' });

      res.json({ accessToken: newAccessToken });
    });

  } catch (err) {
    console.error("❌ Помилка оновлення токена:", err);
    res.status(500).json({ error: "❌ Помилка сервера" });
  }
});



// 🔹 Logout
router.post('/logout', async (req, res) => {
  try {
    const { userId } = req.body;
    if (!userId) {
      return res.status(400).json({ error: "❌ Не вказано ID користувача" });
    }

    await pool.query('UPDATE users SET refresh_token = NULL WHERE id = $1', [userId]);
    res.json({ message: "✅ Вихід успішний" });

  } catch (err) {
    console.error("❌ Помилка виходу:", err);
    res.status(500).json({ error: "❌ Помилка сервера" });
  }
});

// 🔹 Protected маршрут
router.get('/protected', authMiddleware, (req, res) => {
  res.json({ message: '✅ Доступ дозволено', user: req.user });
});

module.exports = router;
