const { Pool } = require('pg');

const pool = new Pool({
  user: 'postgres',
  host: 'localhost',
  database: 'attendance_db',
  password: 'root', // Вкажи свій пароль від PostgreSQL
  port: 5432, // Стандартний порт PostgreSQL
});

module.exports = {
  query: (text, params) => pool.query(text, params),

  // Зберігає refresh-токен в базі
  saveRefreshToken: async (userId, refreshToken) => {
    await pool.query(
      'UPDATE users SET refresh_token = $1 WHERE id = $2',
      [refreshToken, userId]
    );
  },

  // Отримує refresh-токен з бази
  getRefreshToken: async (userId) => {
    const result = await pool.query(
      'SELECT refresh_token FROM users WHERE id = $1',
      [userId]
    );
    return result.rows[0]?.refresh_token;
  },

  // Видаляє refresh-токен (наприклад, при виході користувача)
  clearRefreshToken: async (userId) => {
    await pool.query(
      'UPDATE users SET refresh_token = NULL WHERE id = $1',
      [userId]
    );
  }
};
