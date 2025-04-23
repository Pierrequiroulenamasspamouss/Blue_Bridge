package com.wellconnect.wellmonitoring.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val Context.wellDataStore by preferencesDataStore("well_data_store")

class WellDataStore(private val context: Context) {
    private val WELLS_KEY = stringPreferencesKey("wells_list")


    val wellListFlow: Flow<List<WellData>> = context.wellDataStore.data
        .map { prefs ->
            prefs[WELLS_KEY]?.let { Json.decodeFromString<List<WellData>>(it) } ?: emptyList()
        }


    suspend fun saveWellList(wells: List<WellData>) {
        context.wellDataStore.edit { prefs ->
            prefs[WELLS_KEY] = Json.encodeToString(wells)
        }
    }


    suspend fun getWell(wellId: Int): WellData? {
        return context.wellDataStore.data
            .map { prefs ->
                prefs[WELLS_KEY]?.let { Json.decodeFromString<List<WellData>>(it) } ?: emptyList()
            }
            .map { wells -> wells.find { it.id == wellId } }
            .firstOrNull()
    }


    suspend fun saveWell(well: WellData) {
        context.wellDataStore.edit { prefs ->
            val currentList = prefs[WELLS_KEY]?.let { Json.decodeFromString<List<WellData>>(it) } ?: emptyList()
            val updatedList = if (currentList.any { it.id == well.id }) {
                currentList.map { if (it.id == well.id) well else it }
            } else {
                currentList + well
            }
            prefs[WELLS_KEY] = Json.encodeToString(updatedList)
        }
    }

    suspend fun clearAllWells() {
        context.wellDataStore.edit { prefs ->
            prefs.remove(WELLS_KEY)
        }
    }


}
