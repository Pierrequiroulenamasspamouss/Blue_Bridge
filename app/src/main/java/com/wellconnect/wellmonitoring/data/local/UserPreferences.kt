package com.wellconnect.wellmonitoring.data.local

import UserData
import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import com.wellconnect.wellmonitoring.data.model.Location
import com.wellconnect.wellmonitoring.data.model.WaterNeed
import com.wellconnect.wellmonitoring.viewmodels.PreferencesKeys
import com.wellconnect.wellmonitoring.viewmodels.userDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json



@OptIn(ExperimentalSerializationApi::class)
class UserPreferences(context: Context) {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }
    private val dataStore = context.userDataStore

    suspend fun updateThemePreference(theme: Int) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.THEME] = theme
        }
    }

    suspend fun getTheme(): Int {
        return dataStore.data.first()[PreferencesKeys.THEME] ?: 0
    }

    suspend fun saveUserData(userData: UserData) {
        try {
            Log.d("UserPreferences", "Starting to save user data to DataStore")
            dataStore.edit { prefs ->
                prefs[PreferencesKeys.EMAIL] = userData.email
                prefs[PreferencesKeys.FIRST_NAME] = userData.firstName
                prefs[PreferencesKeys.LAST_NAME] = userData.lastName
                prefs[PreferencesKeys.USERNAME] = userData.username
                prefs[PreferencesKeys.ROLE] = userData.role
                prefs[PreferencesKeys.THEME] = userData.themePreference
                prefs[PreferencesKeys.IS_LOGGED_IN] = true
                prefs[PreferencesKeys.LOCATION] = json.encodeToString(Location.serializer(), userData.location)
                prefs[PreferencesKeys.WATER_NEEDS] = json.encodeToString(ListSerializer(WaterNeed.serializer()), userData.waterNeeds)
                prefs[PreferencesKeys.LOGIN_TOKEN] = userData.loginToken ?: ""
                prefs[PreferencesKeys.IS_WELL_OWNER] = userData.isWellOwner
            }
            Log.d("UserPreferences", "User data successfully saved to DataStore: $userData")
            
            // Verify data was saved correctly
            val isLoggedIn = isLoggedIn()
            val savedEmail = getUserEmail()
            Log.d("UserPreferences", "Verification after save: isLoggedIn=$isLoggedIn, email=$savedEmail")
        } catch (e: Exception) {
            Log.e("UserPreferences", "Error saving user data to DataStore", e)
            throw e
        }
    }

    fun getUserData(): Flow<UserData?> = dataStore.data
        .map { prefs ->
            val isLoggedIn = prefs[PreferencesKeys.IS_LOGGED_IN] == true
            val email = prefs[PreferencesKeys.EMAIL]
            
            if (isLoggedIn && email != null) {
                try {
                    val userData = UserData(
                        email = email,
                        firstName = prefs[PreferencesKeys.FIRST_NAME] ?: "",
                        lastName = prefs[PreferencesKeys.LAST_NAME] ?: "",
                        username = prefs[PreferencesKeys.USERNAME] ?: "",
                        role = prefs[PreferencesKeys.ROLE] ?: "user",
                        themePreference = prefs[PreferencesKeys.THEME] ?: 0,
                        location = prefs[PreferencesKeys.LOCATION]?.let {
                            json.decodeFromString(Location.serializer(), it)
                        } ?: Location(0.0, 0.0),
                        waterNeeds = prefs[PreferencesKeys.WATER_NEEDS]?.let {
                            json.decodeFromString(ListSerializer(WaterNeed.serializer()), it)
                        } ?: emptyList(),
                        loginToken = prefs[PreferencesKeys.LOGIN_TOKEN],
                        isWellOwner = prefs[PreferencesKeys.IS_WELL_OWNER] == true,
                    )
                    Log.d("UserPreferences", "Retrieved user data: $userData")
                    return@map userData
                } catch (e: Exception) {
                    Log.e("UserPreferences", "Error deserializing user data", e)
                    null
                }
            } else {
                Log.d("UserPreferences", "No user logged in or email is null. isLoggedIn=$isLoggedIn, email=$email")
                null
            }
        }

    suspend fun clearUserData() {
        dataStore.edit { it.clear() }
    }

    suspend fun isLoggedIn(): Boolean {
        return dataStore.data.first()[PreferencesKeys.IS_LOGGED_IN] == true
    }

    suspend fun getLoginToken(): String? {
        return dataStore.data.first()[PreferencesKeys.LOGIN_TOKEN]
    }
    suspend fun getUserEmail(): String? {
        return dataStore.data.first()[PreferencesKeys.EMAIL]
    }

    suspend fun getUserWaterNeeds(): String? {
        return dataStore.data.first()[PreferencesKeys.WATER_NEEDS]
    }

    suspend fun setUserWaterNeeds(waterNeeds: String) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.WATER_NEEDS] = waterNeeds
        }
    }
}