# 📦 Diplom Android + Node.js (Особовий склад)

Цей проєкт — повноцінна система обліку особового складу, що складається з:
- 📱 **Android-клієнта** (Kotlin + Jetpack Compose)
- 🖥️ **Node.js backend** (Express + MySQL)
- 🧠 **Авторизації, токенів, ролей, запитів та звітів**

## ⚙️ Технології
- Android: `Kotlin`, `Jetpack Compose`, `MVVM`, `Retrofit`, `Room`, `LiveData`
- Backend: `Node.js`, `Express`, `JWT`, `bcrypt`, `MySQL`, `EJS`

## 📲 Функціонал Android-додатку:
- Авторизація користувачів (з ролями)
- Надсилання запитів
- Перегляд особових даних
- Генерація та перегляд звітів

## 🌐 Backend API:
- Захищене REST API з JWT
- Обробка користувачів, департаментів, звітів
- UI для авторизації та перегляду даних (EJS)

## 🏁 Як запустити
1. **Backend:**
   ```bash
   cd backend
   npm install
   npm run dev
