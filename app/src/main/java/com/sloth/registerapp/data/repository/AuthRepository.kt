package com.sloth.registerapp.data.repository

import com.sloth.registerapp.data.model.LoginRequest
import com.sloth.registerapp.data.model.LoginResponse
import com.sloth.registerapp.data.network.SdiaApiService

class AuthRepository(private val apiService: SdiaApiService) {
    suspend fun loginUser(username: String, password: String): Result<LoginResponse> {
        return try {
            val request = LoginRequest(username, password)
            val response = apiService.login(request)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
