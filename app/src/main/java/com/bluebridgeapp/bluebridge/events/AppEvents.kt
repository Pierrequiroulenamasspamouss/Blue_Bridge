package com.bluebridgeapp.bluebridge.events

import android.util.Log
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class AppEventHandler(
    private val coroutineScope: CoroutineScope,
    private val snackbarHostState: SnackbarHostState
) {
    // Mutable states for bug reporting
    private val showBugReportDialog = mutableStateOf(false)
    private val bugReportName = mutableStateOf("")
    private val bugReportDescription = mutableStateOf("")
    private val bugReportCategory = mutableStateOf("")
    private val bugReportExtra = mutableStateOf(emptyMap<String, String>())

    fun handleEvent(event: AppEvent) {
        when (event) {
            is AppEvent.ShowError -> showSnackbar(event.message)
            is AppEvent.ShowSuccess -> showSnackbar(event.message)
            is AppEvent.ShowInfo -> showSnackbar(event.message)
            is AppEvent.LogError -> logError(event.message)
            is AppEvent.LogSuccess -> logSuccess(event.message)
            is AppEvent.LogInfo -> logInfo(event.message)
            is AppEvent.SubmitBugReport -> {
                bugReportName.value = event.name
                bugReportDescription.value = event.description
                bugReportCategory.value = event.category
                bugReportExtra.value = event.extra
                showBugReportDialog.value = true
            }
        }
    }

    private fun showSnackbar(message: String) {
        coroutineScope.launch {
            try {
                // Clear previous snackbars to avoid overlap
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar(message)
            } catch (e: Exception) {
                Log.e("AppEventHandler", "Error showing snackbar: ${e.message}")
            }
        }
    }

    private fun logError(message: String) {
        Log.e("AppEventHandler", message)
    }

    private fun logSuccess(message: String) {
        Log.i("AppEventHandler-SUCCESS", message)
    }

    private fun logInfo(message: String) {
        Log.i("AppEventHandler-INFO", message)
    }

    private fun submitBugReport() {
        Log.i("AppEventHandler", "Bug Report Submitted:")
        Log.i("AppEventHandler", "Name: ${bugReportName.value}")
        Log.i("AppEventHandler", "Description: ${bugReportDescription.value}")
        Log.i("AppEventHandler", "Category: ${bugReportCategory.value}")
        Log.i("AppEventHandler", "Extra: ${bugReportExtra.value}")

        showSnackbar("Bug report submitted successfully!")

        resetBugReportFields()
    }

    private fun resetBugReportFields() {
        bugReportName.value = ""
        bugReportDescription.value = ""
        bugReportCategory.value = ""
        bugReportExtra.value = emptyMap()
    }

    @Composable
    fun BugReportDialog() {
        if (showBugReportDialog.value) {
            AlertDialog(
                onDismissRequest = {
                    showBugReportDialog.value = false
                    resetBugReportFields()
                },
                title = { Text("Confirm Bug Report Submission") },
                text = {
                    Text(
                        "Name: ${bugReportName.value}\n" +
                                "Description: ${bugReportDescription.value}\n" +
                                "Category: ${bugReportCategory.value}\n" +
                                "Extra: ${bugReportExtra.value}"
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            submitBugReport()
                            showBugReportDialog.value = false
                        }
                    ) {
                        Text("Submit")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showBugReportDialog.value = false
                            resetBugReportFields()
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}