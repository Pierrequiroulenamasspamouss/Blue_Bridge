package com.jowell.wellmonitoring.ui


import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.jowell.wellmonitoring.R
import com.jowell.wellmonitoring.data.WellData
import com.jowell.wellmonitoring.ui.screens.Routes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

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

@Composable
fun SaveDataButton(userViewModel: WellViewModel, onSave: () -> Unit, navController: NavController) {
    RectangleButton(

        textValue = "Save",
        modifier = Modifier.height(110.dp).fillMaxWidth(0.5f),
        backgroundColor = colorResource(id = R.color.light_gray),
        elevation = 1.dp,
        textSize = 20.sp,
        function = {
            userViewModel.saveWells(userViewModel.wellList.value)
            onSave() // Updates local state
            navController.popBackStack() // <-- This navigates back immediately
        },
        functionName = "Save Data",
        textColor = MaterialTheme.colorScheme.onBackground
    )
}


@Composable
fun BackButton(navController: NavController, clickable: Boolean = true) {
    val lastClickTime = remember { mutableLongStateOf(0L) }
    val clickCooldown = 100L

    if (!clickable) {
        TextComponent(textValue = "Save in order to go back", textSize = 20.sp)
        return
    }

    RectangleButton(
        textValue = "Go back",
        modifier = Modifier.height(110.dp).fillMaxWidth(1f),
        backgroundColor = colorResource(id = R.color.light_gray),
        elevation = 1.dp,
        textSize = 20.sp,
        function = {
            val currentTime = System.currentTimeMillis()
            val canGoBack = navController.previousBackStackEntry != null

            if (canGoBack && (currentTime - lastClickTime.longValue > clickCooldown)) {
                navController.popBackStack()
                lastClickTime.longValue = currentTime
            } else {
                println("Cannot go back")
            }
        },
        functionName = "Go back",
        textColor = MaterialTheme.colorScheme.onBackground
    )
}


@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun DataAndButtons(
    well: WellData,
    index: Int,
    navController: NavController,
    userViewModel: WellViewModel,
    context: Context,
    scope: CoroutineScope,
    snackbarHostState: SnackbarHostState
) {
    val expanded = remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .border(1.dp, color = MaterialTheme.colorScheme.primary)
            .padding(16.dp)
    ) {
        Column {
            // Conditionally show only non-empty or non-zero fields
            if (well.wellName.isNotBlank()) TextComponent("Name: ${well.wellName}", 20.sp, textFont = FontWeight.Bold)
            if (well.wellOwner.isNotBlank()) TextComponent("Owner: ${well.wellOwner}", 16.sp)
            if (well.wellLocation.isNotBlank()) TextComponent("Location: ${well.wellLocation}", 16.sp)
            if (well.wellWaterType.isNotBlank()) TextComponent("Type: ${well.wellWaterType}", 16.sp)
            if (well.wellCapacity > 0) TextComponent("Capacity: ${well.wellCapacity} L", 16.sp)
            if (well.wellWaterLevel > 0) TextComponent("Level: ${well.wellWaterLevel} L", 16.sp)
            if (well.wellWaterConsumption > 0) TextComponent("Consumption: ${well.wellWaterConsumption} L/day", 16.sp)
            if (well.ipAddress.isNotBlank()) TextComponent("IP: ${well.ipAddress}", 16.sp)
            well.extraData.forEach { (key, value) ->
                TextComponent("$key: ${value.toString().trim('"')}", 16.sp)
            }

        }

        // Triple-dot menu for actions
        Box(modifier = Modifier.align(Alignment.TopEnd)) {
            IconButton(onClick = { expanded.value = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Actions")
            }

            DropdownMenu(
                expanded = expanded.value,
                onDismissRequest = { expanded.value = false }
            ) {
                if (index > 0) {
                    DropdownMenuItem(
                        text = { Text("Move Up") },
                        onClick = {
                            userViewModel.swapWells(index, index - 1)
                            expanded.value = false
                        },
                        leadingIcon = { Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Move Up") }
                    )
                }
                if (index < userViewModel.wellList.value.size - 1) {
                    DropdownMenuItem(
                        text = { Text("Move Down") },
                        onClick = {
                            userViewModel.swapWells(index, index + 1)
                            expanded.value = false
                        },
                        leadingIcon = { Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Move Down") }
                    )
                }
                DropdownMenuItem(
                    text = { Text("Edit") },
                    onClick = {
                        navController.navigate("${Routes.WELL_CONFIG_SCREEN}/${well.id}")
                        expanded.value = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Delete") },
                    onClick = {
                        userViewModel.deleteWell(index)
                        expanded.value = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("Refresh") },
                    onClick = {
                        if (userViewModel.isConnectedToInternet(context)) {
                            userViewModel.fetchWellDataFromESP(well.ipAddress, context)
                        } else {
                            scope.launch {
                                snackbarHostState.showSnackbar("No internet connection available.")
                            }
                        }
                        expanded.value = false
                    }
                )
            }
        }
    }
}
