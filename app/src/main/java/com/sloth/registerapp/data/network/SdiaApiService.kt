package com.sloth.registerapp.data.network

import com.sloth.registerapp.data.model.LoginResponse
import com.sloth.registerapp.data.model.Mission
import com.sloth.registerapp.data.model.User // Adicionado: Import para o novo modelo de usuário
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface SdiaApiService {
    @POST("auth/login") // MODIFICADO: Caminho do endpoint e tipo de autenticação
    suspend fun login(@Header("Authorization") authHeader: String): LoginResponse

    @GET("auth/me") // NOVO: Endpoint para obter dados do usuário autenticado
    suspend fun getMe(@Header("Authorization") token: String): User

    // Exemplo para buscar missões
    @GET("missions")
    suspend fun getMissions(@Header("Authorization") token: String): List<Mission>
}
