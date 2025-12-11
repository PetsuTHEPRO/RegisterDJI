package com.sloth.registerapp.data.repository

import android.util.Base64
import com.sloth.registerapp.data.model.LoginResponse
import com.sloth.registerapp.data.network.SdiaApiService

class AuthRepository(private val apiService: SdiaApiService) {
    suspend fun loginUser(username: String, password: String): Result<LoginResponse> {
        return try {
            val credentials = "$username:$password"
            val basicAuth = "Basic " + Base64.encodeToString(credentials.toByteArray(), Base64.NO_WRAP)
            val response = apiService.login(basicAuth)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
