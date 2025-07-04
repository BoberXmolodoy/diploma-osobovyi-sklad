const pool = require('../config/database');
const bcrypt = require('bcrypt');

const roleMap = {
  "начальник_курсу": "nk",
  "начальник_факультету": "nf",
  "командир_групи": "kg",
   "начальник_кафедри": "nkf",   // 🆕 додали це
  "черговий_локації": "cl",
};

/// 🔧 Створення користувача
exports.createUser = async (req, res) => {
  try {
    const {
      name,
      login,
      password,
      role,
      rank,
      groupNumber,
      parent_id,
      course_id,
      faculty_id,
      department_id,
      location_id // 🆕
    } = req.body;

    const errors = [];

    if (!name || name.length < 2 || name.length > 15)
      errors.push("Ім’я має бути від 2 до 30 символів");
    if (!login || login.length < 3 || login.length > 15)
      errors.push("Логін має бути від 3 до 20 символів");
    if (!password || password.length < 6 || password.length > 15)
      errors.push("Пароль має бути від 6 до 50 символів");
    if (!role || role.length < 2 || role.length > 15)
      errors.push("Роль має бути від 2 до 20 символів");
    if (!rank || rank.length < 2 || rank.length > 15)
      errors.push("Звання має бути від 2 до 50 символів");

    const dbRole = roleMap[role] || role;

    let groupId = null;
    let finalCourseId = course_id;
    let finalFacultyId = faculty_id || null;
    let finalLocationId = location_id || null;

   // КГ — створюємо або використовуємо групу
if (dbRole === 'kg') {
  if (!groupNumber) errors.push("Потрібно вказати номер групи");
  if (!course_id) errors.push("Потрібно вказати курс");

  if (errors.length === 0) {
    const groupRes = await pool.query(
      `SELECT id, commander_user_id FROM groups WHERE group_number = $1`,
      [groupNumber]
    );

    if (groupRes.rows.length > 0) {
      const existingGroup = groupRes.rows[0];

      if (existingGroup.commander_user_id) {
        // 🛑 Командир вже є — не дозволяємо
        errors.push("Група вже має призначеного командира");
      } else {
        // ✅ Командира нема — використовуємо цю групу
        groupId = existingGroup.id;
      }
    } else {
      // 🔧 Створюємо нову групу
      const locationId = parseInt(groupNumber[0]);
      const newGroup = await pool.query(
        `INSERT INTO groups (group_number, course_id, location_id)
         VALUES ($1, $2, $3) RETURNING id`,
        [groupNumber, course_id, locationId]
      );
      groupId = newGroup.rows[0].id;
    }
  }
}


    // НКФ — начальник кафедри
    if (dbRole === 'nkf') {
      if (!parent_id) errors.push("Потрібно вказати начальника факультету (parent_id)");
      if (!department_id) errors.push("Потрібно вказати кафедру (department_id)");

      if (errors.length === 0) {
        const parentRes = await pool.query(
          `SELECT faculty_id FROM users WHERE id = $1`,
          [parent_id]
        );

        if (parentRes.rows.length === 0 || !parentRes.rows[0].faculty_id) {
          errors.push("Не вдалося отримати faculty_id з НФ");
        } else {
          finalFacultyId = parentRes.rows[0].faculty_id;
        }
      }
    }

    // НК — створюємо курс
    if (dbRole === 'nk') {
      if (!course_id) errors.push("Потрібно вказати номер курсу");
      if (!parent_id) errors.push("Потрібно вказати керівника (parent_id)");

      if (errors.length === 0) {
        const nfRes = await pool.query(
          `SELECT faculty_id FROM users WHERE id = $1`,
          [parent_id]
        );

        if (nfRes.rows.length === 0 || !nfRes.rows[0].faculty_id) {
          errors.push("Не вдалося отримати faculty_id з начальника факультету");
        } else {
          finalFacultyId = nfRes.rows[0].faculty_id;

          const courseRes = await pool.query(
            `SELECT id FROM courses WHERE number = $1`,
            [course_id]
          );

          if (courseRes.rows.length === 0) {
            const newCourse = await pool.query(
              `INSERT INTO courses (number, faculty_id) VALUES ($1, $2) RETURNING id`,
              [course_id, finalFacultyId]
            );
            finalCourseId = newCourse.rows[0].id;
          } else {
            finalCourseId = courseRes.rows[0].id;
          }
        }
      }
    }

    // CL — черговий локації
    if (dbRole === 'cl') {
      if (!location_id) errors.push("Потрібно вказати локацію (location_id)");
      finalFacultyId = null; // ❗ обов'язково
    }

    if ((dbRole === 'nk' || dbRole === 'kg') && !parent_id)
      errors.push("Потрібно вказати керівника (parent_id)");

    if (errors.length > 0)
      return res.status(400).json({ error: errors.join(', ') });

    const existing = await pool.query(
      `SELECT id FROM users WHERE login = $1`,
      [login]
    );
    if (existing.rows.length > 0) {
      return res.status(409).json({ error: 'Користувач з таким логіном вже існує.' });
    }

    const passwordHash = await bcrypt.hash(password, 10);

    const insertResult = await pool.query(
      `INSERT INTO users 
        (name, login, password_hash, role, rank, is_active, created_at, group_id, parent_id, course_id, faculty_id, department_id, location_id)
       VALUES ($1, $2, $3, $4, $5, true, NOW(), $6, $7, $8, $9, $10, $11)
       RETURNING id`,
      [
        name,
        login,
        passwordHash,
        dbRole,
        rank,
        groupId,
        parent_id,
        finalCourseId,
        finalFacultyId,
        department_id,
        finalLocationId // 🆕
      ]
    );
const newUserId = insertResult.rows[0].id;

// 🧷 Якщо це КГ — оновлюємо групу: очищуємо старого командира і ставимо нового
if (dbRole === 'kg' && groupId) {
  // 🔧 Очистити group_id старого командира (якщо є)
  await pool.query(
    `UPDATE users SET group_id = NULL 
     WHERE id = (
       SELECT commander_user_id FROM groups WHERE id = $1
     ) AND id IS NOT NULL`,
    [groupId]
  );

  // 🔧 Встановити нового командира у групу
  await pool.query(
    `UPDATE groups SET commander_user_id = $1 WHERE id = $2`,
    [newUserId, groupId]
  );

  // 🔧 Записати group_id новому користувачу (на всяк випадок, хоча вже має бути)
  await pool.query(
    `UPDATE users SET group_id = $1 WHERE id = $2`,
    [groupId, newUserId]
  );
}


    let path = '';
    if (dbRole === 'nf') {
      path = `${newUserId}`;
    } else if (dbRole === 'nk' || dbRole === 'kg' || dbRole === 'nkf') {
      const parentPathRes = await pool.query(
        `SELECT path FROM users WHERE id = $1`,
        [parent_id]
      );
      if (parentPathRes.rows.length === 0) {
        return res.status(400).json({ error: 'Керівника з таким ID не знайдено' });
      }
      path = `${parentPathRes.rows[0].path}.${newUserId}`;
    }

    await pool.query(`UPDATE users SET path = $1 WHERE id = $2`, [path, newUserId]);

    res.status(201).json({
      message: '✅ Користувача створено успішно',
      user: {
        id: newUserId,
        name,
        login,
        role: dbRole,
        rank,
        groupId,
        course_id: finalCourseId,
        faculty_id: finalFacultyId,
        department_id,
        location_id: finalLocationId,
        path
      }
    });

  } catch (err) {
    console.error('❌ Помилка при створенні користувача:', err);
    res.status(500).json({ error: '❌ Внутрішня помилка сервера' });
  }
};



