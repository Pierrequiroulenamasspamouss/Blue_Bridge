package com.wellconnect.wellmonitoring.data.`interface`

import UserData
import WellData
import com.wellconnect.wellmonitoring.data.model.DeleteAccountRequest
import com.wellconnect.wellmonitoring.data.model.LoginRequest
import com.wellconnect.wellmonitoring.data.model.NearbyUser
import com.wellconnect.wellmonitoring.data.model.RegisterRequest
import com.wellconnect.wellmonitoring.data.model.WaterNeed
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
}

interface NearbyUsersRepository {
    suspend fun getNearbyUsers(latitude: Double, longitude: Double, radius: Double): Result<List<NearbyUser>>
    fun getNearbyUsersFlow(): Flow<List<NearbyUser>>
    suspend fun saveNearbyUsers(users: List<NearbyUser>)
    suspend fun clearNearbyUsers()
    suspend fun updateNearbyUser(user: NearbyUser)
    suspend fun deleteNearbyUser(userId: String)
    suspend fun updateRadius(radius: Double): List<NearbyUser>
    suspend fun applyFilters(filters: Map<String, String>): Any
    suspend fun resetFilters(): List<NearbyUser>


}

interface WellRepository {
    suspend fun saveWell(well: WellData)
    suspend fun saveWellList(wells: List<WellData>)
    suspend fun getWell(wellId: Int): WellData?
    suspend fun getWells(): List<WellData>
    suspend fun deleteWellAt(index: Int)
    suspend fun swapWells(from: Int, to: Int)
    suspend fun isEspIdUnique(id: String, currentId: Int): Boolean
    val wellListFlow: Flow<List<WellData>>
}



