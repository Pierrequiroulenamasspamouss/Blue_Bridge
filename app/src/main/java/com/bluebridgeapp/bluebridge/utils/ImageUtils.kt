package com.bluebridgeapp.bluebridge.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object ImageUtils {
    private const val IMAGE_SIZE = 256
    private const val IMAGE_QUALITY = 80  
    suspend fun downloadAndSaveImage(
        context: Context,
        imageNumber: Int,
        responseBody: ResponseBody
    ): String? = withContext(Dispatchers.IO) {
        try {
            val imageFile = File(context.filesDir, "well_image_$imageNumber.jpg")
            val inputStream = responseBody.byteStream()
            val outputStream = FileOutputStream(imageFile)
            
            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            
            // Resize and compress the image
            val resizedBitmap = resizeAndCompressImage(imageFile.absolutePath)
            if (resizedBitmap != null) {
                val compressedFile = File(context.filesDir, "well_image_${imageNumber}_compressed.jpg")
                val compressedOutputStream = FileOutputStream(compressedFile)
                resizedBitmap.compress(Bitmap.CompressFormat.JPEG, IMAGE_QUALITY, compressedOutputStream)
                compressedOutputStream.close()
                
                // Delete original file and rename compressed file
                imageFile.delete()
                compressedFile.renameTo(imageFile)
            }
            
            imageFile.absolutePath
        } catch (e: Exception) {
            Log.e("ImageUtils", "Error downloading image $imageNumber: ${e.message}", e)
            null
        }
    }
    
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
} 