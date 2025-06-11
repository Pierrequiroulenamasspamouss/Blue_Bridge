package com.bluebridge.bluebridgeapp.viewmodels

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bluebridge.bluebridgeapp.data.`interface`.ServerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ServerState {
    object Loading : ServerState()
    data class Success(val message: String, val version: String) : ServerState()
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


    fun getServerStatus(){
        viewModelScope.launch {
            try {
                _serverState.value = ServerState.Loading
                val response = repository.getServerStatus()
                if (response.isSuccess) {
                    val serverStatus = response.getOrNull()
                    if (serverStatus != null) {
                        val serverVersion = serverStatus.versions.mobile
                        _serverState.value = ServerState.Success(serverStatus.message, serverVersion)

                        _isServerReachable.value = true

                        // Check for updates
                        val currentVersion = getCurrentVersion()
                        _needsUpdate.value = compareVersions(currentVersion, serverVersion) < 0
                    } else {
                        _serverState.value = ServerState.Error("Invalid server response")
                        _isServerReachable.value = false
                    }
                } else {
                    _serverState.value = ServerState.Error(response.exceptionOrNull()?.message ?: "Unknown error")
                    _isServerReachable.value = false
                }
            } catch (e: Exception) {
                Log.e("ServerViewModel", "Error checking server status: ${e.message}", e)
                _serverState.value = ServerState.Error(e.message ?: "Unknown error")
                _isServerReachable.value = false
            }
        }
    }

    private fun getCurrentVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e("ServerViewModel", "Error getting current app version: ${e.message}", e)
            "0.0.0"
        }.toString()
    }

    private fun compareVersions(current: String, server: String): Int {
        val currentParts = current.split(".").map { it.toIntOrNull() ?: 0 }
        val serverParts = server.split(".").map { it.toIntOrNull() ?: 0 }

        // Compare each part of the version
        for (i in 0 until maxOf(currentParts.size, serverParts.size)) {
            val currentPart = currentParts.getOrNull(i) ?: 0
            val serverPart = serverParts.getOrNull(i) ?: 0

            when {
                currentPart < serverPart -> return -1  // Current version is older
                currentPart > serverPart -> return 1   // Current version is newer
            }
        }
        return 0  // Versions are equal
    }

    fun resetUpdateState() {
        _needsUpdate.value = false
    }
}