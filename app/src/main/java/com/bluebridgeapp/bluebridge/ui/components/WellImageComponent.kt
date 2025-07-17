package com.bluebridgeapp.bluebridge.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.bluebridgeapp.bluebridge.data.interfaces.WellRepository
import com.bluebridgeapp.bluebridge.data.model.ImageData
import com.bluebridgeapp.bluebridge.data.model.WellData
import com.bluebridgeapp.bluebridge.data.repository.WellRepositoryImpl
import kotlinx.coroutines.launch

@Composable
fun WellImagesRow(
    wellData: WellData,
    wellRepository: WellRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var loadedImages by remember { mutableStateOf<Map<Int, ImageBitmap>>(emptyMap()) }
    
    LaunchedEffect(wellData.images) {
        wellData.images?.forEach { imageData ->
            scope.launch {
                val bitmap = wellRepository.getWellImageAsComposeBitmap(imageData.imageNumber)
                if (bitmap != null) {
                    loadedImages = loadedImages + (imageData.imageNumber to bitmap)
                }
            }
        }
    }
    
    if (wellData.images?.isNotEmpty() == true) {
        LazyRow(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(wellData.images) { imageData ->
                WellImageCard(
                    imageData = imageData,
                    imageBitmap = loadedImages[imageData.imageNumber],
                    modifier = Modifier.width(200.dp)
                )
            }
        }
    }
}

@Composable
fun WellImageCard(
    imageData: ImageData,
    imageBitmap: ImageBitmap?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
    ) {
        Column {
            if (imageBitmap != null) {
                Image(
                    bitmap = imageBitmap,
                    contentDescription = imageData.description,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Placeholder while loading
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Loading...")
                }
            }
            
            Text(
                text = imageData.description,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

@Composable
fun SingleWellImage(
    imageNumber: Int,
    description: String,
    wellRepository: WellRepositoryImpl,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    
    LaunchedEffect(imageNumber) {
        scope.launch {
            imageBitmap = wellRepository.getWellImageAsComposeBitmap(imageNumber)
        }
    }
    
    Card(modifier = modifier) {
        if (imageBitmap != null) {
            Image(
                bitmap = imageBitmap!!,
                contentDescription = description,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
              Text("Loading image...")
            }
        }
    }
} 