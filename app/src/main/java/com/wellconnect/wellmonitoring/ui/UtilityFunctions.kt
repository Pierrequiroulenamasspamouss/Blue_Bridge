package com.wellconnect.wellmonitoring.ui

import androidx.compose.runtime.MutableState
import com.wellconnect.wellmonitoring.data.WellDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

fun exchangeWells(fromIndex: Int, toIndex: Int, wellDataStore: WellDataStore, scope: CoroutineScope) {
    scope.launch {
        val currentList = wellDataStore.wellListFlow.first().toMutableList()
        if (fromIndex in currentList.indices && toIndex in currentList.indices) {
            val temp = currentList[fromIndex]
            currentList[fromIndex] = currentList[toIndex]
            currentList[toIndex] = temp

            wellDataStore.saveWellList(currentList)
        }
    }
}





fun resetErrorMessage(errorMessage: MutableState<String?>) {
    errorMessage.value = null
}
