package com.bluebridge.bluebridgeapp.data.repository

import com.bluebridge.bluebridgeapp.data.model.ServerStatusResponse
import com.bluebridge.bluebridgeapp.network.ServerApi

// ServerRepository.kt
interface ServerRepository {
    suspend fun getServerStatus(): Result<ServerStatusResponse>
    val isServerReachable: Boolean
}

class ServerRepositoryImpl(
    private val api: ServerApi
) : ServerRepository {
    private var _isServerReachable = true
    override val isServerReachable: Boolean get() = _isServerReachable

    override suspend fun getServerStatus(): Result<ServerStatusResponse> {
        return try {
            val response = api.getServerStatus()
            if (response.isSuccessful) {
                response.body()?.let {
                    _isServerReachable = true
                    Result.success(it)
                } ?: run {
                    _isServerReachable = false
                    Result.failure(Exception("Invalid server response"))
                }
            } else {
                _isServerReachable = false
                Result.failure(Exception("Server returned error: ${response.code()}"))
            }
        } catch (e: Exception) {
            _isServerReachable = false
            Result.failure(e)
        }
    }
}