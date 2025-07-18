package com.bluebridgeapp.bluebridge.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.bluebridgeapp.bluebridge.data.model.WellData
import com.bluebridgeapp.bluebridge.network.ServerApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import android.util.Base64

val Context.wellDataStore: DataStore<Preferences> by preferencesDataStore(name = "well_preferences")
class WellPreferences(val context: Context) {
    companion object {
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

    suspend fun getWellById(wellId: String?): WellData? {
        return wellListFlow.first().find { it.wellId == wellId.toString() }
    }

    suspend fun saveWell(well: WellData, api: ServerApi? = null) {
        val current = wellListFlow.first()
        val updated = current
            .filterNot { it.wellId == well.wellId }
            .plus(well)
        saveWellList(updated)
    }


    suspend fun deleteWell(wellId: String) {
        val current = wellListFlow.first()
        val updated = current.filterNot { it.wellId.toString() == wellId }
        saveWellList(updated)
    }

    suspend fun getAllWells(): List<WellData> {
        return wellListFlow.first()
    }

    suspend fun loadWellImageAsBitmap(wellId: String, imageNumber: Int): Bitmap? {
        val well = getWellById(wellId) ?: return null

        // Check if images list exists and has the requested index
        val images = well.images ?: return null
        if (images.isEmpty() || imageNumber < 0 || imageNumber >= images.size) {
            return null
        }

        val imageBase64 = images[imageNumber].base64encodedImage

        return try {
            val decodedString = Base64.decode(imageBase64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
        } catch (e: IllegalArgumentException) {
            // Handle error: Invalid Base64 string
            null
        } catch (e: Exception) {
            // Handle other potential errors
            null
        }
    }
}