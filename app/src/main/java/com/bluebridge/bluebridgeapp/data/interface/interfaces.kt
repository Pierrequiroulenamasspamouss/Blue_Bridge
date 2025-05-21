package com.bluebridge.bluebridgeapp.data.`interface`

import PaginatedWellsResponse
import ShortenedWellData
import com.bluebridge.bluebridgeapp.data.model.UserData
import WellData
import com.bluebridge.bluebridgeapp.data.model.DeleteAccountRequest
import com.bluebridge.bluebridgeapp.data.model.LoginRequest
import com.bluebridge.bluebridgeapp.data.model.NearbyUser
import com.bluebridge.bluebridgeapp.data.model.RegisterRequest
import com.bluebridge.bluebridgeapp.data.model.WaterNeed
import kotlinx.coroutines.flow.Flow
import java.io.Serializable

interface UserRepository {
    suspend fun register(registerRequest: RegisterRequest): Boolean
    suspend fun login(loginRequest: LoginRequest): Boolean
    suspend fun getUserData(): Flow<UserData?>
    suspend fun getUserEmail(): String?
    suspend fun getUserWaterNeeds(): String?
    suspend fun setUserWaterNeeds(waterNeeds: String)
    suspend fun clearUserData()
    suspend fun getLoginToken(): String?
    suspend fun getAuthToken(): String?
    suspend fun isLoggedIn(): Boolean
    suspend fun saveUserData(userData: UserData)
    suspend fun logout()
    suspend fun updateThemePreference(theme: Int)
    suspend fun getTheme(): Int
    suspend fun updateProfileOnServer(userData: UserData): Boolean
    suspend fun updateWaterNeedsOnServer(waterNeeds: List<WaterNeed>): Boolean
    suspend fun deleteAccount(deleteRequest: DeleteAccountRequest): Boolean
    
    // Guest mode methods
    suspend fun setGuestMode(isGuest: Boolean)
    suspend fun isGuestMode(): Boolean
    
    // Push notification methods
    suspend fun setNotificationsEnabled(enabled: Boolean)
    suspend fun areNotificationsEnabled(): Boolean
    suspend fun saveNotificationToken(token: String)
    suspend fun getNotificationToken(): String?
    suspend fun clearNotificationToken()
    suspend fun registerNotificationToken(email: String, authToken: String, fcmToken: String): Boolean
    suspend fun unregisterNotificationToken(email: String, authToken: String, fcmToken: String): Boolean
}

interface NearbyUsersRepository {
    suspend fun getNearbyUsers(latitude: Double, longitude: Double, radius: Double): Result<List<NearbyUser>>
    fun getNearbyUsersFlow(): Flow<List<NearbyUser>>
    suspend fun saveNearbyUsers(users: List<NearbyUser>)
    suspend fun clearNearbyUsers()
    suspend fun updateNearbyUser(user: NearbyUser)
    suspend fun deleteNearbyUser(userId: String)
    suspend fun updateRadius(radius: Double): List<NearbyUser>
    suspend fun applyFilters(filters: Map<String, String>): List<NearbyUser>
    suspend fun resetFilters(): List<NearbyUser>
}

interface WellRepository {
    val wellListFlow: Flow<List<WellData>>
    suspend fun getWells(): List<WellData>
    suspend fun getWellById(espId: String): WellData
    suspend fun getAllWells(): List<ShortenedWellData>
    suspend fun getFilteredWells(
        page: Int = 1,
        limit: Int = 20,
        wellName: String? = null,
        wellStatus: String? = null,
        wellWaterType: String? = null,
        wellOwner: String? = null,
        espId: String? = null,
        minWaterLevel: Int? = null,
        maxWaterLevel: Int? = null
    ): Result<PaginatedWellsResponse>
    suspend fun saveWell(well: WellData): Boolean
    suspend fun saveWellList(wells: List<WellData>)
    suspend fun getWell(wellId: Int): WellData?
    suspend fun deleteWell(espId: String): Boolean
    suspend fun getFavoriteWells(): Flow<List<ShortenedWellData>>
    suspend fun addFavoriteWell(well: ShortenedWellData)
    suspend fun removeFavoriteWell(espId: String)
    suspend fun isWellFavorite(espId: String): Boolean
    suspend fun getStats(): Flow<List<Serializable>>
    suspend fun isEspIdUnique(espId: String): Boolean
    suspend fun swapWells(from: Int, to: Int)
    suspend fun deleteWellAt(index: Int)
    suspend fun saveWellToServer(wellData: WellData, email: String, token: String): Boolean
}



