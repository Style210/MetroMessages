// File: FacebookTokenManager.kt
package com.metromessages.auth


import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "facebook_tokens")

class FacebookTokenManager(private val context: Context) {

    companion object {
        private val ACCESS_TOKEN_KEY = stringPreferencesKey("fb_access_token")
        private val USER_ID_KEY = stringPreferencesKey("fb_user_id")
    }

    suspend fun saveTokens(accessToken: String, userId: String) {
        context.dataStore.edit { preferences ->
            preferences[ACCESS_TOKEN_KEY] = accessToken
            preferences[USER_ID_KEY] = userId
        }
    }

    suspend fun getAccessToken(): String? {
        return context.dataStore.data
            .map { it[ACCESS_TOKEN_KEY] }
            .first()
    }

    suspend fun getUserId(): String? {
        return context.dataStore.data
            .map { it[USER_ID_KEY] }
            .first()
    }

    suspend fun clearTokens() {
        context.dataStore.edit { preferences ->
            preferences.remove(ACCESS_TOKEN_KEY)
            preferences.remove(USER_ID_KEY)
        }
    }
}

