package com.bluebridge.bluebridgeapp.data.interfaces

import com.bluebridge.bluebridgeapp.data.model.DeleteAccountRequest
import com.bluebridge.bluebridgeapp.data.model.Location
import com.bluebridge.bluebridgeapp.data.model.LoginRequest
import com.bluebridge.bluebridgeapp.data.model.RegisterRequest
import com.bluebridge.bluebridgeapp.data.model.UserData
import com.bluebridge.bluebridgeapp.data.model.WaterNeed
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

interface UserRepository {
    suspend fun saveThemePreference(theme: Int)
    suspend fun getRole(): String
    suspend fun getRoleValue(): Int
    suspend fun getUserId(): String
    suspend fun register(registerRequest: RegisterRequest): String
    suspend fun login(loginRequest: LoginRequest): Boolean
    suspend fun getUserData(): Flow<UserData?>
    suspend fun getUserEmail(): String?
    suspend fun getUserWaterNeeds(): String?
    suspend fun setUserWaterNeeds(waterNeeds: String)
    suspend fun clearUserData()
    suspend fun getLoginToken(): String?
    suspend fun isLoggedIn(): Boolean
    suspend fun saveUserData(userData: UserData)
    suspend fun logout()
    suspend fun getTheme(): Int
    suspend fun updateProfileOnServer(userData: UserData): Boolean
    suspend fun updateWaterNeedsOnServer(waterNeeds: List<WaterNeed>): Boolean
    suspend fun deleteAccount(deleteRequest: DeleteAccountRequest): Boolean

    // Push notification methods
    suspend fun setNotificationsEnabled(enabled: Boolean)
    suspend fun areNotificationsEnabled(): Boolean
    suspend fun saveNotificationToken(token: String)
    suspend fun getNotificationToken(): String?
    suspend fun clearNotificationToken()
    suspend fun registerNotificationToken(email: String, authToken: String, fcmToken: String): Boolean
    suspend fun unregisterNotificationToken(email: String, authToken: String, fcmToken: String): Boolean
    suspend fun updateLocation(location: Location): Boolean {
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
}