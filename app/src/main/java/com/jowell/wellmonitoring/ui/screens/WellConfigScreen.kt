package com.jowell.wellmonitoring.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.jowell.wellmonitoring.R
import com.jowell.wellmonitoring.data.WellConfigEvents
import com.jowell.wellmonitoring.data.isSavable
import com.jowell.wellmonitoring.ui.WellViewModel
import com.jowell.wellmonitoring.ui.theme.SaveDataButton
import com.jowell.wellmonitoring.ui.theme.WellField
import kotlinx.coroutines.launch

const val debug = true

@Composable
fun WellConfigScreen(userViewModel: WellViewModel, navController: NavController, wellId: Int) {
    val wellData = userViewModel.wellData.value
    val savedData = userViewModel.lastSavedData.value
    val wellLoaded = userViewModel.wellLoaded.value

    var showUnsavedChangesDialog by remember { mutableStateOf(false) }

    val hasUnsavedChanges = wellData != savedData

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(wellId) {
        userViewModel.clearWellData()   // reset before loading
        userViewModel.getWellData(wellId)
    }

    BackHandler(enabled = true) {
        when {
            !hasUnsavedChanges && !wellData.isSavable() -> {
                // Empty and unsaved → delete
                userViewModel.deleteWell(wellId)
                userViewModel.clearWellData()
                navController.popBackStack()
            }
            !hasUnsavedChanges -> {
                userViewModel.clearWellData()
                navController.popBackStack()
            }
            else -> {
                showUnsavedChangesDialog = true
            }
        }
    }



    Surface(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            val showSaveButton = hasUnsavedChanges && wellData.isSavable()

                    if (wellLoaded) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(18.dp)
                        ) {
                            item {
                                WellField(
                                    label = stringResource(id = R.string.Well_name_field_text),
                                    value = wellData.wellName,
                                    keyId = wellData.id,
                                    onValueChange = {
                                        userViewModel.onConfigEvent(WellConfigEvents.WellNameEntered(it))
                                    }
                                )
                            }

                            item {
                                WellField(
                                    label = stringResource(id = R.string.Well_owner_field_text),
                                    value = wellData.wellOwner,
                                    keyId = wellData.id,
                                    onValueChange = {
                                        userViewModel.onConfigEvent(WellConfigEvents.OwnerEntered(it))
                                    }
                                )
                            }

                            item {
                                WellField(
                                    label = stringResource(id = R.string.Well_location_field_text),
                                    value = wellData.wellLocation,
                                    keyId = wellData.id,
                                    onValueChange = {
                                        userViewModel.onConfigEvent(WellConfigEvents.WellLocationEntered(it))
                                    }
                                )
                            }

                            item {
                                WellField(
                                    label = stringResource(id = R.string.Water_type_field_text),
                                    value = wellData.wellWaterType,
                                    keyId = wellData.id,
                                    onValueChange = {
                                        userViewModel.onConfigEvent(WellConfigEvents.WaterTypeEntered(it))
                                    }
                                )
                            }

                            item {
                                WellField(
                                    label = stringResource(id = R.string.Well_capacity_field_text),
                                    value = wellData.wellCapacity,
                                    keyId = wellData.id,
                                    onValueChange = {
                                        userViewModel.onConfigEvent(
                                            WellConfigEvents.WellCapacityEntered(it.toIntOrNull() ?: 0)
                                        )
                                    },
                                    isNumeric = true
                                )
                            }

                            item {
                                WellField(
                                    label = stringResource(id = R.string.Water_level_field_text),
                                    value = wellData.wellWaterLevel,
                                    keyId = wellData.id,
                                    onValueChange = {
                                        userViewModel.onConfigEvent(
                                            WellConfigEvents.WaterLevelEntered(it.toIntOrNull() ?: 0)
                                        )
                                    },
                                    isNumeric = true
                                )
                            }

                            item {
                                WellField(
                                    label = stringResource(id = R.string.Water_consumption_field_text),
                                    value = wellData.wellWaterConsumption,
                                    keyId = wellData.id,
                                    onValueChange = {
                                        userViewModel.onConfigEvent(
                                            WellConfigEvents.ConsumptionEntered(it.toIntOrNull() ?: 0)
                                        )
                                    },
                                    isNumeric = true
                                )
                            }

                            item {
                                WellField(
                                    label = stringResource(id = R.string.Well_ip_address_field_text),
                                    value = wellData.ipAddress,
                                    keyId = wellData.id,
                                    onValueChange = { newIp ->
                                        if (userViewModel.isIpAddressDuplicate(newIp, wellId)) {
                                            scope.launch {
                                                snackbarHostState.showSnackbar("IP address already used by another well.")
                                            }
                                        } else {
                                            userViewModel.onConfigEvent(WellConfigEvents.IpAddressEntered(newIp))
                                        }
                                    }
                                )
                            }

                            item {
                                if (showSaveButton) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        SaveDataButton(
                                            userViewModel = userViewModel,
                                            onSave = {
                                                userViewModel.onConfigEvent(
                                                    WellConfigEvents.SaveWell(
                                                        wellId
                                                    )
                                                )
                                                userViewModel.getWellData(wellId) // refresh after save
                                            },
                                            navController = navController,
                                            well = wellData,
                                            snackbarHostState = snackbarHostState,
                                            context = context
                                        )
                                    }
                                }
                            }

                            item { Spacer(modifier = Modifier.height(100.dp)) }
                        }
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Loading well data...")
                        }
                    }
                }

                    SnackbarHost(
                        hostState = snackbarHostState,

                    )

                    if (showUnsavedChangesDialog) {
                        AlertDialog(
                            onDismissRequest = { showUnsavedChangesDialog = false },
                            title = { Text(text = stringResource(id = R.string.unsaved_changes_dialog_title)) },
                            text = { Text(text = stringResource(id = R.string.unsaved_changes_dialog_message)) },
                            confirmButton = {
                                Button(onClick = {
                                    userViewModel.onConfigEvent(WellConfigEvents.SaveWell(wellId))
                                    userViewModel.clearWellData()
                                    showUnsavedChangesDialog = false
                                    navController.popBackStack()
                                }) {
                                    Text(text = stringResource(id = R.string.unsaved_changes_dialog_confirm))
                                }
                            },
                            dismissButton = {
                                Button(onClick = {
                                    userViewModel.revertToLastSavedData()
                                    userViewModel.clearWellData()
                                    showUnsavedChangesDialog = false
                                    navController.popBackStack()
                                }) {
                                    Text(text = stringResource(id = R.string.unsaved_changes_dialog_dismiss))
                                }
                            })
                    }
    }
}

