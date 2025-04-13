package com.jowell.wellmonitoring.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.jowell.wellmonitoring.R
import com.jowell.wellmonitoring.data.WellData
import com.jowell.wellmonitoring.ui.BackButton
import com.jowell.wellmonitoring.ui.TextComponent
import com.jowell.wellmonitoring.ui.TopBar
import com.jowell.wellmonitoring.ui.WellViewModel

@Composable
fun WellDataDisplay(userViewModel: WellViewModel, navController: NavController) {
    val wells by userViewModel.wellList.collectAsState()
    val spacing = 8.dp

    Column(modifier = Modifier.padding(16.dp)) {
        TopBar(topBarMessage = stringResource(id = R.string.Monitoring_top_bar_text))
        BackButton(navController = navController)
        Spacer(modifier = Modifier.padding(top = spacing))

        LazyColumn(
            modifier = Modifier
                .padding(bottom = 60.dp)
                .fillMaxSize()
        ) {
            wells.forEachIndexed { index, well ->
                item {
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            TextComponent("Name: ${well.wellName}", 20.sp, textFont = FontWeight.Bold)
                            TextComponent("Owner: ${well.wellOwner}", 16.sp)
                            TextComponent("Location: ${well.wellLocation}", 16.sp)
                            TextComponent("Type: ${well.wellWaterType}", 16.sp)
                            TextComponent("Capacity: ${well.wellCapacity}L", 16.sp)
                            TextComponent("Level: ${well.wellWaterLevel}L", 16.sp)
                            TextComponent("Consumption: ${well.wellWaterConsumption} L/day", 16.sp)
                        }

                        Column(
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .align(Alignment.CenterVertically)
                        ) {
                            Button(
                                onClick = {
                                    try {
                                        navController.navigate("${Routes.WELL_CONFIG_SCREEN}/${well.id}")
                                    } catch (_: Exception) {
                                        println("Invalid well ID for editing.")
                                    }
                                },
                                modifier = Modifier.padding(bottom = 4.dp)
                            ) {
                                Text("Edit")
                            }

                            Button(onClick = {
                                userViewModel.deleteWell(index)
                            }) {
                                Text("Delete")
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.padding(vertical = 12.dp))
                Button(
                    onClick = {
                        val newWellId = (wells.lastOrNull()?.id ?: 0) + 1
                        val newWell = WellData(id = newWellId)
                        userViewModel.addWell(newWell)

                        try {
                            navController.navigate("${Routes.WELL_CONFIG_SCREEN}/${newWell.id}")
                        } catch (e: Exception) {
                            println("Error navigating to wellConfig: ${e.message}")
                        }

                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(id = R.string.create_new_well))
                }
            }
        }
    }
}

