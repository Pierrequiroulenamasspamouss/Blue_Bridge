


package com.bluebridgeapp.bluebridge.data.local

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.bluebridgeapp.bluebridge.data.model.Location
import com.bluebridgeapp.bluebridge.data.model.UserData
import com.bluebridgeapp.bluebridge.data.model.WaterNeed
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException


// Define DataStore at top level
val Context.userDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

// Define preference keys
object PreferencesKeys {
    val USER_ID = stringPreferencesKey("user_id")
    val EMAIL = stringPreferencesKey("email")
    val FIRST_NAME = stringPreferencesKey("first_name")
    val LAST_NAME = stringPreferencesKey("last_name")
    val USERNAME = stringPreferencesKey("username")
    val ROLE = stringPreferencesKey("role")
    val THEME = intPreferencesKey("theme")
    val LOCATION = stringPreferencesKey("location")
    val WATER_NEEDS = stringPreferencesKey("water_needs")
    val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
    val LOGIN_TOKEN = stringPreferencesKey("login_token")
    val IS_WELL_OWNER = booleanPreferencesKey("is_well_owner")
    val IS_GUEST = booleanPreferencesKey("is_guest")
    val NOTIFICATION_TOKEN = stringPreferencesKey("notification_token")
    val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
    val LANGUAGE_CODE = stringPreferencesKey("language_code")
}

class UserPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    private val dataStore: DataStore<Preferences> = context.userDataStore

    val userDataFlow: Flow<UserData?> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Log.e("UserPreferences", "Error reading preferences", exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            if (preferences[PreferencesKeys.IS_LOGGED_IN] == true) {
                val email = preferences[PreferencesKeys.EMAIL] ?: return@map null
                val firstName = preferences[PreferencesKeys.FIRST_NAME] ?: ""
                val lastName = preferences[PreferencesKeys.LAST_NAME] ?: ""
                val username = preferences[PreferencesKeys.USERNAME] ?: ""
                val role = preferences[PreferencesKeys.ROLE] ?: "user"
                val theme = preferences[PreferencesKeys.THEME] ?: 0
                val token = preferences[PreferencesKeys.LOGIN_TOKEN]
                val languageCode = preferences[PreferencesKeys.LANGUAGE_CODE] ?: "en"
                val userId = preferences[PreferencesKeys.USER_ID] ?: return@map null
                val locationJson = preferences[PreferencesKeys.LOCATION]
                val location = if (locationJson != null) {
                    try { Json.decodeFromString<Location>(locationJson) } catch (e: Exception) { Location(0.0, 0.0) }
                } else { Location(0.0, 0.0) }
                val waterNeedsJson = preferences[PreferencesKeys.WATER_NEEDS]
                val waterNeeds = if (waterNeedsJson != null) {
                    try { Json.decodeFromString<List<WaterNeed>>(waterNeedsJson) } catch (e: Exception) { emptyList() }
                } else { emptyList() }
                UserData(
                    email = email,
                    firstName = firstName,
                    lastName = lastName,
                    username = username,
                    userId = userId,
                    role = role,
                    location = location,
                    themePreference = theme,
                    waterNeeds = waterNeeds,
                    loginToken = token,
                    languageCode = languageCode
                )
            } else {
                null
            }
        }

    fun getUserData(): Flow<UserData?> = userDataFlow

    suspend fun clearUserData() {
        dataStore.edit { preferences -> preferences.clear() }
    }

    suspend fun saveUserData(userData: UserData) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.USER_ID] = userData.userId
            preferences[PreferencesKeys.EMAIL] = userData.email
            preferences[PreferencesKeys.FIRST_NAME] = userData.firstName
            preferences[PreferencesKeys.LAST_NAME] = userData.lastName
            preferences[PreferencesKeys.USERNAME] = userData.username
            preferences[PreferencesKeys.ROLE] = userData.role
            preferences[PreferencesKeys.THEME] = userData.themePreference
            preferences[PreferencesKeys.IS_WELL_OWNER] = userData.role == "well_owner"
            preferences[PreferencesKeys.IS_LOGGED_IN] = true
            preferences[PreferencesKeys.LOCATION] = Json.encodeToString(userData.location)
            preferences[PreferencesKeys.WATER_NEEDS] = Json.encodeToString(userData.waterNeeds)
            userData.loginToken?.let { token -> preferences[PreferencesKeys.LOGIN_TOKEN] = token }
            if (userData.role == "guest") preferences[PreferencesKeys.IS_GUEST] = true
            preferences[PreferencesKeys.LANGUAGE_CODE] = userData.languageCode
        }
    }

    fun getThemePreference(): Int {
        return prefs.getInt("theme_preference", 0)
    }

    suspend fun saveThemePreference(theme: Int) {
        prefs.edit().putInt("theme_preference", theme).apply()
        dataStore.edit { preferences -> preferences[PreferencesKeys.THEME] = theme }
    }

    suspend fun setUserWaterNeeds(waterNeeds: String) {
        dataStore.edit { preferences ->
            if (waterNeeds.isNotBlank()) {
                val waterNeed = WaterNeed(
                    amount = waterNeeds.toFloatOrNull() ?: 0f,
                    usageType = "General",
                    priority = 3,
                    description = "General water need"
                )
                preferences[PreferencesKeys.WATER_NEEDS] = Json.encodeToString(listOf(waterNeed))
            } else {
                preferences[PreferencesKeys.WATER_NEEDS] = Json.encodeToString(emptyList<WaterNeed>())
            }
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { preferences -> preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun areNotificationsEnabled(): Boolean {
        val preferences = dataStore.data.first()
        return preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] == true
    }

    suspend fun saveNotificationToken(token: String) {
        dataStore.edit { preferences -> preferences[PreferencesKeys.NOTIFICATION_TOKEN] = token }
    }

    suspend fun getNotificationToken(): String? {
        val preferences = dataStore.data.first()
        return preferences[PreferencesKeys.NOTIFICATION_TOKEN]
    }

    suspend fun clearNotificationToken() {
        dataStore.edit { preferences -> preferences.remove(PreferencesKeys.NOTIFICATION_TOKEN) }
    }

    suspend fun setLanguage(language: String) {
        prefs.edit().putString("language_code", language).apply()
        dataStore.edit { preferences -> preferences[PreferencesKeys.LANGUAGE_CODE] = language }
    }

    suspend fun getLanguage(): String? {
        val preferences = dataStore.data.first()
        return preferences[stringPreferencesKey("language_code")]
    }
}
