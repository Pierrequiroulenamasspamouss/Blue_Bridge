package com.bluebridge.bluebridgeapp.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.bluebridge.bluebridgeapp.data.model.WellData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val Context.wellDataStore: DataStore<Preferences> by preferencesDataStore(name = "well_preferences")
class WellPreferences(val context: Context) {
    companion object {
        val FAVORITE_WELLS_KEY = stringPreferencesKey("favorite_wells")
        val WELLS_KEY = stringPreferencesKey("wells_list")
    }
    
    val wellListFlow: Flow<List<WellData>> = context.wellDataStore.data.map { prefs ->
        val raw = prefs[WELLS_KEY] ?: return@map emptyList()
        Json.decodeFromString<List<WellData>>(raw)
    }

    suspend fun saveWellList(wells: List<WellData>) {
        context.wellDataStore.edit { prefs ->
            prefs[WELLS_KEY] = Json.encodeToString(wells)
        }
    }

    suspend fun getWellById(wellId: Int): WellData? {
        return wellListFlow.first().find { it.id == wellId }
    }

    suspend fun saveWell(well: WellData) {
        val current = wellListFlow.first()
        val updated = current
            .filterNot { it.id == well.id }
            .plus(well)
        saveWellList(updated)
    }


    suspend fun updateWell(wellData: WellData) {
        val current = wellListFlow.first().toMutableList()
        val index = current.indexOfFirst { it.id == wellData.id }
        if (index != -1) {
            current[index] = wellData
            saveWellList(current)
        }
    }


    suspend fun deleteWell(wellId: String) {
        val current = wellListFlow.first()
        val updated = current.filterNot { it.id.toString() == wellId }
        saveWellList(updated)
    }

    suspend fun getAllWells(): List<WellData> {
        return wellListFlow.first()
    }
}