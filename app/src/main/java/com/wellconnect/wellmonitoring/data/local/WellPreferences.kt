package com.wellconnect.wellmonitoring.data.local

import WellData
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val Context.wellDataStore: DataStore<Preferences> by preferencesDataStore(name = "well_preferences")

class WellPreferences(val context: Context) {

    val wellListFlow: Flow<List<WellData>> = context.wellDataStore.data.map { prefs ->
        val raw = prefs[WELLS_KEY] ?: return@map emptyList()
        runCatching { Json.decodeFromString<List<WellData>>(raw) }.getOrElse { emptyList() }
    }

    suspend fun saveWellList(wells: List<WellData>) {
        context.wellDataStore.edit { prefs ->
            prefs[WELLS_KEY] = Json.encodeToString(wells)
        }
    }

    suspend fun getWell(wellId: Int): WellData? {
        return getCurrentWellList().find { it.id == wellId }
    }

    suspend fun saveWell(well: WellData) {
        val current = getCurrentWellList()
        val updated = current
            .filterNot { it.id == well.id }
            .plus(well)
        saveWellList(updated)
    }

    suspend fun clearAllWells() {
        context.wellDataStore.edit { prefs -> prefs.remove(WELLS_KEY) }
    }

    suspend fun deleteWellAt(index: Int) {
        val current = getCurrentWellList()
        if (index in current.indices) {
            val updated = current.toMutableList().apply { removeAt(index) }
            saveWellList(updated)
        }
    }
    suspend fun swapWells(from: Int, to: Int) {
        val current = getCurrentWellList().toMutableList()
        if (from in current.indices && to in current.indices) {
            val temp = current[from]
            current[from] = current[to]
            current[to] = temp
            saveWellList(current)

        }
    }
    suspend fun isEspIdUnique(id: String, currentId: Int): Boolean {
        return getCurrentWellList().none { it.espId == id && it.id != currentId }
    }

    private suspend fun getCurrentWellList(): List<WellData> {
        val prefs = context.wellDataStore.data.first()
        val raw = prefs[WELLS_KEY] ?: return emptyList()

        return runCatching {
            Json.decodeFromString(ListSerializer(WellData.serializer()), raw)
        }.getOrElse { emptyList<WellData>() }
    }

    companion object {
        val WELLS_KEY = stringPreferencesKey("wells_list")
    }
}