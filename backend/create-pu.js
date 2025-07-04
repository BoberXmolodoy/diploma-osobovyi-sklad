const bcrypt = require('bcrypt');
const pool = require('./config/database');


async function createPUUser() {
  const login = 'pu';
  const password = 'pu';
  const hashed = await bcrypt.hash(password, 10);

  try {
    const result = await pool.query('SELECT id FROM users WHERE login = $1', [login]);

    if (result.rows.length > 0) {
      console.log('ℹ️ Користувач "pu" вже існує');
      process.exit(0);
    }

    await pool.query(`
      INSERT INTO users (name, login, password_hash, role, rank, is_active)
      VALUES ($1, $2, $3, $4, $5, true)
    `, [
      'Пункт управління',
      login,
      hashed,
      'pu',
      'черговий'
    ]);

    console.log('✅ Користувача "pu" створено');
    process.exit(0);
  } catch (err) {
    console.error('❌ Помилка створення користувача:', err);
    process.exit(1);
  }
}

createPUUser();
