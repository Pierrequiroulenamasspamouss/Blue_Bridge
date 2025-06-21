package com.bluebridge.bluebridgeapp.data.repository

import android.util.Log
import com.bluebridge.bluebridgeapp.data.interfaces.NearbyUsersRepository
import com.bluebridge.bluebridgeapp.data.interfaces.UserRepository
import com.bluebridge.bluebridgeapp.data.model.NearbyUser
import com.bluebridge.bluebridgeapp.data.model.NearbyUsersRequest
import com.bluebridge.bluebridgeapp.network.ServerApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

class NearbyUsersRepositoryImpl(
    private val api: ServerApi,
    private val userRepo: UserRepository
) : NearbyUsersRepository {

    private var currentLatitude = 0.0
    private var currentLongitude = 0.0
    private var currentRadius = 50.0
    private val _nearbyUsers = MutableStateFlow<List<NearbyUser>>(emptyList())

    override suspend fun getNearbyUsers(
        latitude: Double,
        longitude: Double,
        radius: Double
    ): Result<List<NearbyUser>> = withContext(Dispatchers.IO) {
        Log.d("NearbyRepo", "getNearbyUsers: lat=$latitude, lon=$longitude, radius=$radius")

        currentLatitude = latitude
        currentLongitude = longitude
        currentRadius = radius

        val userId = userRepo.getUserId()
        val token = userRepo.getLoginToken() ?: return@withContext Result.failure(Exception("Authentication loginToken required"))

        runCatching {
            val request = NearbyUsersRequest(
                latitude = latitude,
                longitude = longitude,
                radius = radius,
                userId = userId,
                loginToken = token
            )
            val response = api.getNearbyUsers(request)
            if (response.isSuccessful) {
                response.body()?.data.also { users ->
                        _nearbyUsers.value = users ?: emptyList()
                        Log.d("NearbyRepo", "Received ${users?.size ?: 0} users")
                    }
                    ?: throw Exception("Response body is null")
            } else {
                throw Exception(response.errorBody()?.string() ?: "Unknown error")
                }


        }
    }

    override fun getNearbyUsersFlow(): Flow<List<NearbyUser>> {
        TODO("Not yet implemented")
    }

    override suspend fun updateRadius(radius: Double): List<NearbyUser> {
        currentRadius = radius
        return getNearbyUsers(currentLatitude, currentLongitude, radius).getOrElse { _nearbyUsers.value }
    }

    override suspend fun applyFilters(filters: Map<String, String>): List<NearbyUser> {
        return _nearbyUsers.value.filter { user ->
            filters.all { (key, value) ->
                TODO("Implement filter logic for $key with value $value")
            }
        }.also {
            _nearbyUsers.value = it
            Log.d("NearbyRepo", "Filtered to ${it.size} users")
        }
    }

    override suspend fun resetFilters() = updateRadius(currentRadius)

    override suspend fun saveNearbyUsers(users: List<NearbyUser>) {
        _nearbyUsers.value = users
    }

    override suspend fun clearNearbyUsers() {
        _nearbyUsers.value = emptyList()
    }

    override suspend fun updateNearbyUser(user: NearbyUser) {
        _nearbyUsers.update { list -> list.map { if (it.userId == user.userId) user else it } }
    }

    override suspend fun deleteNearbyUser(userId: String) {
        _nearbyUsers.update { list -> list.filterNot { it.userId == userId } }
    }
}