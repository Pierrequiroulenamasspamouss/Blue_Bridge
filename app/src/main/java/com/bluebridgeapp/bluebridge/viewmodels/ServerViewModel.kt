package com.bluebridgeapp.bluebridge.viewmodels

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bluebridgeapp.bluebridge.data.interfaces.ServerRepository
import com.bluebridgeapp.bluebridge.data.model.ServerStatusData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ServerState {
    object Loading : ServerState()
    object Online : ServerState()
    data class Error(val message: String) : ServerState()
}

class ServerViewModel(
    private val repository: ServerRepository,
    private val context: Context
) : ViewModel() {
    private val _serverState = MutableStateFlow<ServerState>(ServerState.Loading)
    val serverState: StateFlow<ServerState> = _serverState.asStateFlow()

    private val _needsUpdate = MutableStateFlow(false)
    val needsUpdate: StateFlow<Boolean> = _needsUpdate.asStateFlow()

    private val _isServerReachable = MutableStateFlow(false)
    val isServerReachable: StateFlow<Boolean> = _isServerReachable.asStateFlow()

    fun checkServerReachability() {
        viewModelScope.launch {
            _serverState.value = ServerState.Loading
            try {
                val response = repository.getServerStatus()

                if (response.isSuccess) {
                    val serverStatus = response.getOrNull()
                    if (serverStatus != null) {
                        handleSuccessfulResponse(serverStatus)
                    } else {
                        handleError("Invalid server response")
                    }
                } else {
                    handleError(response.exceptionOrNull()?.message ?: "Unknown error")
                }
            } catch (e: Exception) {
                Log.e("ServerViewModel", "Error checking server reachability", e)
                handleError(e.message ?: "Unknown error")
            }
        }
    }

    private fun handleSuccessfulResponse(serverStatus: ServerStatusData) {
        val serverVersion = serverStatus.versions.mobile
        val currentVersion = getCurrentVersion()

        _serverState.value = ServerState.Online
        _isServerReachable.value = true

        // Check if update is needed
        _needsUpdate.value = compareVersions(currentVersion, serverVersion) < 0
    }

    private fun handleError(message: String) {
        _serverState.value = ServerState.Error(message)
        _isServerReachable.value = false
        _needsUpdate.value = false
    }

    fun setServerUnreachable() {
        _isServerReachable.value = false
        _serverState.value = ServerState.Error("Server unreachable")
    }

    fun resetUpdateState() {
        _needsUpdate.value = false
    }

    private fun getCurrentVersion(): String {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e("ServerViewModel", "Error getting app version", e)
            "0.0.0"
        }.toString()
    }

    private fun compareVersions(current: String, server: String): Int {
        val currentParts = current.split(".").map { it.toIntOrNull() ?: 0 }
        val serverParts = server.split(".").map { it.toIntOrNull() ?: 0 }

        for (i in 0 until maxOf(currentParts.size, serverParts.size)) {
            val currentPart = currentParts.getOrElse(i) { 0 }
            val serverPart = serverParts.getOrElse(i) { 0 }

            when {
                currentPart < serverPart -> return -1
                currentPart > serverPart -> return 1
            }
        }
        return 0
    }
}