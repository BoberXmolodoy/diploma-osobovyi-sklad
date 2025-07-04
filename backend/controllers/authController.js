const bcrypt = require('bcrypt');
const jwt = require('jsonwebtoken');
const pool = require('../config/database');

const SECRET_KEY = process.env.SECRET_KEY;
const REFRESH_SECRET = process.env.REFRESH_SECRET;

console.log('🔥 Сервіс авторизації запущено');


const generateTokens = (user) => {
  const payload = {
    id: user.id,
    name: user.name || '',
    role: user.role,
    login: user.login,
    rank: user.rank || '',
    group_id: user.group_id ?? null,
    group_number: user.group_number ?? null,
    course_id: user.course_id ?? null,
    faculty_id: user.faculty_id ?? null,
    department_id: user.department_id ?? null,
    location_id: user.location_id ?? null  // ✅ Додано
  };

  const accessToken = jwt.sign(payload, SECRET_KEY, { expiresIn: '30m' });
  const refreshToken = jwt.sign({ id: user.id }, REFRESH_SECRET, { expiresIn: '7d' });

  return { accessToken, refreshToken };
};


const login = async (req, res) => {
  try {
    const { login, password } = req.body;

    if (!login || !password) {
      return res.status(400).json({ error: '❌ Введіть логін і пароль' });
    }

    console.log("🟡 Починаємо логін користувача:", login);

    const result = await pool.query(
      `SELECT 
        u.id, u.name, u.role, u.login,
        COALESCE(u.rank, '') AS rank,
        u.group_id,
        g.group_number,
        u.course_id,
        c.faculty_id,
        u.department_id,
        u.location_id,
        u.password_hash
      FROM users u
      LEFT JOIN groups g ON u.group_id = g.id
      LEFT JOIN courses c ON u.course_id = c.id
      WHERE u.login = $1`,
      [login]
    );

    if (result.rows.length === 0) {
      return res.status(401).json({ error: 'Невірний логін або пароль' });
    }

    const user = result.rows[0];
    console.log("🧩 Знайдено користувача:", user);

    const isMatch = await bcrypt.compare(password, user.password_hash);
    if (!isMatch) {
      return res.status(401).json({ error: 'Невірний логін або пароль' });
    }

    const { accessToken, refreshToken } = generateTokens({
      ...user,
      department_id: user.department_id ?? null,
      location_id: user.location_id ?? null
    });

    await pool.query('UPDATE users SET refresh_token = $1 WHERE id = $2', [
      refreshToken,
      user.id,
    ]);

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
        location_id: user.location_id ?? null // ✅ додано у відповідь
      },
    });

  } catch (err) {
    console.error('❌ Помилка логіну:', err);
    res.status(500).json({ error: 'Помилка сервера' });
  }
};



const refresh = async (req, res) => {
  try {
    const { refreshToken } = req.body;

    if (!refreshToken) {
      return res.status(401).json({ error: '❌ RefreshToken відсутній' });
    }

    const result = await pool.query(
      `SELECT 
        u.id, u.name, u.role, u.login,
        COALESCE(u.rank, '') AS rank,
        u.group_id,
        g.group_number,
        u.course_id,
        c.faculty_id,
        u.department_id,
        u.location_id
      FROM users u
      LEFT JOIN groups g ON u.group_id = g.id
      LEFT JOIN courses c ON u.course_id = c.id
      WHERE u.refresh_token = $1`,
      [refreshToken]
    );

    if (result.rows.length === 0) {
      return res.status(403).json({ error: '❌ RefreshToken недійсний' });
    }

    const user = result.rows[0];
    console.log('✅ user object:', user);

    jwt.verify(refreshToken, REFRESH_SECRET, (err) => {
      if (err) {
        return res.status(403).json({ error: '❌ RefreshToken прострочений або недійсний' });
      }

      const newAccessToken = jwt.sign(
        {
          id: user.id,
          name: user.name || '',
          role: user.role,
          login: user.login,
          rank: user.rank || '',
          group_id: user.group_id ?? null,
          group_number: user.group_number ?? null,
          course_id: user.course_id ?? null,
          faculty_id: user.faculty_id ?? null,
          department_id: user.department_id ?? null,
          location_id: user.location_id ?? null // ✅ додано
        },
        SECRET_KEY,
        { expiresIn: '30m' }
      );

      res.json({ accessToken: newAccessToken });
    });

  } catch (err) {
    console.error('❌ Помилка оновлення токена:', err);
    res.status(500).json({ error: 'Помилка сервера' });
  }
};


const handleLogin = async (req, res) => {

  const { login, password } = req.body;

  try {
    const result = await pool.query(
      `SELECT id, login, role, password_hash FROM users WHERE login = $1`,
      [login]
    );

    if (result.rows.length === 0) {
      return res.render('login', { error: '❌ Користувача не знайдено' });
    }

    const user = result.rows[0];

    if (user.role !== 'pu') {
      return res.render('login', { error: '❌ Доступ лише для пункту управління' });
    }

    const isMatch = await bcrypt.compare(password, user.password_hash);
    if (!isMatch) {
      return res.render('login', { error: '❌ Невірний пароль' });
    }

    req.session.user = {
      id: user.id,
      login: user.login,
      role: user.role
    };

    return res.redirect('/'); // 🔁 Перекидає на головну сторінку ПУ

  } catch (err) {
    console.error('❌ Помилка логіну ПУ:', err);
    return res.render('login', { error: '🚫 Помилка сервера' });
  }
};



module.exports = { login, refresh, handleLogin };
