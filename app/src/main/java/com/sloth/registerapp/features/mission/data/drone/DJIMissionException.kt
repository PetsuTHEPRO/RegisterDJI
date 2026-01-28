package com.sloth.registerapp.features.mission.data.drone

/**
 * Exception customizada para erros de missão DJI
 */
class DJIMissionException(message: String, cause: Throwable? = null) :
    Exception(message, cause)