// ✅ updateUser без змін — вже підтримує groupNumber → group_id
exports.updateUser = async (req, res) => {
  const { id } = req.params;
  const {
    name,
    login,
    password,
    role,
    rank,
    is_active,
    groupNumber,
    parent_id,
    course_id // ✅ додано
  } = req.body;

  const errors = [];

  if (!name || name.length < 2 || name.length > 30)
    errors.push("Ім’я має бути від 2 до 30 символів");
  if (!login || login.length < 3 || login.length > 20)
    errors.push("Логін має бути від 3 до 20 символів");
  if (password && (password.length < 6 || password.length > 50))
    errors.push("Пароль має бути від 6 до 50 символів");
  if (!role || role.length < 2 || role.length > 20)
    errors.push("Роль має бути від 2 до 20 символів");
  if (!rank || rank.length < 2 || rank.length > 50)
    errors.push("Звання має бути від 2 до 50 символів");

  const dbRole = roleMap[role] || role;

  let groupId = null;
 // КГ — створюємо групу
if (dbRole === 'kg') {
  if (!groupNumber) errors.push("Потрібно вказати номер групи");
  if (!course_id) errors.push("Потрібно вказати курс");

  if (errors.length === 0) {
    const groupRes = await pool.query(
      `SELECT id FROM groups WHERE group_number = $1`,
      [groupNumber]
    );

    if (groupRes.rows.length > 0) {
      groupId = groupRes.rows[0].id;
    } else {
      const locationId = parseInt(groupNumber[0]);
      const newGroup = await pool.query(
        `INSERT INTO groups (group_number, course_id, location_id)
         VALUES ($1, $2, $3) RETURNING id`,
        [groupNumber, course_id, locationId]
      );
      groupId = newGroup.rows[0].id;
    }
  }
}


  if ((dbRole === 'nk' || dbRole === 'kg') && !parent_id) {
    errors.push("Потрібно вказати керівника (parent_id)");
  }

  if (dbRole === 'nk' && !course_id) {
    errors.push("Потрібно вказати курс для начальника курсу");
  }

  if (errors.length > 0) {
    return res.status(400).json({ error: errors.join(', ') });
  }

  try {
    const userCheck = await pool.query('SELECT * FROM users WHERE id = $1', [id]);
    if (userCheck.rows.length === 0) {
      return res.status(404).json({ error: 'Користувача не знайдено' });
    }

    const loginCheck = await pool.query('SELECT id FROM users WHERE login = $1 AND id != $2', [login, id]);
    if (loginCheck.rows.length > 0) {
      return res.status(409).json({ error: 'Логін вже використовується іншим користувачем.' });
    }

    let passwordHash = userCheck.rows[0].password_hash;
    if (password) {
      passwordHash = await bcrypt.hash(password, 10);
    }

    await pool.query(
      `UPDATE users SET
        name = $1,
        login = $2,
        password_hash = $3,
        role = $4,
        rank = $5,
        is_active = $6,
        group_id = $7,
        parent_id = $8,
        course_id = $9
      WHERE id = $10`,
      [name, login, passwordHash, dbRole, rank, is_active, groupId, parent_id, course_id, id]
    );

    let path = '';
    if (dbRole === 'nf') {
      path = `${id}`;
    } else if (dbRole === 'nk' || dbRole === 'kg') {
      const parentPathRes = await pool.query('SELECT path FROM users WHERE id = $1', [parent_id]);
      if (parentPathRes.rows.length === 0) {
        return res.status(400).json({ error: 'Керівника з таким ID не знайдено' });
      }
      path = `${parentPathRes.rows[0].path}.${id}`;
    }

    await pool.query('UPDATE users SET path = $1 WHERE id = $2', [path, id]);

    res.json({ message: 'Користувача оновлено' });
  } catch (err) {
    console.error('❌ Помилка при оновленні користувача:', err);
    res.status(500).json({ error: 'Внутрішня помилка сервера' });
  }
};


