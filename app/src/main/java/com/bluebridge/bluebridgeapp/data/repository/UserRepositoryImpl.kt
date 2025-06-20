package com.bluebridge.bluebridgeapp.data.repository

import android.util.Log
import com.bluebridge.bluebridgeapp.data.AppEvent
import com.bluebridge.bluebridgeapp.data.AppEventChannel
import com.bluebridge.bluebridgeapp.data.`interface`.UserRepository
import com.bluebridge.bluebridgeapp.data.local.UserPreferences
import com.bluebridge.bluebridgeapp.data.model.DeleteAccountRequest
import com.bluebridge.bluebridgeapp.data.model.LoginRequest
import com.bluebridge.bluebridgeapp.data.model.NotificationTokenRequest
import com.bluebridge.bluebridgeapp.data.model.RegisterRequest
import com.bluebridge.bluebridgeapp.data.model.UpdateProfileRequest
import com.bluebridge.bluebridgeapp.data.model.UpdateWaterNeedsRequest
import com.bluebridge.bluebridgeapp.data.model.UserData
import com.bluebridge.bluebridgeapp.data.model.WaterNeed
import com.bluebridge.bluebridgeapp.network.ServerApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import org.json.JSONObject


class UserRepositoryImpl(
    private val api: ServerApi,
    private val preferences: UserPreferences
) : UserRepository {

    // ============================================================================================
    // 1. Authentication Methods
    // ============================================================================================

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

                Log.d("UserRepository", "Registration successful: $userData")
                saveUserData(userData)
                "true"
            } else {
                // Try to parse error response
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

                Log.e("UserRepository", "Registration failed: $errorMessage")
                errorMessage
            }
        } catch (e: Exception) {
            val errorMsg = "Registration failed: ${e.message}"
            Log.e("UserRepository", errorMsg, e)
            errorMsg
        }
    }

    override suspend fun login(request: LoginRequest): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d("UserRepository", "Attempting login with email: ${request.email}")
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

                saveUserData(userData)
                true
            } else {
                val errorCode = response.code()
                val errorBody = response.errorBody()?.string()
                Log.e("UserRepository", "Login failed with code $errorCode: $errorBody")
                AppEventChannel.sendEvent(AppEvent.ShowInfo(errorBody.toString()))
                false

            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Login failed with exception", e)
            false
        }
    }

    override suspend fun logout() {
        clearUserData()
    }

    override suspend fun deleteAccount(deleteRequest: DeleteAccountRequest): Boolean = withContext(Dispatchers.IO) {
        try {
            val response = api.deleteAccount(deleteRequest)

            if (response.isSuccessful && response.body()?.status == "success") {
                clearUserData()
                true
            } else {
                val errorCode = response.code()
                val errorBody = response.errorBody()?.string()
                Log.e("UserRepository", "Delete account failed with code $errorCode: $errorBody")
                false
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Delete account failed with exception", e)
            false
        }
    }

    override suspend fun isLoggedIn(): Boolean {
        return getUserData().firstOrNull()?.loginToken != null
    }

    // ============================================================================================
    // 2. User Data Methods
    // ============================================================================================

    override suspend fun getUserData(): Flow<UserData?> = preferences.getUserData()
    override suspend fun saveUserData(userData: UserData) = preferences.saveUserData(userData)
    override suspend fun clearUserData() = preferences.clearUserData()

    // Convenience methods for accessing specific user data fields
    override suspend fun getUserId(): String = getUserData().firstOrNull()?.userId.toString()
    override suspend fun getUserEmail(): String? = getUserData().firstOrNull()?.email
    override suspend fun getLoginToken(): String? = getUserData().firstOrNull()?.loginToken

    // ============================================================================================
    // 3. Role Management
    // ============================================================================================

    override suspend fun getRole(): String = getUserData().firstOrNull()?.role.toString()

    override suspend fun getRoleValue(): Int {
        return when (getRole()) {
            "unregistered" -> 0
            "guest" -> 1
            "user" -> 2
            "well_owner" -> 3
            "admin" -> 4
            else -> 0 // Default or unknown role
        }
    }

    // ============================================================================================
    // 4. Water Needs Management
    // ============================================================================================

    override suspend fun getUserWaterNeeds(): String? = getUserData().firstOrNull()?.waterNeeds.toString()
    override suspend fun setUserWaterNeeds(waterNeeds: String) = preferences.setUserWaterNeeds(waterNeeds)

    override suspend fun updateWaterNeedsOnServer(waterNeeds: List<WaterNeed>): Boolean = withContext(Dispatchers.IO) {
        try {
            val email = getUserEmail() ?: return@withContext false
            val token = getLoginToken() ?: ""

            val request = UpdateWaterNeedsRequest(
                email = email,
                waterNeeds = waterNeeds,
                token = token
            )

            val response = api.updateWaterNeeds(request)

            if (response.isSuccessful) {
                // Update local data after successful server update
                getUserData().first()?.let {
                    saveUserData(it.copy(waterNeeds = waterNeeds))
                }
                true
            } else {
                // Fallback to local storage if server update fails
                getUserData().first()?.let {
                    saveUserData(it.copy(waterNeeds = waterNeeds))
                }
                true
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Exception updating water needs", e)
            false
        }
    }

    // ============================================================================================
    // 5. Profile Management
    // ============================================================================================

    override suspend fun updateProfileOnServer(userData: UserData): Boolean = withContext(Dispatchers.IO) {
        try {
            val token = getLoginToken() ?: ""

            val request = UpdateProfileRequest(
                email = userData.email,
                firstName = userData.firstName,
                lastName = userData.lastName,
                username = userData.username,
                location = userData.location,
                token = token
            )

            val response = api.updateProfile(request)

            if (response.isSuccessful) {
                saveUserData(userData) // Save to local storage after successful server update
                true
            } else {
                saveUserData(userData) // Fallback to local storage
                true
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Exception updating profile", e)
            false
        }
    }

    // ============================================================================================
    // 6. Theme Preferences
    // ============================================================================================

    override suspend fun getTheme(): Int = getUserData().firstOrNull()?.themePreference ?: 0
    override suspend fun saveThemePreference(theme: Int) = preferences.saveThemePreference(theme)

    // ============================================================================================
    // 7. Notification Management
    // ============================================================================================

    override suspend fun setNotificationsEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
        preferences.setNotificationsEnabled(enabled)
    }

    override suspend fun areNotificationsEnabled(): Boolean = withContext(Dispatchers.IO) {
        preferences.areNotificationsEnabled()
    }

    override suspend fun saveNotificationToken(token: String) = withContext(Dispatchers.IO) {
        preferences.saveNotificationToken(token)
    }

    override suspend fun getNotificationToken(): String? = withContext(Dispatchers.IO) {
        preferences.getNotificationToken()
    }

    override suspend fun clearNotificationToken() = withContext(Dispatchers.IO) {
        preferences.clearNotificationToken()
    }

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
            return@withContext response.isSuccessful && response.body()?.status == "success"
        } catch (e: Exception) {
            Log.e("UserRepository", "Error registering notification loginToken", e)
            return@withContext false
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
            return@withContext response.isSuccessful && response.body()?.status == "success"
        } catch (e: Exception) {
            Log.e("UserRepository", "Error unregistering notification loginToken", e)
            return@withContext false
        }
    }
}