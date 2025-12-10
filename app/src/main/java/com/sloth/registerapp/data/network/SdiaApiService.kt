package com.sloth.registerapp.data.network

import com.sloth.registerapp.data.model.LoginRequest
import com.sloth.registerapp.data.model.LoginResponse
import com.sloth.registerapp.data.model.Mission
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface SdiaApiService {
    @POST("login") // Endpoint relativo
    suspend fun login(@Body request: LoginRequest): LoginResponse

    // Exemplo para buscar missões
    @GET("missions")
    suspend fun getMissions(@Header("Authorization") token: String): List<Mission>
}
