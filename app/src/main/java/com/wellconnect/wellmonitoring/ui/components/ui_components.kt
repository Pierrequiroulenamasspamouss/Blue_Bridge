package com.wellconnect.wellmonitoring.ui.components


import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wellconnect.wellmonitoring.R

@Composable
fun TopBar(topBarMessage: String,isIcon : Boolean = true,iconId : Int = R.drawable.app_logo) {
    Row(modifier = Modifier.fillMaxWidth().padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = topBarMessage,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 30.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.weight(1f)) // puts the following element on the extreme right

        if (isIcon) {
            Image(
                modifier = Modifier.size(80.dp),
                painter = painterResource(id = iconId),
                contentDescription = stringResource(id = R.string.logo_description)
            )
        }
    }
}

