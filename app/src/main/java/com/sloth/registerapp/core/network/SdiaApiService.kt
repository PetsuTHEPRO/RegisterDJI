package com.sloth.registerapp.core.network

import com.sloth.registerapp.features.auth.data.remote.dto.LoginRequestDto
import com.sloth.registerapp.core.network.dto.AuthTokensDto
import com.sloth.registerapp.core.network.dto.RefreshTokenRequestDto
import com.sloth.registerapp.features.auth.data.remote.dto.RegisterRequestDto
import com.sloth.registerapp.features.auth.data.remote.dto.RegisterResponseDto
import com.sloth.registerapp.features.auth.domain.model.User
import com.sloth.registerapp.features.mission.data.remote.dto.ServerMissionDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface SdiaApiService {
    @POST("auth/login")
    suspend fun login(@Header("Authorization") authHeader: String): AuthTokensDto

    @POST("auth/login")
    suspend fun loginWithBody(@Body request: LoginRequestDto): AuthTokensDto

    @POST("auth/register") // NOVO: Endpoint para registrar um novo usuário
    suspend fun register(@Body request: RegisterRequestDto): RegisterResponseDto

    @POST("auth/refresh")
    suspend fun refresh(@Header("Authorization") authHeader: String): AuthTokensDto

    @POST("auth/refresh")
    suspend fun refreshWithBody(@Body request: RefreshTokenRequestDto): AuthTokensDto

    @GET("auth/me")
    suspend fun getMe(@Header("Authorization") token: String): User

    @GET("missions")
    suspend fun getMissions(): List<ServerMissionDto>

    @POST("mission")
    suspend fun createMission(@Body mission: ServerMissionDto): ServerMissionDto
}
