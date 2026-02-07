package com.sloth.registerapp.core.network

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.sloth.registerapp.core.auth.SessionManager
import com.sloth.registerapp.core.auth.TokenRepository
import com.sloth.registerapp.core.network.dto.AuthTokensDto
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import java.io.IOException
import kotlinx.coroutines.runBlocking

class TokenRefreshAuthenticator(
    context: Context,
    private val tokenRepository: TokenRepository
) : Authenticator {
    companion object {
        private const val TAG = "TokenRefreshAuth"
    }

    private val appContext = context.applicationContext
    private val refreshClient = OkHttpClient()
    private val gson = Gson()
    private val lock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        val path = response.request.url.encodedPath
        if (!AuthRoutePolicy.requiresAccessToken(path) || AuthRoutePolicy.isAuthRoute(path)) {
            return null
        }
        if (responseCount(response) >= 2) {
            return null
        }

        val failedToken = response.request.header("Authorization")
            ?.removePrefix("Bearer")
            ?.trim()

        synchronized(lock) {
            val latestAccessToken = tokenRepository.getAccessTokenBlocking()
            if (!latestAccessToken.isNullOrBlank() && latestAccessToken != failedToken) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $latestAccessToken")
                    .build()
            }

            val refreshToken = tokenRepository.getRefreshTokenBlocking() ?: return clearSessionAndAbort()
            val refreshed = refreshTokens(refreshToken) ?: return clearSessionAndAbort()
            val newAccessToken = refreshed.resolvedAccessToken() ?: return clearSessionAndAbort()
            val newRefreshToken = refreshed.refreshToken ?: refreshToken

            tokenRepository.saveTokensBlocking(newAccessToken, newRefreshToken)
            Log.i(TAG, "Token refresh succeeded")
            return response.request.newBuilder()
                .header("Authorization", "Bearer $newAccessToken")
                .build()
        }
    }

    private fun refreshTokens(refreshToken: String): AuthTokensDto? {
        val refreshUrl = "${RetrofitClient.baseUrl()}auth/refresh"
        val headerRequest = Request.Builder()
            .url(refreshUrl)
            .post("{}".toRequestBody("application/json; charset=utf-8".toMediaType()))
            .header("Authorization", "Bearer $refreshToken")
            .build()

        try {
            refreshClient.newCall(headerRequest).execute().use { response ->
                if (response.isSuccessful) {
                    return response.body?.charStream()?.use { reader ->
                        gson.fromJson(reader, AuthTokensDto::class.java)
                    }
                }
            }
        } catch (_: IOException) {
            // Fallback below (body style).
        }

        val bodyJson = gson.toJson(mapOf("refresh_token" to refreshToken))
        val bodyRequest = Request.Builder()
            .url(refreshUrl)
            .post(bodyJson.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        return try {
            refreshClient.newCall(bodyRequest).execute().use { response ->
                if (!response.isSuccessful) return null
                response.body?.charStream()?.use { reader ->
                    gson.fromJson(reader, AuthTokensDto::class.java)
                }
            }
        } catch (_: IOException) {
            null
        }
    }

    private fun clearSessionAndAbort(): Request? {
        Log.w(TAG, "Token refresh failed; clearing local session")
        tokenRepository.clearTokensBlocking()
        runBlocking {
            SessionManager.getInstance(appContext).clearSession()
        }
        return null
    }

    private fun responseCount(response: Response): Int {
        var result = 1
        var current = response.priorResponse
        while (current != null) {
            result++
            current = current.priorResponse
        }
        return result
    }
}
