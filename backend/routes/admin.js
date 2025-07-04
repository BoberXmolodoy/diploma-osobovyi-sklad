const express = require('express');
const router = express.Router();
const pool = require('../config/database'); // Підключення до PostgreSQL

// 🔹 Оновлення ролі користувача (адміністратор може змінювати ролі)
router.put('/users/:id/role', async (req, res) => {
    const { id } = req.params;
    const { role } = req.body;

    if (!role) {
        return res.status(400).json({ error: "Роль є обов’язковою" });
    }

    try {
        const updatedUser = await pool.query(
            'UPDATE users SET role = $1 WHERE id = $2 RETURNING id, name, role, login, is_active',
            [role, id]
        );

        if (updatedUser.rowCount === 0) {
            return res.status(404).json({ error: "Користувача не знайдено" });
        }

        console.log("✅ Роль змінена:", updatedUser.rows[0]);
        res.json(updatedUser.rows[0]);
    } catch (err) {
        console.error("❌ ПОМИЛКА зміни ролі:", err);
        res.status(500).json({ error: "Помилка сервера" });
    }
});

// 🔹 Активація/деактивація користувача
router.put('/users/:id/status', async (req, res) => {
    const { id } = req.params;
    const { is_active } = req.body;

    if (typeof is_active !== "boolean") {
        return res.status(400).json({ error: "is_active має бути true або false" });
    }

    try {
        const updatedUser = await pool.query(
            'UPDATE users SET is_active = $1 WHERE id = $2 RETURNING id, name, role, login, is_active',
            [is_active, id]
        );

        if (updatedUser.rowCount === 0) {
            return res.status(404).json({ error: "Користувача не знайдено" });
        }

        console.log("✅ Статус оновлено:", updatedUser.rows[0]);
        res.json(updatedUser.rows[0]);
    } catch (err) {
        console.error("❌ ПОМИЛКА оновлення статусу:", err);
        res.status(500).json({ error: "Помилка сервера" });
    }
});

module.exports = router;
