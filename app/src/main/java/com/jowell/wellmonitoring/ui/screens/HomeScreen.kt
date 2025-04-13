package com.jowell.wellmonitoring.ui.screens
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.jowell.wellmonitoring.R
import com.jowell.wellmonitoring.ui.RectangleButton
import com.jowell.wellmonitoring.ui.TopBar


@Composable
fun HomeScreen(navController: NavController) {

    Surface(modifier = Modifier.fillMaxSize()) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(15.dp)
        ) {
            TopBar(topBarMessage = stringResource(id = R.string.Welcome_top_bar_text))

            RectangleButton(
                textValue = "Monitor Wells",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                backgroundColor = MaterialTheme.colorScheme.primary ,
                function = { navController.navigate(Routes.MONITORING_SCREEN) },
                functionName = "Navigate to Monitor Wells Screen",
                textColor = MaterialTheme.colorScheme.background
            )





            RectangleButton(
                textValue = "Settings",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                textColor = MaterialTheme.colorScheme.onBackground,
                backgroundColor = colorResource(id = R.color.light_gray),
                function = { navController.navigate(Routes.SETTINGS_SCREEN) },
                functionName = "Go to the settings"
            )


        }
    }
}


