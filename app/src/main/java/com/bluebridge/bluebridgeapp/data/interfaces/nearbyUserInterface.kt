package com.bluebridge.bluebridgeapp.data.interfaces

import com.bluebridge.bluebridgeapp.data.model.NearbyUser
import kotlinx.coroutines.flow.Flow


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




