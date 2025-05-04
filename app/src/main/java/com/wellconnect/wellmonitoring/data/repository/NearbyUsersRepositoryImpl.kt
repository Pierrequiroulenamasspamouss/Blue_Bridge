package com.wellconnect.wellmonitoring.data.repository

import android.util.Log
import com.wellconnect.wellmonitoring.data.`interface`.NearbyUsersRepository
import com.wellconnect.wellmonitoring.data.`interface`.UserRepository
import com.wellconnect.wellmonitoring.data.model.NearbyUser
import com.wellconnect.wellmonitoring.network.ServerApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

class NearbyUsersRepositoryImpl(
    private val api: ServerApi,
    private val userRepo: UserRepository
) : NearbyUsersRepository {

    private var currentLatitude: Double = 0.0
    private var currentLongitude: Double = 0.0
    private var currentRadius: Double = 50.0
    private val _nearbyUsers = MutableStateFlow<List<NearbyUser>>(emptyList())

    override suspend fun getNearbyUsers(
        latitude: Double,
        longitude: Double,
        radius: Double
    ): Result<List<NearbyUser>> {
        Log.d("NearbyUsersRepositoryImpl", "getNearbyUsers called with lat=$latitude, lon=$longitude, radius=$radius")
        
        // Save current search parameters
        currentLatitude = latitude
        currentLongitude = longitude
        currentRadius = radius
        
        return try {
            val userEmail = userRepo.getUserEmail()
            
            if (userEmail == null) {
                Log.e("NearbyUsersRepositoryImpl", "User not logged in")
                return Result.failure(Exception("User not logged in"))
            }

            Log.d("NearbyUsersRepositoryImpl", "Fetching nearby users at ($latitude, $longitude) with radius $radius km for user $userEmail")
            
            val response = api.getNearbyUsers(latitude, longitude, radius, userEmail)

            if (response.isSuccessful) {
                val responseBody = response.body()
                Log.d("NearbyUsersRepositoryImpl", "Response received: ${responseBody != null}")
                
                responseBody?.users?.let { users ->
                    Log.d("NearbyUsersRepositoryImpl", "Received ${users.size} nearby users")
                    
                    // Update the flow with new data
                    _nearbyUsers.value = users
                    return Result.success(users)
                } ?: run {
                    Log.e("NearbyUsersRepositoryImpl", "Empty response body")
                    // Don't clear users on empty response body
                    return Result.failure(Exception("Empty response body"))
                }
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Unknown error"
                Log.e("NearbyUsersRepositoryImpl", "Error response: ${response.code()} - $errorMessage")
                return Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e("NearbyUsersRepositoryImpl", "Exception in getNearbyUsers", e)
            return Result.failure(e)
        }
    }

    override suspend fun updateRadius(radius: Double): List<NearbyUser> = withContext(Dispatchers.IO) {
        Log.d("NearbyUsersRepositoryImpl", "Updating radius to $radius km")
        currentRadius = radius
        
        try {
            val result = getNearbyUsers(currentLatitude, currentLongitude, radius)
            return@withContext result.getOrNull() ?: _nearbyUsers.value
        } catch (e: Exception) {
            Log.e("NearbyUsersRepositoryImpl", "Error updating radius", e)
            return@withContext _nearbyUsers.value
        }
    }
    
    override suspend fun applyFilters(filters: Map<String, String>): List<NearbyUser> = withContext(Dispatchers.IO) {
        try {
            val currentUsers = _nearbyUsers.value
            Log.d("NearbyUsersRepositoryImpl", "Applying filters $filters to ${currentUsers.size} users")
            
            val filteredUsers = currentUsers.filter { user ->
                filters.all { (key, value) ->
                    when (key) {
                        "gender" -> user.javaClass.getDeclaredField("gender")?.let { field ->
                            field.isAccessible = true
                            (field.get(user) as? String)?.equals(value, ignoreCase = true) ?: true
                        } ?: true
                        else -> true
                    }
                }
            }

            _nearbyUsers.value = filteredUsers
            Log.d("NearbyUsersRepositoryImpl", "After filtering: ${filteredUsers.size} users")
            return@withContext filteredUsers
        } catch (e: Exception) {
            Log.e("NearbyUsersRepositoryImpl", "Error applying filters", e)
            return@withContext _nearbyUsers.value
        }
    }

    override suspend fun resetFilters(): List<NearbyUser> = withContext(Dispatchers.IO) {
        Log.d("NearbyUsersRepositoryImpl", "Resetting filters")
        try {
            val result = getNearbyUsers(currentLatitude, currentLongitude, currentRadius)
            return@withContext result.getOrNull() ?: _nearbyUsers.value
        } catch (e: Exception) {
            Log.e("NearbyUsersRepositoryImpl", "Error resetting filters", e)
            return@withContext _nearbyUsers.value
        }
    }

    override fun getNearbyUsersFlow(): Flow<List<NearbyUser>> = _nearbyUsers

    override suspend fun saveNearbyUsers(users: List<NearbyUser>) {
        Log.d("NearbyUsersRepositoryImpl", "Saving ${users.size} nearby users")
        _nearbyUsers.value = users
    }

    override suspend fun clearNearbyUsers() {
        Log.d("NearbyUsersRepositoryImpl", "Clearing nearby users")
        _nearbyUsers.value = emptyList()
    }

    override suspend fun updateNearbyUser(user: NearbyUser) = withContext(Dispatchers.Default) {
        Log.d("NearbyUsersRepositoryImpl", "Updating user ${user.userId}")
        _nearbyUsers.update { list ->
            list.map {
                if (it.userId == user.userId) user else it
            }
        }
    }

    override suspend fun deleteNearbyUser(userId: String) = withContext(Dispatchers.Default) {
        Log.d("NearbyUsersRepositoryImpl", "Deleting user $userId")
        _nearbyUsers.update { list ->
            list.filterNot { it.userId == userId }
        }
    }
}
