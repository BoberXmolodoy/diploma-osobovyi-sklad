package com.example.diplom.data.network

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.example.diplom.data.TokenManager
import java.util.concurrent.TimeUnit
import android.util.Log

object RetrofitClient {  // ✅ `object` вже є синглтоном, метод getInstance() не потрібен
    private const val BASE_URL = "http://172.20.10.3:5000/"


    private var tokenManager: TokenManager? = null

    fun setTokenManager(manager: TokenManager) {
        tokenManager = manager
        createRetrofitInstance()  // ✅ Створюємо `Retrofit` лише після встановлення `TokenManager`
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val authInterceptor = Interceptor { chain ->
        val original: Request = chain.request()
        val requestBuilder = original.newBuilder()

        val token = tokenManager?.getAccessToken()
        if (!token.isNullOrEmpty()) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        } else {
            Log.e("RetrofitClient", "❌ TokenManager не містить токена! Авторизація не додана.")
        }

        val request = requestBuilder.build()
        chain.proceed(request)
    }

    private lateinit var retrofit: Retrofit  // ✅ Створюємо `Retrofit` після встановлення `TokenManager`

    private fun createRetrofitInstance() {
        retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(
                OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .writeTimeout(15, TimeUnit.SECONDS)
                    .addInterceptor(loggingInterceptor)
                    .addInterceptor(authInterceptor)
                    .build()
            )
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val apiService: ApiService by lazy {
        check(::retrofit.isInitialized) { "Retrofit не ініціалізовано! Викличте setTokenManager() перед використанням." }
        retrofit.create(ApiService::class.java)
    }
}
