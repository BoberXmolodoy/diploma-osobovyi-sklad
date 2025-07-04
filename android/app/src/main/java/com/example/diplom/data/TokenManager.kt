package com.example.diplom.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.util.concurrent.TimeUnit



class TokenManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    fun saveTokens(accessToken: String, refreshToken: String) {
        prefs.edit()
            .putString("access_token", accessToken)
            .putString("refresh_token", refreshToken)
            .apply()
        Log.d("TokenManager", "✅ Токени збережено")
    }

    fun getAccessToken(): String? {
        val token = prefs.getString("access_token", null) ?: return null.also {
            Log.e("TokenManager", "❌ AccessToken відсутній у SharedPreferences")
        }

        return if (isTokenExpired(token)) {
            Log.e("TokenManager", "❌ AccessToken прострочений, потрібне оновлення")
            null
        } else token
    }

    fun isAccessTokenAvailable(): Boolean = getAccessToken() != null

    fun getRefreshToken(): String? = prefs.getString("refresh_token", null)

    suspend fun clearTokens() {
        withContext(Dispatchers.IO) {
            Log.w("TokenManager", "⚠️ Видалення токенів!")
            prefs.edit().remove("access_token").remove("refresh_token").apply()
        }
    }

    fun getAuthHeader(): String? = getAccessToken()?.let { "Bearer $it" }

    fun getUserRole(): String? {
        val token = getAccessToken() ?: return null
        return try {
            decodeToken(token)?.optString("role", null)
        } catch (e: Exception) {
            Log.e("TokenManager", "❌ Помилка отримання ролі користувача", e)
            null
        }
    }


    fun getUserId(): Int? {
        val token = getAccessToken() ?: return null
        return try {
            decodeToken(token)?.let { json ->
                Log.d("TokenManager", "🎯 JSON токена: $json")
                json.optInt("id", -1).takeIf { it != -1 }
            }
        } catch (e: Exception) {
            Log.e("TokenManager", "❌ Помилка отримання id з токена", e)
            null
        }
    }


    fun getGroupId(): Int? {
        val token = getAccessToken() ?: return null
        return try {
            decodeToken(token)?.optInt("group_id", -1)?.takeIf { it != -1 }
        } catch (e: Exception) {
            Log.e("TokenManager", "❌ Помилка отримання group_id з токена", e)
            null
        }
    }

    fun getGroupNumber(): Int? {
        val token = getAccessToken() ?: return null
        return try {
            decodeToken(token)?.let { json ->
                Log.d("TokenManager", "🧩 Payload токена: $json")
                val number = when {
                    json.has("groupNumber") -> json.optInt("groupNumber", -1)
                    json.has("group_number") -> json.optInt("group_number", -1)
                    else -> -1
                }
                number.takeIf { it != -1 }
            }
        } catch (e: Exception) {
            Log.e("TokenManager", "❌ Помилка отримання номера групи", e)
            null
        }
    }


    private fun isTokenExpired(token: String): Boolean {
        return try {
            val exp = decodeToken(token)
                ?.optLong("exp", 0)
                ?.times(1000) ?: return true // якщо токен не декодувався — вважаємо простроченим

            System.currentTimeMillis() >= exp
        } catch (e: Exception) {
            Log.e("TokenManager", "❌ Помилка перевірки токена", e)
            true
        }
    }

    fun getUserName(): String? {
        val token = getAccessToken() ?: return null
        return try {
            decodeToken(token)?.optString("login", null)
        } catch (e: Exception) {
            Log.e("TokenManager", "❌ Помилка отримання імені користувача", e)
            null
        }
    }


    fun saveGroupInfo(groupId: Int?, groupNumber: String?) {
        prefs.edit().apply {
            groupId?.let { putInt("group_id", it) }
            groupNumber?.let { putString("group_number", it) }
            apply()
        }
        Log.d("TokenManager", "✅ Збережено groupId: $groupId, groupNumber: $groupNumber")
    }


    private fun decodeToken(token: String): JSONObject? {
        return try {
            val parts = token.split(".")
            if (parts.size < 2) return null

            var payload = parts[1]

            // 🧩 Додаємо padding, якщо не вистачає
            val rem = payload.length % 4
            if (rem > 0) {
                payload += "=".repeat(4 - rem)
            }

            // ✅ Використовуємо URL_SAFE для JWT
            val decodedBytes = Base64.decode(payload, Base64.URL_SAFE)
            val json = String(decodedBytes, Charsets.UTF_8)

            Log.d("TokenManager", "🎯 JSON токена (декодований): $json")
            JSONObject(json)
        } catch (e: Exception) {
            Log.e("TokenManager", "❌ Не вдалося декодувати токен", e)
            null
        }
    }


    fun getCourseId(): Int? {
        val token = getAccessToken() ?: return null
        return try {
            decodeToken(token)?.optInt("course_id", -1)?.takeIf { it != -1 }
        } catch (e: Exception) {
            Log.e("TokenManager", "❌ Помилка отримання course_id з токена", e)
            null
        }
    }


    fun getFacultyId(): Int? {
        val token = getAccessToken() ?: return null
        return try {
            decodeToken(token)?.optInt("faculty_id", -1)?.takeIf { it != -1 }
        } catch (e: Exception) {
            Log.e("TokenManager", "❌ Помилка отримання faculty_id", e)
            null
        }
    }


    fun getDepartmentId(): Int? {
        val token = getAccessToken() ?: return null
        return try {
            decodeToken(token)?.optInt("department_id", -1)?.takeIf { it != -1 }
        } catch (e: Exception) {
            Log.e("TokenManager", "❌ Помилка отримання department_id", e)
            null
        }
    }
    fun getLocationId(): Int? {
        val token = getAccessToken() ?: return null
        return try {
            decodeToken(token)?.optInt("location_id", -1)?.takeIf { it != -1 }
        } catch (e: Exception) {
            Log.e("TokenManager", "❌ Помилка отримання location_id", e)
            null
        }
    }




    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun requestNewAccessToken(): Boolean {
        val refreshToken = getRefreshToken() ?: return false

        return withContext(Dispatchers.IO) {
            val jsonBody = JSONObject().apply { put("refreshToken", refreshToken) }
            val requestBody = RequestBody.create("application/json".toMediaTypeOrNull(), jsonBody.toString())

            val request = Request.Builder()
                .url("http://10.0.2.2:5000/api/auth/refresh")
                .post(requestBody)
                .header("Content-Type", "application/json")
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val jsonResponse = JSONObject(response.body?.string() ?: "")
                        val newAccessToken = jsonResponse.optString("accessToken", "")

                        if (newAccessToken.isNotEmpty()) {
                            saveTokens(newAccessToken, refreshToken)
                            return@withContext true
                        }
                    } else if (response.code == 401) {
                        Log.e("TokenManager", "❌ RefreshToken недійсний, видаляємо")
                        clearTokens()
                    }
                }
                false
            } catch (e: Exception) {
                Log.e("TokenManager", "❌ Помилка оновлення токена", e)
                false
            }
        }
    }
}
