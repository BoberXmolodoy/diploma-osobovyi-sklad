require('dotenv').config();
const express = require('express');
const cors = require('cors');
const path = require('path');
const session = require('express-session');
const expressLayouts = require('express-ejs-layouts'); // ✅ новий модуль

const pool = require('./config/database');
const facultiesRoutes = require('./routes/faculties');

const app = express();
app.use(cors());
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// ✅ Налаштування EJS + Layout
app.set('view engine', 'ejs');
app.set('views', path.join(__dirname, 'views'));
app.use(expressLayouts);                    // 🔹 підключити layouts
app.set('layout', 'layout');               // 🔹 встановити шаблон за замовчуванням
app.use('/css', express.static(path.join(__dirname, 'css')));

app.use(session({
  secret: process.env.SESSION_SECRET || 'secret',
  resave: false,
  saveUninitialized: false,
}));

// 🧪 Перевірка .env
const SECRET_KEY = process.env.SECRET_KEY;
const REFRESH_SECRET = process.env.REFRESH_SECRET;
const PORT = process.env.PORT || 5000;

if (!SECRET_KEY || !REFRESH_SECRET) {
  console.error("❌ SECRET_KEY або REFRESH_SECRET не встановлені у .env");
  process.exit(1);
}

// 🔌 Перевірка з'єднання з БД
pool.query('SELECT 1', (err) => {
  if (err) {
    console.error("❌ Немає з'єднання з БД", err);
    process.exit(1);
  } else {
    console.log("✅ Підключення до БД успішне");
  }
});

// 🔗 API маршрути
const authRoutes = require('./routes/auth');
const adminRoutes = require('./routes/admin');
const userRoutes = require('./routes/users');
const attendanceRoutes = require('./routes/attendance');
const departmentsRoutes = require('./routes/departments');
const locationsRoutes = require('./routes/locations');

app.use('/api/auth', authRoutes);
app.use('/api/admin', adminRoutes);
app.use('/api/users', userRoutes);
app.use('/api/attendance', attendanceRoutes);
app.use('/api/faculties', facultiesRoutes);
app.use('/api/departments', departmentsRoutes);
app.use('/api/locations', locationsRoutes);

// 🌐 WEB-маршрути для ПУ, логіну тощо
const webRoutes = require('./routes/web');
app.use('/', webRoutes);

// ❌ 404
app.use((req, res) => {
  res.status(404).json({ error: "Маршрут не знайдено" });
});

// 🚀 Старт сервера
app.listen(PORT, '0.0.0.0', () => {
  console.log(`✅ Сервер запущено на порту ${PORT}`);
});
