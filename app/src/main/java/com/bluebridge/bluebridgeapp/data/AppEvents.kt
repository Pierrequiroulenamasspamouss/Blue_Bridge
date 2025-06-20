package com.bluebridge.bluebridgeapp.data

import android.util.Log
import androidx.compose.material3.SnackbarHostState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class AppEventHandler(
    private val coroutineScope: CoroutineScope,
    private val snackbarHostState: SnackbarHostState
) {
    fun handleEvent(event: AppEvent) {
        when (event) {
            is AppEvent.ShowError -> showSnackbar(event.message)
            is AppEvent.ShowSuccess -> showSnackbar(event.message)
            is AppEvent.ShowInfo -> showSnackbar(event.message)
            is AppEvent.LogError -> logError(event.message)
            is AppEvent.LogSuccess -> logSuccess(event.message)
            is AppEvent.LogInfo -> logInfo(event.message)
        }
    }

    private fun showSnackbar(message: String) {
        coroutineScope.launch {
            // Clear previous snackbars to avoid overlap
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message)
        }
    }

    private fun logError(message: String) {
        Log.e("AppEventHandler", message)
    }

    private fun logSuccess(message: String) {
        Log.i("AppEventHandler", message)
    }

    private fun logInfo(message: String) {
        Log.i("AppEventHandler", message)
    }
}
