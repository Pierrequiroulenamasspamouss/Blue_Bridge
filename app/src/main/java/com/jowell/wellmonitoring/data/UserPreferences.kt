package com.jowell.wellmonitoring.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class UserPreferences(
    val username: String,
    val themePreference: ThemePreference
)
enum class ThemePreference(val value: Int) {
    SYSTEM_DEFAULT(0),
    LIGHT(1),
    DARK(2);

    companion object {
        fun fromValue(value: Int): ThemePreference {
            return ThemePreference.entries.firstOrNull { it.value == value } ?: SYSTEM_DEFAULT
        }
    }
}


class UserPreferencesStore(private val context: Context) {

    companion object {
        val NAME = stringPreferencesKey("Username")
        val THEME = intPreferencesKey("Theme")
    }

    suspend fun saveUsername(name: String) {
        context.wellDataStore.edit { prefs ->
            prefs[NAME] = name
        }
    }

    suspend fun saveThemePreference(theme: ThemePreference) {
        context.wellDataStore.edit { prefs ->
            prefs[THEME] = theme.value
        }
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.wellDataStore.data.map { prefs ->
        val name = prefs[NAME] ?: ""
        val theme = ThemePreference.fromValue(prefs[THEME] ?: 0)
        UserPreferences(name, theme)
    }
}