package com.wellconnect.wellmonitoring.data.repository

import WellData
import com.wellconnect.wellmonitoring.data.`interface`.WellRepository
import com.wellconnect.wellmonitoring.data.local.WellPreferences
import com.wellconnect.wellmonitoring.data.local.wellDataStore
import kotlinx.coroutines.flow.first

class WellRepositoryImpl(
    private val preferences: WellPreferences
) : WellRepository {
    override suspend fun saveWell(well: WellData) {
        preferences.saveWell(well)
    }

    override suspend fun saveWellList(wells: List<WellData>) {
        preferences.saveWellList(wells)
    }

    override suspend fun getWell(wellId: Int): WellData? {
        return preferences.getWell(wellId)
    }

    override suspend fun deleteWellAt(index: Int) {
        preferences.deleteWellAt(index)
    }

    override suspend fun swapWells(from: Int, to: Int) {
        preferences.swapWells(from, to)
    }

    override suspend fun isEspIdUnique(id: String, currentId: Int): Boolean {
        return preferences.isEspIdUnique(id, currentId)
    }

    override suspend fun getWells(): List<WellData> {
        return preferences.run {
            val prefs = context.wellDataStore.data.first()
            val raw = prefs[WellPreferences.Companion.WELLS_KEY] ?: return emptyList()
            runCatching {
                kotlinx.serialization.json.Json.decodeFromString(
                    kotlinx.serialization.builtins.ListSerializer(WellData.serializer()), raw
                )
            }.getOrElse { emptyList<WellData>() }
        }
    }

    override val wellListFlow get() = preferences.wellListFlow
}
