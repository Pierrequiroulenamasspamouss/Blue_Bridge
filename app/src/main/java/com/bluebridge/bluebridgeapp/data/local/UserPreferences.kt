package com.bluebridge.bluebridgeapp.data.local

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
import com.bluebridge.bluebridgeapp.data.model.Location
import com.bluebridge.bluebridgeapp.data.model.MovementSpeeds
import com.bluebridge.bluebridgeapp.data.model.UserData
import com.bluebridge.bluebridgeapp.data.model.WaterNeed
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
}

class UserPreferences(context: Context) {

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
                val isWellOwner = preferences[PreferencesKeys.IS_WELL_OWNER] ?: false
                val token = preferences[PreferencesKeys.LOGIN_TOKEN]

                // Parse location from JSON string
                val locationJson = preferences[PreferencesKeys.LOCATION]
                val location = if (locationJson != null) {
                    try {
                        Json.decodeFromString<Location>(locationJson)
                    } catch (e: Exception) {
                        Log.e("UserPreferences", "Error parsing location", e)
                        Location(0.0, 0.0)
                    }
                } else {
                    Location(0.0, 0.0)
                }

                // Parse water needs from JSON string
                val waterNeedsJson = preferences[PreferencesKeys.WATER_NEEDS]
                val waterNeeds = if (waterNeedsJson != null) {
                    try {
                        Json.decodeFromString<List<WaterNeed>>(waterNeedsJson)
                    } catch (e: Exception) {
                        Log.e("UserPreferences", "Error parsing water needs", e)
                        emptyList()
                    }
                } else {
                    emptyList()
                }

                UserData(
                    email = email,
                    firstName = firstName,
                    lastName = lastName,
                    username = username,
                    role = role,
                    location = location,
                    themePreference = theme,
                    waterNeeds = waterNeeds,
                    movementSpeeds = MovementSpeeds(),
                    loginToken = token
                )
            } else {
                null
            }
        }

    suspend fun getUserData(): Flow<UserData?> = userDataFlow

    suspend fun clearUserData() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    suspend fun saveUserData(userData: UserData) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.EMAIL] = userData.email
            preferences[PreferencesKeys.FIRST_NAME] = userData.firstName
            preferences[PreferencesKeys.LAST_NAME] = userData.lastName
            preferences[PreferencesKeys.USERNAME] = userData.username
            preferences[PreferencesKeys.ROLE] = userData.role
            preferences[PreferencesKeys.THEME] = userData.themePreference
            preferences[PreferencesKeys.IS_WELL_OWNER] = userData.role == "well_owner"
            preferences[PreferencesKeys.IS_LOGGED_IN] = true

            // Store location as JSON string
            preferences[PreferencesKeys.LOCATION] = Json.encodeToString(userData.location)

            // Store water needs as JSON string
            preferences[PreferencesKeys.WATER_NEEDS] = Json.encodeToString(userData.waterNeeds)

            // Store login token
            userData.loginToken?.let { token ->
                preferences[PreferencesKeys.LOGIN_TOKEN] = token
            }
            
            // If this is a guest login, set the guest flag
            if (userData.role == "guest") {
                preferences[PreferencesKeys.IS_GUEST] = true
            }
        }
    }

    suspend fun isLoggedIn(): Boolean {
        val preferences = dataStore.data.first()
        return preferences[PreferencesKeys.IS_LOGGED_IN] ?: false
    }

    suspend fun updateThemePreference(theme: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME] = theme
        }
    }

    suspend fun getTheme(): Int {
        val preferences = dataStore.data.first()
        return preferences[PreferencesKeys.THEME] ?: 0
    }

    suspend fun getUserEmail(): String? {
        val preferences = dataStore.data.first()
        return preferences[PreferencesKeys.EMAIL]
    }

    suspend fun getLoginToken(): String? {
        val preferences = dataStore.data.first()
        return preferences[PreferencesKeys.LOGIN_TOKEN]
    }
    
    suspend fun getUserWaterNeeds(): String? {
        val preferences = dataStore.data.first()
        return preferences[PreferencesKeys.WATER_NEEDS]
    }
    
    suspend fun setUserWaterNeeds(waterNeeds: String) {
        dataStore.edit { preferences ->
            // We're storing as a stringified JSON array now, but keeping this method for backward compatibility
            // In future, consider parsing and saving as WaterNeed list directly
            if (waterNeeds.isNotBlank()) {
                val waterNeed = WaterNeed(
                    amount = waterNeeds.toIntOrNull() ?: 0,
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
    
    // Guest mode methods
    suspend fun setGuestMode(isGuest: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_GUEST] = isGuest
            
            if (!isGuest) {
                // Clear the guest flag when logging out of guest mode
                preferences.remove(PreferencesKeys.IS_GUEST)
            }
        }
    }
    
    suspend fun isGuestMode(): Boolean {
        val preferences = dataStore.data.first()
        return preferences[PreferencesKeys.IS_GUEST] ?: false
    }
    
    // Push notification methods
    suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] = enabled
        }
    }
    
    suspend fun areNotificationsEnabled(): Boolean {
        val preferences = dataStore.data.first()
        return preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] ?: false
    }
    
    suspend fun saveNotificationToken(token: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFICATION_TOKEN] = token
        }
    }
    
    suspend fun getNotificationToken(): String? {
        val preferences = dataStore.data.first()
        return preferences[PreferencesKeys.NOTIFICATION_TOKEN]
    }
    
    suspend fun clearNotificationToken() {
        dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.NOTIFICATION_TOKEN)
        }
    }
}