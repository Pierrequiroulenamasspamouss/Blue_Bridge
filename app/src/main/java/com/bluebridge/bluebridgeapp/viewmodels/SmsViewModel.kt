package com.bluebridge.bluebridgeapp.viewmodels

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bluebridge.bluebridgeapp.network.Location
import com.bluebridge.bluebridgeapp.network.SmsApi
import kotlinx.coroutines.launch

class SmsViewModel(
    private val smsApi: SmsApi
) : ViewModel() {
    private val _state = mutableStateOf<UiState<Unit>>(UiState.Empty)
    val state: State<UiState<Unit>> = _state

    private val _actionState = mutableStateOf<ActionState>(ActionState.Idle)
    val actionState: State<ActionState> = _actionState

    fun sendSms(command: String, location: Location? = null) {
        viewModelScope.launch {
            _actionState.value = ActionState.Loading
            try {
                smsApi.sendSms(command, location)
                _actionState.value = ActionState.Success("SMS sent successfully")
            } catch (e: Exception) {
                _actionState.value = ActionState.Error(e.message ?: "Error sending SMS")
            }
        }
    }

    companion object {
        fun provideFactory(
            smsApi: SmsApi
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SmsViewModel(smsApi) as T
            }
        }
    }
} 