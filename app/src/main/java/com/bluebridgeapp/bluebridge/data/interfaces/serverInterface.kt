package com.bluebridgeapp.bluebridge.data.interfaces

import com.bluebridgeapp.bluebridge.data.model.ServerStatusData

interface ServerRepository {
    suspend fun getServerStatus(): Result<ServerStatusData>
    val isServerReachable: Boolean
}

