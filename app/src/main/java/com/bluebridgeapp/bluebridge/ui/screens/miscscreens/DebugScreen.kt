package com.bluebridgeapp.bluebridge.ui.screens.miscscreens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bluebridgeapp.bluebridge.data.model.WellData
import com.bluebridgeapp.bluebridge.data.model.ImageData
import com.bluebridgeapp.bluebridge.data.model.Location
import com.bluebridgeapp.bluebridge.ui.components.WellImagesRow
import com.bluebridgeapp.bluebridge.viewmodels.UserViewModel
import com.bluebridgeapp.bluebridge.viewmodels.WellViewModel

@Composable
fun DebugScreen(userViewModel: UserViewModel, wellViewModel: WellViewModel) {
    var username by remember { mutableStateOf("") }
    var sampleWell by remember { mutableStateOf<WellData?>(null) }

    LaunchedEffect(Unit) {
        userViewModel.getUserData().collect { userData ->
            username = userData?.username ?: "User"
        }
    }

    // Create a sample well with images for demonstration
    LaunchedEffect(Unit) {
        sampleWell = WellData(
            wellId = "ESP-1234",
            wellName = "Sample Well",
            wellLocation = Location(latitude = 45.0517, longitude = -73.5673),
            wellWaterType = "Clean",
            wellCapacity = "50",
            wellWaterLevel = "75",
            wellStatus = "Active",
            images = listOf(
                ImageData(
                    imageNumber = 0, description = "Well entrance",
                    fileName = "well_001_0.png",
                    uploadDate = "",
                    fileSize = 150
                )/*,
                ImageData(
                    imageNumber = 1, description = "Water quality test",
                    fileName = TODO(),
                    uploadDate = TODO(),
                    fileSize = TODO()
                ),
                ImageData(
                    imageNumber = 2, description = "Equipment overview",
                    fileName = TODO(),
                    uploadDate = TODO(),
                    fileSize = TODO()
                )
                */
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Hello $username")
        
        Card(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(text = "Well Images Demo")
                
                sampleWell?.let { well ->
                    WellImagesRow(
                        wellData = well,
                        wellRepository = wellViewModel.repository,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}