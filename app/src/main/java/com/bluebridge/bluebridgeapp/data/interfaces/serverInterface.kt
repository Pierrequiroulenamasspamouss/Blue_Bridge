package com.bluebridge.bluebridgeapp.data.interfaces

import com.bluebridge.bluebridgeapp.data.model.ServerStatusData

interface ServerRepository {
    suspend fun getServerStatus(): Result<ServerStatusData>
    val isServerReachable: Boolean
}

