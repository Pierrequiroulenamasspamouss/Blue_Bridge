package com.bluebridgeapp.bluebridge.utils

import android.content.Context
import android.util.Base64
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream


object ImageUtils {
    private const val IMAGE_SIZE = 256

    suspend fun loadImageFromFile(filePath: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            if (file.exists()) {
                BitmapFactory.decodeFile(file.absolutePath)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("ImageUtils", "Error loading image from file $filePath: ${e.message}", e)
            null
        }
    }
    
    suspend fun loadImageAsComposeBitmap(filePath: String): ImageBitmap? = withContext(Dispatchers.IO) {
        try {
            val bitmap = loadImageFromFile(filePath)
            bitmap?.asImageBitmap()
        } catch (e: Exception) {
            Log.e("ImageUtils", "Error loading image as Compose bitmap: ${e.message}", e)
            null
        }
    }
    
    private fun resizeAndCompressImage(filePath: String): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(filePath, options)
            
            val originalWidth = options.outWidth
            val originalHeight = options.outHeight
            
            val scaleFactor = calculateScaleFactor(originalWidth, originalHeight, IMAGE_SIZE)
            
            val newOptions = BitmapFactory.Options().apply {
                inSampleSize = scaleFactor
            }
            
            BitmapFactory.decodeFile(filePath, newOptions)
        } catch (e: Exception) {
            Log.e("ImageUtils", "Error resizing image: ${e.message}", e)
            null
        }
    }
    
    private fun calculateScaleFactor(originalWidth: Int, originalHeight: Int, targetSize: Int): Int {
        val maxDimension = maxOf(originalWidth, originalHeight)
        return if (maxDimension > targetSize) {
            maxDimension / targetSize
        } else {
            1     }
    }
    
    fun deleteImageFile(context: Context, imageNumber: Int) {
        try {
            val imageFile = File(context.filesDir, "well_image_$imageNumber.jpg")
            if (imageFile.exists()) {
                imageFile.delete()
            }
        } catch (e: Exception) {
            Log.e("ImageUtils", "Error deleting image file: ${e.message}", e)
        }
    }
    
    fun getImageFile(context: Context, imageNumber: Int): File {
        return File(context.filesDir, "well_image_$imageNumber.jpg")
    }
    
    fun imageExists(context: Context, imageNumber: Int): Boolean {
        return getImageFile(context, imageNumber).exists()
    }

    // Compresser et convertir en Base64
    suspend fun compressAndConvertToBase64(imagePath: String, quality: Int = 80): String? = withContext(Dispatchers.IO) {
        try {
            val bitmap = BitmapFactory.decodeFile(imagePath) ?: return@withContext null
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            val byteArray = outputStream.toByteArray()
            return@withContext Base64.encodeToString(byteArray, Base64.DEFAULT)
        } catch (e: Exception) {
            Log.e("ImageUtils", "Error converting to base64: ${e.message}", e)
            null
        }
    }


    // Sauvegarder l'image reconstruite
    suspend fun saveBase64Image(base64Image: String, context: Context, filename: String): String? = withContext(Dispatchers.IO) {
        try {
            val decodedBytes = Base64.decode(base64Image, Base64.DEFAULT)
            val imageFile = File(context.filesDir, filename)
            FileOutputStream(imageFile).use { outputStream: FileOutputStream ->
                outputStream.write(decodedBytes)
            }
            return@withContext imageFile.absolutePath
        } catch (e: Exception) {
            Log.e("ImageUtils", "Error saving base64 image: ${e.message}", e)
            null
        }
    }
    // Fonction pour compresser davantage le Base64
    fun compressBase64(base64: String): String {
        val bytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT)
        val baos = ByteArrayOutputStream()
        GZIPOutputStream(baos).use { it.write(bytes) }
        val compressedBytes = baos.toByteArray()
        return android.util.Base64.encodeToString(compressedBytes, android.util.Base64.NO_WRAP)
    }

    // Fonction pour décompresser le Base64
    private fun uncompressBase64(compressedBase64: String): String {
        val compressedBytes = android.util.Base64.decode(compressedBase64, android.util.Base64.NO_WRAP)
        val bais = ByteArrayInputStream(compressedBytes)
        val baos = ByteArrayOutputStream()
        GZIPInputStream(bais).use { it.copyTo(baos) }
        return android.util.Base64.encodeToString(baos.toByteArray(), android.util.Base64.DEFAULT)
    }

}


