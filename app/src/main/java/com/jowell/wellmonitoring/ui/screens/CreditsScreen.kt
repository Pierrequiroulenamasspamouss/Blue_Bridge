package com.jowell.wellmonitoring.ui.screens

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
import com.jowell.wellmonitoring.R
import com.jowell.wellmonitoring.ui.BackButton
import com.jowell.wellmonitoring.ui.TextComponent
import com.jowell.wellmonitoring.ui.TopBar


@Composable
fun CreditsScreen(navController: NavController) {
    val appDeveloper: String = stringResource(id = R.string.app_dev)
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(16.dp)) {
            TopBar(topBarMessage = "Credits")
            BackButton(navController = navController)
            Spacer(modifier = Modifier.padding(top = 16.dp))
            TextComponent("App Developer: $appDeveloper", 18.sp)
        }
    }
}