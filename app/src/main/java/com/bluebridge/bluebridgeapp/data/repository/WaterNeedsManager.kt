package com.bluebridge.bluebridgeapp.data.repository

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.navigation.NavController
import com.bluebridge.bluebridgeapp.data.model.WaterNeed
import com.bluebridge.bluebridgeapp.viewmodels.UserViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


class WaterNeedsManager(
    private val coroutineScope: CoroutineScope,
    private val snackbarHostState: SnackbarHostState,
    private val userViewModel: UserViewModel,
    private val navController: NavController
) {
    // UI State
    var isLoading by mutableStateOf(false)
    var showAddDialog by mutableStateOf(false)
    var showEditDialog by mutableStateOf(false)
    var showDeleteConfirmDialog by mutableStateOf(false)
    var editingNeedIndex by mutableStateOf(-1)
    var deletingNeedIndex by mutableStateOf(-1)

    // Form State
    var newAmount by mutableStateOf("")
    var newAmountSlider by mutableFloatStateOf(50f)
    var newUsageType by mutableStateOf("")
    var newDescription by mutableStateOf("")
    var newPriority by mutableStateOf(3)
    var customType by mutableStateOf("")

    // Data State
    val waterNeeds = mutableStateListOf<WaterNeed>()

    // CRUD Operations
    fun addWaterNeed() {
        val amount = newAmount.toIntOrNull() ?: 0
        when {
            amount <= 0 -> showError("Please enter a valid amount")
            newUsageType.isBlank() -> showError("Please select a usage type")
            else -> {
                waterNeeds.add(createWaterNeed(amount))
                resetForm()
                showAddDialog = false
            }
        }
    }

    fun updateWaterNeed(index: Int) {
        val amount = newAmount.toIntOrNull() ?: 0
        when {
            amount <= 0 -> showError("Please enter a valid amount")
            newUsageType.isBlank() -> showError("Please select a usage type")
            else -> {
                waterNeeds[index] = createWaterNeed(amount)
                resetForm()
                editingNeedIndex = -1
                showEditDialog = false
            }
        }
    }

    fun deleteWaterNeed(index: Int) {
        deletingNeedIndex = index
        showDeleteConfirmDialog = true
    }

    fun confirmDelete() {
        waterNeeds.removeAt(deletingNeedIndex)
        showDeleteConfirmDialog = false
    }

    fun saveAll() {
        isLoading = true
        coroutineScope.launch {
            try {
                userViewModel.updateWaterNeeds(waterNeeds.toList())
                showSuccess("Water needs updated successfully")
                navController.popBackStack()
            } catch (e: Exception) {
                showError("Error: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    fun prepareEdit(index: Int) {
        waterNeeds[index].let { need ->
            newAmount = need.amount.toString()
            newAmountSlider = need.amount.toFloat()
            newUsageType = when (need.usageType) {
                in listOf("Absolute emergency", "Medical", "Drinking", "Farming", "Industry") -> need.usageType
                else -> "Other"
            }
            newDescription = need.description
            newPriority = need.priority
            customType = if (newUsageType == "Other") need.usageType else ""
            editingNeedIndex = index
            showEditDialog = true
        }
    }

    // Private Helpers
    private fun createWaterNeed(amount: Int): WaterNeed {
        return WaterNeed(
            amount = amount,
            usageType = if (newUsageType == "Other") customType else newUsageType,
            description = newDescription,
            priority = newPriority
        )
    }

    private fun resetForm() {
        newAmount = ""
        newAmountSlider = 50f
        newUsageType = ""
        newDescription = ""
        newPriority = 3
        customType = ""
    }

    private fun showError(message: String) {
        coroutineScope.launch { snackbarHostState.showSnackbar(message) }
    }

    private fun showSuccess(message: String) {
        coroutineScope.launch { snackbarHostState.showSnackbar(message) }
    }
}