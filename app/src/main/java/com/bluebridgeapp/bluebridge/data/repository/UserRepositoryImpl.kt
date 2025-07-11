package com.bluebridgeapp.bluebridge.data.repository

import android.util.Log
import com.bluebridgeapp.bluebridge.data.interfaces.UserRepository
import com.bluebridgeapp.bluebridge.data.local.UserPreferences
import com.bluebridgeapp.bluebridge.data.model.DeleteAccountRequest
import com.bluebridgeapp.bluebridge.data.model.LoginRequest
import com.bluebridgeapp.bluebridge.data.model.NotificationTokenRequest
import com.bluebridgeapp.bluebridge.data.model.RegisterRequest
import com.bluebridgeapp.bluebridge.data.model.UpdateProfileRequest
import com.bluebridgeapp.bluebridge.data.model.UpdateWaterNeedsRequest
import com.bluebridgeapp.bluebridge.data.model.UserData
import com.bluebridgeapp.bluebridge.data.model.WaterNeed
import com.bluebridgeapp.bluebridge.network.ServerApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import org.json.JSONObject

class UserRepositoryImpl(
    private val api: ServerApi,
    private val preferences: UserPreferences
) : UserRepository {
    // 1. Authentication Methods
    override suspend fun register(request: RegisterRequest): String {
        return try {
            val response = api.register(request)
            if (response.isSuccessful && response.body()?.status == "success") {
                val userDataResponse = response.body()!!.userData
                val userData = UserData(
                    email = request.email,
                    userId = userDataResponse!!.userId,
                    firstName = request.firstName,
                    lastName = request.lastName,
                    username = request.username,
                    role = request.role,
                    location = request.location,
                    themePreference = request.themePreference,
                    waterNeeds = request.waterNeeds,
                    loginToken = userDataResponse.loginToken
                )
                preferences.saveUserData(userData)
                "true"
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = try {
                    if (!errorBody.isNullOrEmpty()) {
                        val json = JSONObject(errorBody)
                        json.getString("message")
                    } else {
                        response.body()?.message ?: "Unknown error occurred"
                    }
                } catch (e: Exception) {
                    "Registration failed: ${response.message()}"
                }
                errorMessage
            }
        } catch (e: Exception) {
            "Registration failed: ${e.message}"
        }
    }

    override suspend fun login(request: LoginRequest): Boolean = withContext(Dispatchers.IO) {
        try {
            val response = api.login(request)
            if (response.isSuccessful && response.body()?.status == "success") {
                val userDataResponse = response.body()!!.data
                val userData = UserData(
                    userId = userDataResponse.userId,
                    email = userDataResponse.email,
                    firstName = userDataResponse.firstName,
                    lastName = userDataResponse.lastName,
                    username = userDataResponse.username,
                    role = userDataResponse.role,
                    themePreference = userDataResponse.themePreference,
                    location = userDataResponse.location,
                    waterNeeds = userDataResponse.waterNeeds,
                    lastLogin = System.currentTimeMillis().toString(),
                    loginToken = userDataResponse.loginToken
                )
                preferences.saveUserData(userData)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun logout() {
        preferences.clearUserData()
    }

    override suspend fun deleteAccount(deleteRequest: DeleteAccountRequest): Boolean = withContext(Dispatchers.IO) {
        try {
            val response = api.deleteAccount(deleteRequest)
            if (response.isSuccessful && response.body()?.status == "success") {
                preferences.clearUserData()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun isLoggedIn(): Boolean {
        return getUserData().firstOrNull()?.loginToken != null
    }

    // 2. User Data Methods
    override suspend fun getUserData(): Flow<UserData?> = preferences.getUserData()
    override suspend fun saveUserData(userData: UserData) = preferences.saveUserData(userData)
    override suspend fun clearUserData() = preferences.clearUserData()
    override suspend fun getUserId(): String = getUserData().firstOrNull()?.userId.orEmpty()
    override suspend fun getUserEmail(): String? = getUserData().firstOrNull()?.email
    override suspend fun getLoginToken(): String? = getUserData().firstOrNull()?.loginToken

    // 3. Role Management
    override suspend fun getRole(): String = getUserData().firstOrNull()?.role.orEmpty()
    override suspend fun getRoleValue(): Int {
        return when (getRole()) {
            "unregistered" -> 0
            "guest" -> 1
            "user" -> 2
            "well_owner" -> 3
            "admin" -> 4
            else -> 0
        }
    }

    // 4. Water Needs Management
    override suspend fun getUserWaterNeeds(): String? = getUserData().firstOrNull()?.waterNeeds.toString()
    override suspend fun setUserWaterNeeds(waterNeeds: List<WaterNeed>) = preferences.setUserWaterNeeds(waterNeeds.toString())
    override suspend fun updateWaterNeedsOnServer(waterNeeds: List<WaterNeed>): Boolean = withContext(Dispatchers.IO) {
        try {
            val token = getLoginToken() ?: ""
            val userId = getUserId()
            val request = UpdateWaterNeedsRequest(
                userId = userId,
                waterNeeds = waterNeeds,
                loginToken = token
            )
            val response = api.updateWaterNeeds(request)
            if (response.isSuccessful) {
                getUserData().firstOrNull()?.let {
                    saveUserData(it.copy(waterNeeds = waterNeeds))
                }
                true
            } else {
                getUserData().firstOrNull()?.let {
                    saveUserData(it.copy(waterNeeds = waterNeeds))
                }
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    // 5. Profile Management
    override suspend fun updateProfileOnServer(userData: UserData): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = UpdateProfileRequest(
                userId = userData.userId,
                firstName = userData.firstName,
                lastName = userData.lastName,
                username = userData.username,
                location = userData.location,
                loginToken = userData.loginToken ?: ""
            )
            val response = api.updateProfile(request)
            if (response.isSuccessful) {
                saveUserData(userData)
                true
            } else {
                saveUserData(userData)
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    // 6. Theme Preferences
    override suspend fun getTheme(): Int = getUserData().firstOrNull()?.themePreference ?: 0
    override suspend fun getLanguage(): String {
        return preferences.getLanguage().orEmpty()
    }
    override suspend fun saveThemePreference(theme: Int) = preferences.saveThemePreference(theme)

    // 7. Notification Management
    override suspend fun setNotificationsEnabled(enabled: Boolean) = preferences.setNotificationsEnabled(enabled)
    override suspend fun areNotificationsEnabled(): Boolean = preferences.areNotificationsEnabled()
    override suspend fun saveNotificationToken(token: String) = preferences.saveNotificationToken(token)
    override suspend fun getNotificationToken(): String? = preferences.getNotificationToken()
    override suspend fun clearNotificationToken() = preferences.clearNotificationToken()
    override suspend fun registerNotificationToken(
        userId: String,
        authToken: String,
        fcmToken: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = NotificationTokenRequest(
                userId = userId,
                loginToken = authToken,
                deviceToken = fcmToken
            )
            val response = api.registerNotificationToken(request)
            response.isSuccessful && response.body()?.status == "success"
        } catch (e: Exception) {
            false
        }
    }
    override suspend fun unregisterNotificationToken(
        userId: String,
        authToken: String,
        fcmToken: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = NotificationTokenRequest(
                userId = userId,
                loginToken = authToken,
                deviceToken = fcmToken
            )
            val response = api.unregisterNotificationToken(request)
            response.isSuccessful && response.body()?.status == "success"
        } catch (e: Exception) {
            false
        }
    }
    override suspend fun updateLocation(location: com.bluebridgeapp.bluebridge.data.model.Location): Boolean {
        return try {
            val userData = getUserData().firstOrNull()
            userData?.let {
                val updatedUser = it.copy(location = location)
                saveUserData(updatedUser)
                updateProfileOnServer(updatedUser)
            } == true
        } catch (e: Exception) {
            false
        }
    }
    override suspend fun setLanguage(language: String) = preferences.setLanguage(language)
}