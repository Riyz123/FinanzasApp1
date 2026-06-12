package com.upn3.proyecto_finanzas_personales.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

class UserPreferences(private val context: Context) {
    companion object {
        val USER_EMAIL = stringPreferencesKey("user_email")
        val CACHED_RATES = stringPreferencesKey("cached_rates")
        val LAST_RATES_UPDATE = longPreferencesKey("last_rates_update")
        val SELECTED_LANGUAGE = stringPreferencesKey("selected_language")
        val INSTALLED_LANGUAGES = stringPreferencesKey("installed_languages")
    }

    val userEmail: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[USER_EMAIL]
        }

    val cachedRates: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[CACHED_RATES]
        }

    val lastRatesUpdate: Flow<Long> = context.dataStore.data
        .map { preferences ->
            preferences[LAST_RATES_UPDATE] ?: 0L
        }

    val selectedLanguage: Flow<String> =
        context.dataStore.data.map { preferences ->
            preferences[SELECTED_LANGUAGE] ?: "es"
        }

    val installedLanguages: Flow<String> =
        context.dataStore.data.map { preferences ->

            preferences[INSTALLED_LANGUAGES] ?: ""
        }

    suspend fun saveUserEmail(email: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_EMAIL] = email
        }
    }

    suspend fun saveRates(ratesJson: String, timestamp: Long) {
        context.dataStore.edit { preferences ->
            preferences[CACHED_RATES] = ratesJson
            preferences[LAST_RATES_UPDATE] = timestamp
        }
    }

    suspend fun clearUserEmail() {
        context.dataStore.edit { preferences ->
            preferences.remove(USER_EMAIL)
        }
    }

    suspend fun saveLanguage(language: String) {
        context.dataStore.edit { preferences ->
            preferences[SELECTED_LANGUAGE] = language
        }
    }

    suspend fun saveInstalledLanguages(languages: String) {
        context.dataStore.edit { preferences ->
            preferences[INSTALLED_LANGUAGES] = languages
        }
    }
}
