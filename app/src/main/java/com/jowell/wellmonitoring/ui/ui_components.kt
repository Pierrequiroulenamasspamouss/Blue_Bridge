package com.jowell.wellmonitoring.ui


import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jowell.wellmonitoring.R

@Composable
fun TopBar(topBarMessage: String,isIcon : Boolean = true,iconId : Int = R.drawable.water_well_icon) {
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



@Composable
fun TextComponent(textValue: String,
                  textSize : TextUnit,
                  textColor: Color = MaterialTheme.colorScheme.onBackground,
                  textFont : FontWeight = FontWeight.Light){
    Text(text = textValue,
        fontSize = textSize,
        color = textColor,
        fontWeight = textFont)
}

@Composable
fun NumbersFieldComponent(
    onTextChanged: (String) -> Unit,
    defaultInputMessage: String = "PLACEHOLDER TEXT",
    initialValue: String = ""
) {
    var currentValue by remember { mutableStateOf(initialValue) }

    val localFocusManager = LocalFocusManager.current
    Box(modifier = Modifier.background(color = MaterialTheme.colorScheme.secondary) ) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = currentValue,
            onValueChange = {
                // Filter out non-numeric characters
                val filteredValue = it.filter { char -> char.isDigit() }
                currentValue = filteredValue
                onTextChanged(filteredValue) // Pass the filtered value to the parent
            },
            placeholder = {
                Text(
                    text = defaultInputMessage,
                    fontSize = 18.sp,
                    color =  Color.Gray
                )
            },
            textStyle = TextStyle.Default.copy(fontSize = 24.sp, color = MaterialTheme.colorScheme.surface),
            keyboardOptions = KeyboardOptions(
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions {
                localFocusManager.clearFocus()
            },
            singleLine = true
        )
    }
}


@Composable
fun TextFieldComponent(
    onTextChanged: (String) -> Unit,
    defaultInputMessage: String = "PLACEHOLDER TEXT",
    initialValue: String = ""
) {
    var currentValue by remember { mutableStateOf(initialValue) }

    val localFocusManager = LocalFocusManager.current
    Box(modifier = Modifier.background(color = MaterialTheme.colorScheme.secondary)) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = currentValue,
            onValueChange = {
                currentValue = it
                onTextChanged(it)
            },
            placeholder = {
                Text(
                    text = defaultInputMessage,
                    fontSize = 18.sp,
                    color = Color.Gray
                )
            },
            textStyle = TextStyle.Default.copy(fontSize = 24.sp, color = MaterialTheme.colorScheme.surface),
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions {
                localFocusManager.clearFocus()
            },
            singleLine = true
        )
    }
}


@Composable
fun RectangleButton(
    textValue: String = "Rectangle Button",
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(100.dp),
    backgroundColor: Color = MaterialTheme.colorScheme.primary ,
    elevation: Dp = 10.dp,
    textSize: TextUnit = 18.sp,
    function: (Any) -> Unit = {},
    functionName: String = "No action set for this button",
    textColor: Color = MaterialTheme.colorScheme.onBackground
) {
    Card(
        modifier = modifier
            .padding(24.dp)
            .clickable { function(functionName) },
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = textValue,
                color = textColor,
                fontSize = textSize,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

