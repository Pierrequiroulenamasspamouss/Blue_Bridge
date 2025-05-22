package com.bluebridge.bluebridgeapp.data.repository

import android.util.Log
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


class UserRepositoryImpl(
    private val api: ServerApi,
    private val preferences: UserPreferences
) : UserRepository {

    override suspend fun register(request: RegisterRequest): Boolean = withContext(Dispatchers.IO) {
        try {
            val response = api.register(request)
            if (response.isSuccessful && response.body()?.status == "success") {
                val userDataResponse = response.body()!!.userData
                val userData = UserData(email = request.email, userId = userDataResponse!!.userId, firstName = request.firstName, lastName = request.lastName, username = request.username, role = request.role, location = request.location, themePreference = request.themePreference, waterNeeds = request.waterNeeds, loginToken = userDataResponse.loginToken)
                Log.d("UserRepository", "Registration successful: $userData")
                saveUserData(userData)
                true
            } else {
                Log.e("UserRepository", "Registration failed: ${response.body()?.message}")
                false
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Registration failed", e)
            false
        }
    }
    override suspend fun login(request: LoginRequest): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d("UserRepository", "Attempting login with email: ${request.email}")
            val response = api.login(request)
            
            if (response.isSuccessful && response.body()?.status == "success") {
                val userDataResponse = response.body()!!.data
                Log.d("UserRepository", "Login successful for ${request.email}, received userData: $userDataResponse")
                
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
                
                Log.d("UserRepository", "Saving user data after login: $userData")
                saveUserData(userData)
                
                // Verify data was saved
                val isLoggedIn = isLoggedIn()
                Log.d("UserRepository", "Verified login state after save: isLoggedIn=$isLoggedIn")
                
                true
            } else {
                val errorCode = response.code()
                val errorBody = response.errorBody()?.string()
                Log.e("UserRepository", "Login failed with code $errorCode: $errorBody")
                false
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Login failed with exception", e)
            false
        }
    }
    override suspend fun getRole(): String = getUserData().firstOrNull()?.role.toString()
    override suspend fun getRoleValue(): Int {
        return getRole().let { role ->
            // Define user roles
            val guestRole = 1
            val userRole = 2
            val wellOwnerRole = 3
            val adminRole = 4

            when (role) {
                "guest" -> guestRole
                "user" -> userRole
                "well_owner" -> wellOwnerRole
                "admin" -> adminRole
                else -> 0 // Default or unknown role

            }
        }
    }
    override suspend fun getUserId(): String = getUserData().firstOrNull()?.userId.toString()
    override suspend fun getUserData(): Flow<UserData?> = preferences.getUserData()
    override suspend fun getLoginToken(): String? = getUserData().firstOrNull()?.loginToken
    override suspend fun getUserEmail(): String? = getUserData().firstOrNull()?.email
    override suspend fun getUserWaterNeeds(): String? = getUserData().firstOrNull()?.waterNeeds.toString()
    override suspend fun setUserWaterNeeds(waterNeeds: String) = preferences.setUserWaterNeeds(waterNeeds)
    override suspend fun getTheme(): Int = getUserData().firstOrNull()?.themePreference ?: 0
    override suspend fun isLoggedIn(): Boolean {
        val result = getUserData().firstOrNull()?.loginToken != null
        Log.d("UserRepository", "Checking login state: isLoggedIn=$result")
        return result
    }


    override suspend fun clearUserData() {
        Log.d("UserRepository", "Clearing user data from DataStore")
        try {
            preferences.clearUserData()
            Log.d("UserRepository", "User data successfully cleared")
        } catch (e: Exception) {
            Log.e("UserRepository", "Error clearing user data", e)
            throw e
        }
    }

    override suspend fun saveUserData(userData: UserData) = preferences.saveUserData(userData)

    override suspend fun logout() {
        clearUserData()
    }

    override suspend fun deleteAccount(deleteRequest: DeleteAccountRequest): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d("UserRepository", "Attempting to delete account for: ${deleteRequest.email}")
            
            val response = api.deleteAccount(deleteRequest)
            
            if (response.isSuccessful && response.body()?.status == "success") {
                Log.d("UserRepository", "Account deleted successfully for: ${deleteRequest.email}")
                // Clear local user data
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

    override suspend fun updateThemePreference(theme: Int) {
        preferences.updateThemePreference(theme)
    }

    override suspend fun updateProfileOnServer(userData: UserData): Boolean = withContext(Dispatchers.IO) {
        try {
            val token = getLoginToken() ?: ""
            Log.d("UserRepository", "Preparing to update profile on server for: ${userData.email}")
            Log.d("UserRepository", "Profile data: firstName=${userData.firstName}, lastName=${userData.lastName}, username=${userData.username}")
            Log.d("UserRepository", "Location data: lat=${userData.location.latitude}, lon=${userData.location.longitude}")
            
            val request = UpdateProfileRequest(
                email = userData.email,
                firstName = userData.firstName,
                lastName = userData.lastName,
                username = userData.username,
                location = userData.location,
                token = token
            )
            
            Log.d("UserRepository", "Sending profile update request to server: $request")
            val response = api.updateProfile(request)
            
            if (response.isSuccessful) {
                Log.d("UserRepository", "Profile update response success: ${response.body()}")
                // Save to local storage after successful server update
                saveUserData(userData)
                Log.d("UserRepository", "Profile data saved locally after server update")
                true
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Log.e("UserRepository", "Failed to update profile on server. Status: ${response.code()}, Error: $errorBody")
                // Still save locally to avoid data loss
                saveUserData(userData)
                Log.d("UserRepository", "Profile data saved locally despite server update failure")
                true // Return true so UI doesn't show error, since we saved locally
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Exception updating profile on server", e)
            // Still save locally to avoid data loss
            try {
                saveUserData(userData)
                Log.d("UserRepository", "Profile data saved locally after exception")
                true // Return true so UI doesn't show error, since we saved locally
            } catch (saveException: Exception) {
                Log.e("UserRepository", "Failed to save profile locally after server exception", saveException)
                false
            }
        }
    }
    
    override suspend fun updateWaterNeedsOnServer(waterNeeds: List<WaterNeed>): Boolean = withContext(Dispatchers.IO) {
        try {
            val email = getUserEmail() ?: return@withContext false
            val token = getLoginToken() ?: ""
            
            Log.d("UserRepository", "Preparing to update water needs on server for: $email")
            Log.d("UserRepository", "Water needs data: ${waterNeeds.joinToString { "${it.amount}L ${it.usageType} (P${it.priority})" }}")
            
            val request = UpdateWaterNeedsRequest(
                email = email,
                waterNeeds = waterNeeds,
                token = token
            )
            
            Log.d("UserRepository", "Sending water needs update request to server: $request")
            val response = api.updateWaterNeeds(request)
            
            if (response.isSuccessful) {
                Log.d("UserRepository", "Water needs update response success: ${response.body()}")
                // Get current user data and update water needs
                val userData = getUserData().first()
                userData?.let {
                    val updatedUserData = it.copy(waterNeeds = waterNeeds)
                    saveUserData(updatedUserData)
                    Log.d("UserRepository", "User data with updated water needs saved locally")
                }
                true
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Log.e("UserRepository", "Failed to update water needs on server. Status: ${response.code()}, Error: $errorBody")
                // Still update locally to avoid data loss
                val userData = getUserData().first()
                userData?.let {
                    val updatedUserData = it.copy(waterNeeds = waterNeeds)
                    saveUserData(updatedUserData)
                    Log.d("UserRepository", "User data with water needs saved locally despite server update failure")
                }
                true // Return true so UI doesn't show error
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Exception updating water needs on server", e)
            // Still update locally
            try {
                val userData = getUserData().first()
                userData?.let {
                    val updatedUserData = it.copy(waterNeeds = waterNeeds)
                    saveUserData(updatedUserData)
                    Log.d("UserRepository", "User data with water needs saved locally after exception")
                }
                true // Return true so UI doesn't show error
            } catch (saveException: Exception) {
                Log.e("UserRepository", "Failed to save water needs locally after server exception", saveException)
                false
            }
        }
    }

    // Push notification methods
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
        email: String, 
        authToken: String, 
        fcmToken: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d("UserRepository", "Registering notification token for $email")
            
            val request = NotificationTokenRequest(
                email = email,
                token = authToken,
                deviceToken = fcmToken
            )
            
            val response = api.registerNotificationToken(request)
            
            if (response.isSuccessful && response.body()?.status == "success") {
                Log.d("UserRepository", "Successfully registered notification token")
                true
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("UserRepository", "Failed to register notification token: $errorBody")
                false
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Error registering notification token", e)
            false
        }
    }

    override suspend fun unregisterNotificationToken(
        email: String, 
        authToken: String, 
        fcmToken: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d("UserRepository", "Unregistering notification token for $email")
            
            val request = NotificationTokenRequest(
                email = email,
                token = authToken,
                deviceToken = fcmToken
            )
            
            val response = api.unregisterNotificationToken(request)
            
            if (response.isSuccessful && response.body()?.status == "success") {
                Log.d("UserRepository", "Successfully unregistered notification token")
                true
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("UserRepository", "Failed to unregister notification token: $errorBody")
                false
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Error unregistering notification token", e)
            false
        }
    }
}