package com.sloth.registerapp.data.model

data class User(
    val id: String,
    val username: String,
    val email: String
    // Adicione outros campos conforme seu backend Python retorna (nome, etc.)
)
