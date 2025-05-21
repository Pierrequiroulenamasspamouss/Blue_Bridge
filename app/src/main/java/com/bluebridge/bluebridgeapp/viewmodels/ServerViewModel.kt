package com.bluebridge.bluebridgeapp.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bluebridge.bluebridgeapp.data.model.ServerStatusResponse
import com.bluebridge.bluebridgeapp.data.repository.ServerRepository
import com.caverock.androidsvg.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ServerState {
    object Loading : ServerState()
    data class Success(val status: ServerStatusResponse) : ServerState()
    data class Error(val message: String) : ServerState()
}

class ServerViewModel(
    private val repository: ServerRepository
) : ViewModel() {
    private val _serverState = MutableStateFlow<ServerState>(ServerState.Loading)
    val serverState: StateFlow<ServerState> = _serverState.asStateFlow()

    private val _needsUpdate = MutableStateFlow(false)
    val needsUpdate: StateFlow<Boolean> = _needsUpdate.asStateFlow()

    val isServerReachable: StateFlow<Boolean> =
        MutableStateFlow(repository.isServerReachable).asStateFlow()

    fun checkServerStatus() {
        viewModelScope.launch {
            _serverState.value = ServerState.Loading
            repository.getServerStatus()
                .onSuccess { statusResponse ->
                    _serverState.value = ServerState.Success(statusResponse)

                    // Check if update is needed
                    val appVersion = statusResponse.data.versions.mobile
                    val currentVersion = BuildConfig.VERSION_NAME
                    _needsUpdate.value = appVersion > currentVersion
                }
                .onFailure { e ->
                    _serverState.value = ServerState.Error(e.message ?: "Unknown error")
                }
        }
    }

    fun resetUpdateState() {
        _needsUpdate.value = false
    }
}