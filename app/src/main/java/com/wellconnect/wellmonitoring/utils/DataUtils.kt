package com.wellconnect.wellmonitoring.utils

//TODO : DO NOT USE THIS PACKAGE IT IS OBSOLETE

import WellData
import androidx.compose.runtime.MutableState
import com.wellconnect.wellmonitoring.data.repository.WellRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

fun persistWellData(
    wellId: Int,
    data: WellData,
    wellDataStore: WellRepositoryImpl,
    scope: CoroutineScope,
    errorMessage: MutableState<String?>,
    lastSavedData: MutableState<WellData>,
    wellData: MutableState<WellData>
) {
    scope.launch {
        try {
            // Validate input data
            if (wellId <= 0) {
                errorMessage.value = "Invalid well ID"
                return@launch
            }



            // Create updated data with proper ID
            val currentData = data.copy(id = wellId)

            // Save to data store
            wellDataStore.saveWell(currentData)

            // Update state
            lastSavedData.value = currentData
            wellData.value = currentData
            errorMessage.value = null

        } catch (e: Exception) {
            errorMessage.value = "Save failed: ${e.localizedMessage ?: "Unknown error"}"
        }
    }
}

fun resetWellDataState(
    wellData: MutableState<WellData>,
    lastSavedData: MutableState<WellData>,
    wellLoaded: MutableState<Boolean>
) {
    // Only reset if there's no ID (new well)
    if (wellData.value.id == 0) {
        wellData.value = WellData()
        lastSavedData.value = WellData()
    }
    wellLoaded.value = false
}

fun restorePreviousWellData(
    wellData: MutableState<WellData>,
    lastSavedData: MutableState<WellData>
) {
    wellData.value = lastSavedData.value
}

fun removeWellByIndex(index: Int, wellDataStore: WellRepositoryImpl, scope: CoroutineScope) {
    scope.launch {
        val currentList = wellDataStore.wellListFlow.first().toMutableList()
        if (index in currentList.indices) {
            val wellToDelete = currentList[index]
            val updatedList = currentList.filterNot { it.id == wellToDelete.id }
            wellDataStore.saveWellList(updatedList)
        }
    }
}


fun loadWellData(wellId: Int, wellDataStore: WellRepositoryImpl, scope: CoroutineScope, wellData: MutableState<WellData>, lastSavedData: MutableState<WellData>, wellLoaded: MutableState<Boolean>) {
    scope.launch {
        val data = wellDataStore.getWell(wellId) ?: WellData(id = wellId)
        wellData.value = data
        lastSavedData.value = data
        wellLoaded.value = true // Ensure loading is marked as complete
    }
}