exports.deleteUser = async (req, res) => {
  const { id } = req.params;

  try {
    const subordinates = await pool.query('SELECT id FROM users WHERE parent_id = $1', [id]);
    if (subordinates.rows.length > 0) {
      return res.status(400).json({ error: 'Неможливо деактивувати. Є підлеглі користувачі.' });
    }

    // 📍 Якщо користувач був командиром групи — скидаємо commander_user_id
    await pool.query(`
      UPDATE groups
      SET commander_user_id = NULL
      WHERE commander_user_id = $1
    `, [id]);

    // 🟡 Soft delete користувача
    const result = await pool.query(
      'UPDATE users SET is_active = false WHERE id = $1 RETURNING id',
      [id]
    );

    if (result.rowCount === 0) {
      return res.status(404).json({ error: 'Користувача не знайдено' });
    }

    res.json({ message: 'Користувача деактивовано (soft delete)' });
  } catch (err) {
    console.error('❌ Помилка при деактивації користувача:', err);
    res.status(500).json({ error: 'Внутрішня помилка сервера' });
  }
};



// 🆕 Отримання номера групи за groupId
exports.getGroupNumberById = async (req, res) => {
  const { groupId } = req.params;

  try {
    const result = await pool.query(
      'SELECT group_number FROM groups WHERE id = $1',
      [groupId]
    );

    if (result.rows.length === 0) {
      return res.status(404).json({ error: 'Групу не знайдено' });
    }

    res.json({ number: result.rows[0].group_number });
  } catch (err) {
    console.error('❌ Помилка при отриманні номера групи:', err);
    res.status(500).json({ error: 'Помилка сервера' });
  }
};


exports.getUserById = async (req, res) => {
  const userId = req.params.id;

  try {
      const result = await pool.query(`
          SELECT id, name, login, role, rank, is_active, group_id, parent_id, course_id
          FROM users
         WHERE id = $1 AND is_active = true
      `, [userId]);

      if (result.rows.length === 0) {
          return res.status(404).json({ error: "Користувача не знайдено" });
      }

      res.json(result.rows[0]);
  } catch (err) {
      console.error("❌ ПОМИЛКА отримання користувача:", err);
      res.status(500).json({ error: "Помилка сервера" });
  }
};
