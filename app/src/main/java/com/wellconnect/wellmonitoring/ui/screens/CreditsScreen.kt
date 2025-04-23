package com.wellconnect.wellmonitoring.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.wellconnect.wellmonitoring.R
import com.wellconnect.wellmonitoring.ui.TextComponent
import com.wellconnect.wellmonitoring.ui.TopBar
import com.wellconnect.wellmonitoring.ui.WellViewModel
import com.wellconnect.wellmonitoring.ui.theme.BackButton


@Composable
fun CreditsScreen(navController: NavController,userViewModel: WellViewModel ) {
    val appDeveloper: String = stringResource(id = R.string.app_dev)
    val appDesigner: String = stringResource(id = R.string.app_designer)
    val helper: String = stringResource(id = R.string.helper)
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(16.dp)) {
            TopBar(topBarMessage = "Credits")
            BackButton(navController = navController, userViewModel = userViewModel) {
                navController.popBackStack()
            }
            Spacer(modifier = Modifier.padding(top = 16.dp))
            TextComponent("App Developer: $appDeveloper", 18.sp)
            TextComponent("Design helper: $helper", 18.sp)
            TextComponent("App Desinger: $appDesigner", 18.sp)
        }
    }
}