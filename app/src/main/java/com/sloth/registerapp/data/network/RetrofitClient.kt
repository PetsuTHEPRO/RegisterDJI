package com.sloth.registerapp.data.network

import android.content.Context
import com.sloth.registerapp.data.repository.TokenRepository
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "http://192.168.2.46:5000/api/" // TODO: Replace with actual IP address
    //private const val BASE_URL = "http://10.1.8.115:5000/api/"
    private lateinit var apiService: SdiaApiService

    fun getInstance(context: Context): SdiaApiService {
        if (!::apiService.isInitialized) {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val authInterceptor = AuthInterceptor(TokenRepository(context))

            val httpClient = OkHttpClient.Builder()
                .addInterceptor(logging)
                .addInterceptor(authInterceptor)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(httpClient)
                .build()
            apiService = retrofit.create(SdiaApiService::class.java)
        }
        return apiService
    }
}

