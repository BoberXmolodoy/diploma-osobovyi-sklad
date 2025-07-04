const express = require('express');
const router = express.Router();
const pool = require('../config/database');
const userController = require('../controllers/userController');
const { authMiddleware, authorizeRole } = require('../middleware/authMiddleware');

const roleMap = {
    "начальник_курсу": "nk",
    "начальник_факультету": "nf",
    "командир_групи": "kg",
    "командир_відділення": "kv",
    "черговий_по_факультету": "cf",
    "черговий_локації": "cl"
};

// 🆕 Отримання номера групи за groupId (має бути вище за /:id)
router.get('/groups/:groupId/number', authMiddleware, userController.getGroupNumberById);

// 🔍 Отримання groupId по номеру групи
router.get('/groups/number/:number', authMiddleware, async (req, res) => {
    const { number } = req.params;

    try {
        const result = await pool.query('SELECT id FROM groups WHERE group_number = $1', [number]);

        if (result.rows.length === 0) {
            return res.status(404).json({ error: 'Групу не знайдено' });
        }

        res.json({ groupId: result.rows[0].id });
    } catch (err) {
        console.error('❌ ПОМИЛКА при пошуку groupId:', err);
        res.status(500).json({ error: 'Помилка сервера' });
    }
});

// 🔹 ВСІ активні користувачі
router.get('/', authMiddleware, async (req, res) => {
    try {
        const result = await pool.query(
            'SELECT id, name, role, rank, is_active FROM users WHERE is_active = true ORDER BY id'
        );
        res.json(result.rows);
    } catch (err) {
        console.error("❌ ПОМИЛКА отримання користувачів:", err);
        res.status(500).json({ error: "Помилка сервера" });
    }
});

// 🔹 ВСІ неактивні користувачі (soft-deleted)
router.get('/inactive', authMiddleware, async (req, res) => {
    try {
        const result = await pool.query(
            'SELECT id, name, role, rank, is_active FROM users WHERE is_active = false ORDER BY id'
        );
        res.json(result.rows);
    } catch (err) {
        console.error("❌ ПОМИЛКА отримання деактивованих користувачів:", err);
        res.status(500).json({ error: "Помилка сервера" });
    }
});

// 🔹 Отримання користувачів за роллю (лише активні)
router.get('/role/:role', authMiddleware, async (req, res) => {
    let { role } = req.params;
    role = decodeURIComponent(role);
    const dbRole = roleMap[role] || role;

    try {
        const result = await pool.query(
            'SELECT id, name, role, rank FROM users WHERE role = $1 AND is_active = true',
            [dbRole]
        );

        if (result.rows.length === 0) {
            return res.status(404).json({ error: "Користувачів не знайдено" });
        }

        res.json(result.rows);
    } catch (err) {
        console.error("❌ ПОМИЛКА отримання користувачів:", err);
        res.status(500).json({ error: "Помилка сервера" });
    }
});

// ➕ Створення користувача
router.post('/create', authMiddleware, authorizeRole(['admin']), userController.createUser);

// 🔄 Оновлення користувача
router.put('/:id', authMiddleware, authorizeRole(['admin']), (req, res, next) => {
    console.log("📥 UPDATE BODY:", req.body);
    next();
}, userController.updateUser);

// ❌ М’яке видалення користувача
router.delete('/:id', authMiddleware, authorizeRole(['admin']), userController.deleteUser);

// 🔹 Отримання одного користувача (без перевірки is_active)
router.get('/:id', userController.getUserById);

module.exports = router;
