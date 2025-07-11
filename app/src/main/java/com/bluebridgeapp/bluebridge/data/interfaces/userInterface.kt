package com.bluebridgeapp.bluebridge.data.interfaces

import com.bluebridgeapp.bluebridge.data.model.DeleteAccountRequest
import com.bluebridgeapp.bluebridge.data.model.Location
import com.bluebridgeapp.bluebridge.data.model.LoginRequest
import com.bluebridgeapp.bluebridge.data.model.RegisterRequest
import com.bluebridgeapp.bluebridge.data.model.UserData
import com.bluebridgeapp.bluebridge.data.model.WaterNeed
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    suspend fun getLanguage(): String
    suspend fun saveThemePreference(theme: Int)
    suspend fun getRole(): String
    suspend fun getRoleValue(): Int
    suspend fun getUserId(): String
    suspend fun getUserData(): Flow<UserData?>
    suspend fun getUserEmail(): String?
    suspend fun getUserWaterNeeds(): String?
    suspend fun getLoginToken(): String?
    suspend fun getTheme(): Int

    suspend fun register(registerRequest: RegisterRequest): String
    suspend fun login(loginRequest: LoginRequest): Boolean
    suspend fun setUserWaterNeeds(waterNeeds: List<WaterNeed>)
    suspend fun clearUserData()
    suspend fun isLoggedIn(): Boolean

    suspend fun logout()

    suspend fun saveUserData(userData: UserData)
    suspend fun updateProfileOnServer(userData: UserData): Boolean
    suspend fun updateWaterNeedsOnServer(waterNeeds: List<WaterNeed>): Boolean
    suspend fun deleteAccount(deleteRequest: DeleteAccountRequest): Boolean
    suspend fun setNotificationsEnabled(enabled: Boolean)
    suspend fun areNotificationsEnabled(): Boolean
    suspend fun saveNotificationToken(token: String)
    suspend fun getNotificationToken(): String?
    suspend fun clearNotificationToken()
    suspend fun registerNotificationToken(userId: String, authToken: String, fcmToken: String): Boolean
    suspend fun unregisterNotificationToken(userId: String, authToken: String, fcmToken: String): Boolean
    suspend fun updateLocation(location: Location): Boolean
    suspend fun setLanguage(language: String)
}