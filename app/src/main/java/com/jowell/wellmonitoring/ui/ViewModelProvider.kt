package com.jowell.wellmonitoring.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.jowell.wellmonitoring.data.WellDataStore

@Suppress("UNCHECKED_CAST")
class WellViewModelFactory(
    private val wellDataStore: WellDataStore
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WellViewModel::class.java)) {
            return WellViewModel(wellDataStore) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
