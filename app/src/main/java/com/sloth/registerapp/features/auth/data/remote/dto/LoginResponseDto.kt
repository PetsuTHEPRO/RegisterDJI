package com.sloth.registerapp.features.auth.data.remote.dto

data class LoginResponseDto(
    val token: String,
    val userId: String,
    val username: String? = null,
    val email: String? = null
)
