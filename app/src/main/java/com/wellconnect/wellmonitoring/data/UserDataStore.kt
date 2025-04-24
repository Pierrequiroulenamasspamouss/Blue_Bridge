package com.wellconnect.wellmonitoring.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.wellconnect.wellmonitoring.network.LoginRequest
import com.wellconnect.wellmonitoring.network.RetrofitBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

private val Context.userDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

interface UserDataStore {
    suspend fun login(email: String, password: String): Boolean
    suspend fun saveUserData(userData: UserData)
    fun getUserData(): Flow<UserData?>
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
    }

    override suspend fun login(email: String, password: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val api = RetrofitBuilder.create("http://192.168.0.98:8090")
                val response = api.login(LoginRequest(email = email, password = password))
                
                if (response.isSuccessful && response.body() != null) {
                    val loginResponse = response.body()!!
                    if (loginResponse.status == "success" && loginResponse.userData?.user != null) {
                        val user = UserData(
                            email = loginResponse.userData.user.email,
                            firstName = loginResponse.userData.user.firstName,
                            lastName = loginResponse.userData.user.lastName,
                            username = loginResponse.userData.user.username,
                            role = loginResponse.userData.user.role,
                            themePreference = 0 // Default to system theme
                        )
                        saveUserData(user)
                        true
                    } else {
                        Log.w("UserDataStore", "Login failed: ${loginResponse.message}")
                        false
                    }
                } else {
                    Log.e("UserDataStore", "Login failed: ${response.errorBody()?.string()}")
                    false
                }
            } catch (e: Exception) {
                Log.e("UserDataStore", "Login error", e)
                false
            }
        }
    }

    override suspend fun saveUserData(userData: UserData) {
        context.userDataStore.edit { preferences ->
            preferences[PreferencesKeys.EMAIL] = userData.email
            preferences[PreferencesKeys.FIRST_NAME] = userData.firstName
            preferences[PreferencesKeys.LAST_NAME] = userData.lastName
            preferences[PreferencesKeys.USERNAME] = userData.username
            preferences[PreferencesKeys.ROLE] = userData.role
            preferences[PreferencesKeys.THEME] = userData.themePreference
        }
    }

    override fun getUserData(): Flow<UserData?> {
        return context.userDataStore.data.map { preferences ->
            if (preferences[PreferencesKeys.EMAIL] != null) {
                UserData(
                    email = preferences[PreferencesKeys.EMAIL]!!,
                    firstName = preferences[PreferencesKeys.FIRST_NAME]!!,
                    lastName = preferences[PreferencesKeys.LAST_NAME]!!,
                    username = preferences[PreferencesKeys.USERNAME]!!,
                    role = preferences[PreferencesKeys.ROLE]!!,
                    themePreference = preferences[PreferencesKeys.THEME] ?: 0
                )
            } else {
                null
            }
        }
    }

    override suspend fun clearUserData() {
        context.userDataStore.edit { preferences ->
            preferences.clear()
        }
    }

    override suspend fun updateThemePreference(theme: Int) {
        context.userDataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME] = theme
        }
    }
} 