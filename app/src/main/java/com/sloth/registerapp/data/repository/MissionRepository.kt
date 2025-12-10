package com.sloth.registerapp.data.repository

import com.sloth.registerapp.data.model.Mission
import com.sloth.registerapp.data.network.SdiaApiService
import kotlinx.coroutines.flow.first

class MissionRepository(
    private val apiService: SdiaApiService,
    private val tokenRepository: TokenRepository
) {

    suspend fun getMissions(): Result<List<Mission>> {
        return try {
            val token = tokenRepository.token.first()
            val authHeader = "Bearer ${token.orEmpty()}"
            val missions = apiService.getMissions(authHeader)
            Result.success(missions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
