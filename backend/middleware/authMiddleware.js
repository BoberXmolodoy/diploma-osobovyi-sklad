const jwt = require('jsonwebtoken');
require('dotenv').config();

const SECRET_KEY = process.env.SECRET_KEY;

const authMiddleware = (req, res, next) => {
  const authHeader = req.headers.authorization;
  
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return res.status(403).json({ error: '❌ Доступ заборонено. Немає токена' });
  }

  const token = authHeader.split(' ')[1];

  try {
    const decoded = jwt.verify(token, SECRET_KEY);
    req.user = decoded;
    next(); // Передаємо управління далі
  } catch (err) {
    console.error("❌ Помилка перевірки токена:", err);
    return res.status(401).json({ error: '❌ Токен недійсний або прострочений' });
  }
};

// Middleware для перевірки ролей користувачів
const authorizeRole = (roles) => {
  return (req, res, next) => {
    if (!roles.includes(req.user.role)) {
      return res.status(403).json({ error: '❌ Недостатньо прав' });
    }
    next();
  };
};

module.exports = { authMiddleware, authorizeRole };
