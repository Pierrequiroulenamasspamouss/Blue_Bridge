package com.jowell.wellmonitoring.data

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

val WELL_LIST = stringPreferencesKey("well_list")


class WellDataStore(private val context: Context) {

    companion object {
        val WELLS = stringPreferencesKey("wells") // Key for storing all wells
    }

    suspend fun saveWellData(wellId: Int?, wellData: WellData) {
        context.wellDataStore.edit { prefs ->
            val wellList = prefs[WELLS]?.let { Json.decodeFromString<List<WellData>>(it) } ?: emptyList()
            val updatedWellList = wellList.map { if (it.id == wellId) wellData else it }
            prefs[WELLS] = Json.encodeToString(updatedWellList)
        }
    }

    suspend fun getWellData(wellId: Int): WellData? {
        val wellList = context.wellDataStore.data.firstOrNull()?.let { prefs ->
            val wellsJson = prefs[WELLS]
            wellsJson?.let { Json.decodeFromString<List<WellData>>(it) }
        }
        return wellList?.find { it.id == wellId }
    }

    suspend fun saveWellList(wells: List<WellData>) {
        val json = Json.encodeToString(wells)
        context.wellDataStore.edit { prefs ->
            prefs[WELL_LIST] = json
        }
    }

    suspend fun resetWellList() {
        context.wellDataStore.edit { prefs ->
            prefs.remove(WELL_LIST)
        }
    }

    val wellListFlow: Flow<List<WellData>> = context.wellDataStore.data.map { prefs ->
        val json = prefs[WELL_LIST] ?: "[]"
        Json.decodeFromString<List<WellData>>(json)
    }
}


