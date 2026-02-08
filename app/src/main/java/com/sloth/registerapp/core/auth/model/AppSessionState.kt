package com.sloth.registerapp.core.auth.model

enum class LocalSessionState {
    LOCAL_LOGGED_OUT,
    LOCAL_LOGGED_IN
}

enum class NetworkState {
    ONLINE,
    OFFLINE
}

enum class ServerAuthState {
    SERVER_AUTH_OK,
    SERVER_ACCESS_EXPIRED,
    SERVER_AUTH_REQUIRED
}

