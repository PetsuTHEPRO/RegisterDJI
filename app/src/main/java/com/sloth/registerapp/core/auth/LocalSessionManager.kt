package com.sloth.registerapp.core.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sloth.registerapp.core.auth.model.LocalSessionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.localSessionStore: DataStore<Preferences> by preferencesDataStore(name = "local_session")

class LocalSessionManager private constructor(private val context: Context) {

    private val currentUserIdKey = stringPreferencesKey("current_user_id")
    private val currentUsernameKey = stringPreferencesKey("current_username")
    private val currentEmailKey = stringPreferencesKey("current_email")
    private val guestModeEnabledKey = booleanPreferencesKey("guest_mode_enabled")

    val currentUserId: Flow<String?> = context.localSessionStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { it[currentUserIdKey] }

    val currentUsername: Flow<String?> = context.localSessionStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { it[currentUsernameKey] }

    val currentEmail: Flow<String?> = context.localSessionStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { it[currentEmailKey] }

    val isGuestModeEnabled: Flow<Boolean> = context.localSessionStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { it[guestModeEnabledKey] ?: false }

    val localSessionState: Flow<LocalSessionState> = currentUserId.map { userId ->
        if (userId.isNullOrBlank()) LocalSessionState.LOCAL_LOGGED_OUT
        else LocalSessionState.LOCAL_LOGGED_IN
    }

    suspend fun setGuestModeEnabled(enabled: Boolean) {
        context.localSessionStore.edit { prefs ->
            prefs[guestModeEnabledKey] = enabled
        }
    }

    suspend fun loginLocal(userId: String, username: String, email: String?) {
        context.localSessionStore.edit { prefs ->
            prefs[currentUserIdKey] = userId
            prefs[currentUsernameKey] = username
            prefs[currentEmailKey] = email.orEmpty()
            prefs[guestModeEnabledKey] = false
        }
    }

    suspend fun logoutLocal() {
        context.localSessionStore.edit { prefs ->
            prefs.remove(currentUserIdKey)
            prefs.remove(currentUsernameKey)
            prefs.remove(currentEmailKey)
            prefs[guestModeEnabledKey] = false
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: LocalSessionManager? = null

        fun getInstance(context: Context): LocalSessionManager {
            return INSTANCE ?: synchronized(this) {
                val instance = LocalSessionManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}

