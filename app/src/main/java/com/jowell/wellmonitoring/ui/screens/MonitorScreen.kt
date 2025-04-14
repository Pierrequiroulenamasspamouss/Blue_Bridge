package com.jowell.wellmonitoring.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.jowell.wellmonitoring.R
import com.jowell.wellmonitoring.data.WellData
import com.jowell.wellmonitoring.ui.BackButton
import com.jowell.wellmonitoring.ui.DataAndButtons
import com.jowell.wellmonitoring.ui.TopBar
import com.jowell.wellmonitoring.ui.WellViewModel
import kotlinx.coroutines.launch


@Composable
fun WellDataDisplay(userViewModel: WellViewModel, navController: NavController) {
    val wells by userViewModel.wellList.collectAsState()
    val errorMessage by userViewModel.errorMessage
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Launch snackbar when there's an error
    LaunchedEffect(errorMessage) {
        errorMessage?.let { message ->
            scope.launch {
                snackbarHostState.showSnackbar(message)
                userViewModel.clearErrorMessage()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize()
        ) {

            LazyColumn(
                modifier = Modifier
                    .padding(bottom = 60.dp)
                    .fillMaxSize()
            ) {
                item {
                    TopBar(topBarMessage = stringResource(id = R.string.Monitoring_top_bar_text))
                    BackButton(navController = navController)
                }
                itemsIndexed(wells) { index, well ->
                    DataAndButtons(well, index, navController, userViewModel, context, scope, snackbarHostState)

                }


                // Add new well button
                item {
                    Spacer(modifier = Modifier.padding(vertical = 12.dp))
                    Button(
                        onClick = {
                            val newWellId = (wells.lastOrNull()?.id ?: 0) + 1
                            val newWell = WellData(id = newWellId)
                            userViewModel.addWell(newWell)
                            navController.navigate("${Routes.WELL_CONFIG_SCREEN}/${newWell.id}")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = stringResource(id = R.string.create_new_well))
                    }
                }
            }
        }
        // Display error messages
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.padding(16.dp)
        )
    }
}

