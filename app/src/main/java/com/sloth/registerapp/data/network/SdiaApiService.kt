package com.sloth.registerapp.data.network

import com.sloth.registerapp.data.model.LoginResponse
import com.sloth.registerapp.data.model.ServerMission
import com.sloth.registerapp.data.model.RegisterRequest
import com.sloth.registerapp.data.model.RegisterResponse
import com.sloth.registerapp.data.model.User
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface SdiaApiService {
    @POST("auth/login")
    suspend fun login(@Header("Authorization") authHeader: String): LoginResponse

    @POST("auth/register") // NOVO: Endpoint para registrar um novo usuário
    suspend fun register(@Body request: RegisterRequest): RegisterResponse

    @GET("auth/me")
    suspend fun getMe(@Header("Authorization") token: String): User

    @GET("missions")
    suspend fun getMissions(): List<ServerMission>
}
