package com.jowell.wellmonitoring.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.jowell.wellmonitoring.R
import com.jowell.wellmonitoring.data.WellConfigEvents
import com.jowell.wellmonitoring.ui.BackButton
import com.jowell.wellmonitoring.ui.NumbersFieldComponent
import com.jowell.wellmonitoring.ui.SaveDataButton
import com.jowell.wellmonitoring.ui.TextComponent
import com.jowell.wellmonitoring.ui.TextFieldComponent
import com.jowell.wellmonitoring.ui.TopBar
import com.jowell.wellmonitoring.ui.WellViewModel

const val debug = true

@Composable
fun WellConfigScreen(userViewModel: WellViewModel, navController: NavController, wellId: Int) {
    val wellData = userViewModel.wellData.value
    val savedData = userViewModel.lastSavedData.value
    val wellLoaded = userViewModel.wellLoaded.value

    var dataIsSaved by remember { mutableStateOf(false) }
    var showUnsavedChangesDialog by remember { mutableStateOf(false) }

    val hasUnsavedChanges = wellData != savedData

    LaunchedEffect(wellId) {
        userViewModel.clearWellData()   // reset before loading
        userViewModel.getWellData(wellId)
    }


    BackHandler(enabled = true) {
        if (!hasUnsavedChanges) {
            navController.popBackStack()
        } else {
            showUnsavedChangesDialog = true
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize()
    ) {
        if (wellLoaded) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                item {
                    TopBar(
                        topBarMessage = stringResource(id = R.string.Configure_top_bar_text) + " ${
                            if (debug) {
                                wellId
                            } else {
                            }
                        }"
                    )
                }
                item {
                    BackButton(navController = navController, clickable = !hasUnsavedChanges)
                }


                item { Spacer(modifier = Modifier.size(30.dp)) }

                item {
                    TextComponent(
                        textValue = stringResource(id = R.string.Well_name_field_text),
                        textSize = 18.sp
                    )
                    key(wellData.id) {
                        TextFieldComponent(
                            initialValue = wellData.wellName,
                            defaultInputMessage = stringResource(id = R.string.Well_name_field_text),
                            onTextChanged = {
                                userViewModel.onConfigEvent(WellConfigEvents.WellNameEntered(it))
                            }
                        )
                    }


                }

                item {
                    Spacer(modifier = Modifier.size(10.dp))
                    TextComponent(
                        textValue = stringResource(id = R.string.Well_owner_field_text),
                        textSize = 18.sp
                    )
                    key(wellData.id) {
                        TextFieldComponent(
                            initialValue = wellData.wellOwner,
                            defaultInputMessage = stringResource(id = R.string.Well_owner_field_text),
                            onTextChanged = {
                                userViewModel.onConfigEvent(WellConfigEvents.OwnerEntered(it))
                            }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.size(10.dp))
                    TextComponent(
                        textValue = stringResource(id = R.string.Well_location_field_text),
                        textSize = 18.sp
                    )
                    key(wellData.id) {
                        TextFieldComponent(
                            initialValue = wellData.wellLocation,
                            defaultInputMessage = stringResource(id = R.string.Well_location_field_text),
                            onTextChanged = {
                                userViewModel.onConfigEvent(WellConfigEvents.WellLocationEntered(it))
                            }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.size(10.dp))
                    TextComponent(
                        textValue = stringResource(id = R.string.Water_type_field_text),
                        textSize = 18.sp
                    )
                    key(wellData.id) {
                        TextFieldComponent(
                            initialValue = wellData.wellWaterType,
                            defaultInputMessage = stringResource(id = R.string.Water_type_field_text),
                            onTextChanged = {
                                userViewModel.onConfigEvent(WellConfigEvents.WaterTypeEntered(it))
                            }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.size(10.dp))
                    TextComponent(
                        textValue = stringResource(id = R.string.Well_capacity_field_text),
                        textSize = 18.sp
                    )
                    key(wellData.id) {
                        NumbersFieldComponent(
                            initialValue = wellData.wellCapacity.toString(),
                            defaultInputMessage = stringResource(id = R.string.Well_capacity_field_text),
                            onTextChanged = {
                                userViewModel.onConfigEvent(
                                    WellConfigEvents.WellCapacityEntered(
                                        it.toIntOrNull() ?: 0
                                    )
                                )
                            }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.size(10.dp))
                    TextComponent(
                        textValue = stringResource(id = R.string.Water_level_field_text),
                        textSize = 18.sp
                    )
                    key(wellData.id) {
                        NumbersFieldComponent(
                            initialValue = wellData.wellWaterLevel.toString(),
                            defaultInputMessage = stringResource(id = R.string.Water_level_field_text),
                            onTextChanged = {
                                userViewModel.onConfigEvent(
                                    WellConfigEvents.WaterLevelEntered(
                                        it.toIntOrNull() ?: 0
                                    )
                                )
                            }
                        )
                    }
                }

                item { Spacer(modifier = Modifier.size(10.dp)) }
                item {
                    TextComponent(
                        textValue = stringResource(id = R.string.Water_consumption_field_text),
                        textSize = 18.sp
                    )
                    key(wellData.id) {
                    NumbersFieldComponent(
                        initialValue = wellData.wellWaterConsumption.toString(),
                        defaultInputMessage = stringResource(id = R.string.Water_consumption_field_text),
                        onTextChanged = {
                            userViewModel.onConfigEvent(
                                WellConfigEvents.ConsumptionEntered(
                                    it.toIntOrNull() ?: 0
                                )
                            )
                        }
                    )
                        }
                }

                item {
                    Spacer(modifier = Modifier.size(10.dp))
                    TextComponent(
                        textValue = stringResource(id = R.string.Well_ip_address_field_text),
                        textSize = 18.sp
                    )
                    key(wellData.id) {
                        TextFieldComponent(
                            initialValue = wellData.ipAddress.toString(),
                            defaultInputMessage = stringResource(id = R.string.Well_ip_address_field_text),
                            onTextChanged = {
                                userViewModel.onConfigEvent(
                                    WellConfigEvents.IpAddressEntered(
                                        it
                                    )
                                )
                            }
                        )
                    }
                }

                item {
                    if (!dataIsSaved) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            SaveDataButton(
                                userViewModel = userViewModel,
                                onSave = {
                                    userViewModel.onConfigEvent(WellConfigEvents.SaveWell(wellId))
                                    dataIsSaved = true
                                },
                                navController = navController
                            )
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(100.dp)) }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Loading well data...")
            }

            // Unsaved changes dialog
            if (showUnsavedChangesDialog) {
                AlertDialog(
                    onDismissRequest = { showUnsavedChangesDialog = false },
                    title = { Text(text = stringResource(id = R.string.unsaved_changes_dialog_title)) },
                    text = { Text(text = stringResource(id = R.string.unsaved_changes_dialog_message)) },

                    confirmButton = {
                        Button(onClick = {
                            userViewModel.onConfigEvent(WellConfigEvents.SaveWell(wellId))
                            showUnsavedChangesDialog = false
                            navController.popBackStack()
                        }) {
                            Text(text = stringResource(id = R.string.unsaved_changes_dialog_confirm))
                        }
                    },

                    dismissButton = {
                        Button(onClick = {
                            userViewModel.revertToLastSavedData() // Revert changes
                            showUnsavedChangesDialog = false
                            navController.popBackStack() // Navigate back
                        }) {
                            Text(text = stringResource(id = R.string.unsaved_changes_dialog_dismiss))
                        }
                    }
                )
            }
        }
    }
}
