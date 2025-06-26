package com.bluebridgeapp.bluebridge.data.repository

import com.bluebridgeapp.bluebridge.data.interfaces.ServerRepository
import com.bluebridgeapp.bluebridge.data.model.ServerStatusData
import com.bluebridgeapp.bluebridge.network.ServerApi


class ServerRepositoryImpl(
    private val api: ServerApi
) : ServerRepository {
    private var _isServerReachable = true
    override val isServerReachable: Boolean get() = _isServerReachable

    /*This function's only purpose is to check if the server is reachable. not get the data */
    override suspend fun getServerStatus(): Result<ServerStatusData> {
        return try {
            val response = api.getServerStatus()
            if (response.isSuccessful) {
                response.body()?.let {
                    _isServerReachable = true
                    Result.success(it.data)

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
