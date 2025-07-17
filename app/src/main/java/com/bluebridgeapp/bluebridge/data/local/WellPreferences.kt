package com.bluebridgeapp.bluebridge.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.bluebridgeapp.bluebridge.data.model.WellData
import com.bluebridgeapp.bluebridge.network.ServerApi
import com.bluebridgeapp.bluebridge.utils.ImageUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

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

    suspend fun getWellById(wellId: Int): WellData? {
        return wellListFlow.first().find { it.wellId == wellId.toString() }
    }

    suspend fun saveWell(well: WellData, api: ServerApi? = null) {
        val current = wellListFlow.first()
        val updated = current
            .filterNot { it.wellId == well.wellId }
            .plus(well)
        saveWellList(updated)
        
        // Download and save images if API is provided
        api?.let { serverApi ->
            well.images?.forEach { imageData ->
                if (!ImageUtils.imageExists(context, imageData.imageNumber)) {
                    try {
                        val response = serverApi.getWellImage(well.wellId, imageData.imageNumber)
                        if (response.isSuccessful) {
                            response.body()?.let { responseBody ->
                                ImageUtils.downloadAndSaveImage(context, imageData.imageNumber, responseBody)
                            }
                        }
                    } catch (e: Exception) {
                        // Log error but don't fail the save operation
                        android.util.Log.e("WellPreferences", "Error downloading image ${imageData.imageNumber}: ${e.message}")
                    }
                }
            }
        }
    }

    suspend fun updateWell(wellData: WellData, api: ServerApi? = null) {
        val current = wellListFlow.first().toMutableList()
        val index = current.indexOfFirst { it.wellId == wellData.wellId }
        if (index != -1) {
            current[index] = wellData
            saveWellList(current)
            
            // Download new images if API is provided
            api?.let { serverApi ->
                wellData.images?.forEach { imageData ->
                    if (!ImageUtils.imageExists(context, imageData.imageNumber)) {
                        try {
                            val response = serverApi.getWellImage(wellData.wellId, imageData.imageNumber)
                            if (response.isSuccessful) {
                                response.body()?.let { responseBody ->
                                    ImageUtils.downloadAndSaveImage(context, imageData.imageNumber, responseBody)
                                }
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("WellPreferences", "Error downloading image ${imageData.imageNumber}: ${e.message}")
                        }
                    }
                }
            }
        }
    }

    suspend fun deleteWell(wellId: String) {
        val current = wellListFlow.first()
        val wellToDelete = current.find { it.wellId.toString() == wellId }
        
        // Delete associated images
        wellToDelete?.images?.forEach { imageData ->
            ImageUtils.deleteImageFile(context, imageData.imageNumber)
        }
        
        val updated = current.filterNot { it.wellId.toString() == wellId }
        saveWellList(updated)
    }

    suspend fun getAllWells(): List<WellData> {
        return wellListFlow.first()
    }
    
    suspend fun getWellImage(imageNumber: Int): String? {
        return if (ImageUtils.imageExists(context, imageNumber)) {
            ImageUtils.getImageFile(context, imageNumber).absolutePath
        } else {
            null
        }
    }
    
    suspend fun loadWellImageAsBitmap(imageNumber: Int): android.graphics.Bitmap? {
        return if (ImageUtils.imageExists(context, imageNumber)) {
            ImageUtils.loadImageFromFile(ImageUtils.getImageFile(context, imageNumber).absolutePath)
        } else {
            null
        }
    }
    
    suspend fun loadWellImageAsComposeBitmap(imageNumber: Int): androidx.compose.ui.graphics.ImageBitmap? {
        return if (ImageUtils.imageExists(context, imageNumber)) {
            ImageUtils.loadImageAsComposeBitmap(ImageUtils.getImageFile(context, imageNumber).absolutePath)
        } else {
            null
        }
    }
}