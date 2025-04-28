package com.wellconnect.wellmonitoring.data

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
import com.wellconnect.wellmonitoring.network.LoginRequest
import com.wellconnect.wellmonitoring.network.RetrofitBuilder
import com.wellconnect.wellmonitoring.utils.getBaseApiUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val TAG = "UserDataStore"
private val Context.userDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

interface UserDataStore {
    suspend fun login(email: String, password: String): Boolean
    suspend fun saveUserData(userData: UserData)
    fun getUserData(): Flow<UserData?>
    suspend fun isLoggedIn(): Boolean
    suspend fun clearUserData()
    suspend fun updateThemePreference(theme: Int)
}

class UserDataStoreImpl(private val context: Context) : UserDataStore {
    private object PreferencesKeys {
        val EMAIL = stringPreferencesKey("email")
        val FIRST_NAME = stringPreferencesKey("first_name")
        val LAST_NAME = stringPreferencesKey("last_name")
        val USERNAME = stringPreferencesKey("username")
        val ROLE = stringPreferencesKey("role")
        val THEME = intPreferencesKey("theme")
        val LOCATION = stringPreferencesKey("location")
        val WATER_NEEDS = stringPreferencesKey("water_needs")
        val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
    }

    override suspend fun login(email: String, password: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Attempting login for email: $email")
                val baseUrl = getBaseApiUrl(context)
                val api = RetrofitBuilder.create(baseUrl)
                val response = api.login(LoginRequest(email = email, password = password))
                
                if (response.isSuccessful && response.body() != null) {
                    val loginResponse = response.body()!!
                    Log.d(TAG, "Response received: ${loginResponse.status}, message: ${loginResponse.message}")
                    
                    if (loginResponse.status == "success" && loginResponse.data != null) {
                        val serverData = loginResponse.data
                        Log.d(TAG, "Login successful. User data received: ${serverData.user.username}")
                        
                        // Create userData with server data
                        val userData = UserData(
                            email = serverData.user.email,
                            firstName = serverData.user.firstName,
                            lastName = serverData.user.lastName,
                            username = serverData.user.username,
                            role = serverData.user.role,
                            themePreference = context.userDataStore.data.first()[PreferencesKeys.THEME] ?: 0,
                            location = if (serverData.location != null) {
                                Location(
                                    latitude = serverData.location.latitude ?: 0.0,
                                    longitude = serverData.location.longitude ?: 0.0
                                )
                            } else {
                                Location(0.0, 0.0)
                            },
                            waterNeeds = serverData.waterNeeds?.map { need ->
                                WaterNeed(
                                    amount = need.amount,
                                    usageType = need.usageType,
                                    description = need.description,
                                    priority = need.priority
                                )
                            } ?: emptyList()
                        )
                        
                        Log.d(TAG, "Saving user data to preferences")
                        saveUserData(userData)
                        return@withContext true
                    } else {
                        Log.w(TAG, "Login failed: ${loginResponse.message}")
                        return@withContext false
                    }
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                    Log.e(TAG, "Login request failed with code ${response.code()}: $errorMsg")
                    return@withContext false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception during login", e)
                return@withContext false
            }
        }
    }

    override suspend fun isLoggedIn(): Boolean {
        val isLoggedIn = context.userDataStore.data.first()[PreferencesKeys.IS_LOGGED_IN] ?: false
        Log.d(TAG, "isLoggedIn check: $isLoggedIn")
        
        if (isLoggedIn) {
            val email = context.userDataStore.data.first()[PreferencesKeys.EMAIL]
            Log.d(TAG, "User is logged in with email: $email")
        }
        
        return isLoggedIn
    }

    override suspend fun saveUserData(userData: UserData) {
        try {
            Log.d(TAG, "Saving user data for: ${userData.username}")
            context.userDataStore.edit { preferences ->
                preferences[PreferencesKeys.EMAIL] = userData.email
                preferences[PreferencesKeys.FIRST_NAME] = userData.firstName
                preferences[PreferencesKeys.LAST_NAME] = userData.lastName
                preferences[PreferencesKeys.USERNAME] = userData.username
                preferences[PreferencesKeys.ROLE] = userData.role
                preferences[PreferencesKeys.THEME] = userData.themePreference
                preferences[PreferencesKeys.IS_LOGGED_IN] = true
                
                // Serialize complex objects to JSON strings
                val locationJson = Json.encodeToString(userData.location)
                val waterNeedsJson = Json.encodeToString(userData.waterNeeds)
                preferences[PreferencesKeys.LOCATION] = locationJson
                preferences[PreferencesKeys.WATER_NEEDS] = waterNeedsJson
                
                Log.d(TAG, "User data saved successfully")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving user data", e)
            throw e
        }
    }

    override fun getUserData(): Flow<UserData?> {
        return context.userDataStore.data
            .catch { exception ->
                Log.e(TAG, "Error reading user data", exception)
                emit(emptyPreferences())
            }
            .map { preferences ->
                try {
                    val isLoggedIn = preferences[PreferencesKeys.IS_LOGGED_IN] ?: false
                    val email = preferences[PreferencesKeys.EMAIL]
                    
                    if (isLoggedIn && email != null) {
                        val locationJson = preferences[PreferencesKeys.LOCATION]
                        val waterNeedsJson = preferences[PreferencesKeys.WATER_NEEDS]
                        
                        val userData = UserData(
                            email = email,
                            firstName = preferences[PreferencesKeys.FIRST_NAME] ?: "",
                            lastName = preferences[PreferencesKeys.LAST_NAME] ?: "",
                            username = preferences[PreferencesKeys.USERNAME] ?: "",
                            role = preferences[PreferencesKeys.ROLE] ?: "user",
                            themePreference = preferences[PreferencesKeys.THEME] ?: 0,
                            location = if (locationJson != null) {
                                try {
                                    Json.decodeFromString<Location>(locationJson)
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error parsing location data", e)
                                    Location(0.0, 0.0)
                                }
                            } else {
                                Location(0.0, 0.0)
                            },
                            waterNeeds = if (waterNeedsJson != null) {
                                try {
                                    Json.decodeFromString<List<WaterNeed>>(waterNeedsJson)
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error parsing water needs data", e)
                                    emptyList()
                                }
                            } else {
                                emptyList()
                            }
                        )
                        // Log.d(TAG, "Retrieved user data for: ${userData.username}")
                        userData
                    } else {
                        Log.d(TAG, "No user data found or user not logged in")
                        null
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing user data", e)
                    null
                }
            }
    }

    override suspend fun clearUserData() {
        Log.d(TAG, "Clearing user data")
        context.userDataStore.edit { preferences ->
            preferences.clear()
        }
    }

    override suspend fun updateThemePreference(theme: Int) {
        Log.d(TAG, "Updating theme preference to: $theme")
        context.userDataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME] = theme
        }
    }
}