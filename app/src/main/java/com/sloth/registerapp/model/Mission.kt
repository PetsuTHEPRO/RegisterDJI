package com.sloth.registerapp.model

data class Mission(
    val id: Int,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val waypointCount: Int,
    val autoSpeed: Float,
    val maxSpeed: Float
)
