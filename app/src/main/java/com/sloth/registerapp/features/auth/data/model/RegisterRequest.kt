package com.sloth.registerapp.features.auth.data.model

data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String
)
