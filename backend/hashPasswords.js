const bcrypt = require('bcrypt');
const pool = require('./config/database'); // Підключаємо базу

async function hashAndUpdatePasswords() {
    const users = [
        { login: 'admin', password: 'admin' }
    ];

    for (const user of users) {
        const hashedPassword = await bcrypt.hash(user.password, 10);
        await pool.query(
            'UPDATE users SET password_hash = $1 WHERE login = $2',
            [hashedPassword, user.login]
        );
        console.log(`🔹 Пароль для ${user.login} оновлено.`);
    }

    console.log('✅ Всі паролі хешовано.');
    process.exit(); // Завершуємо процес
}

hashAndUpdatePasswords().catch(err => {
    console.error('❌ Помилка хешування:', err);
    process.exit(1);
});
