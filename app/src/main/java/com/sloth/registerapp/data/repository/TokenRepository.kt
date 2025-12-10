package com.sloth.registerapp.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class TokenRepository(private val context: Context) {

    private val tokenKey = stringPreferencesKey("auth_token")

    val token: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[tokenKey]
        }

    suspend fun saveToken(token: String) {
        context.dataStore.edit { settings ->
            settings[tokenKey] = token
        }
    }
}
