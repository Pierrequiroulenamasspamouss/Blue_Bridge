package com.jowell.wellmonitoring.ui.theme

import android.content.Context
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.jowell.wellmonitoring.R
import com.jowell.wellmonitoring.data.WellConfigEvents
import com.jowell.wellmonitoring.data.WellData
import com.jowell.wellmonitoring.ui.NumbersFieldComponent
import com.jowell.wellmonitoring.ui.RectangleButton
import com.jowell.wellmonitoring.ui.TextComponent
import com.jowell.wellmonitoring.ui.TextFieldComponent
import com.jowell.wellmonitoring.ui.WellViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SaveDataButton(
    userViewModel: WellViewModel,
    onSave: () -> Unit,
    navController: NavController,
    well: WellData,
    context: Context,
    snackbarHostState: SnackbarHostState
) {
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    RectangleButton(
        textValue = "Save",
        modifier = Modifier
            .height(110.dp)
            .fillMaxWidth(0.5f),
        backgroundColor = colorResource(id = R.color.light_gray),
        elevation = 1.dp,
        textSize = 20.sp,
        function = {
            focusManager.clearFocus(force = true)

            coroutineScope.launch {
                try {
                    // 1. Validate input
                    if (well.ipAddress.isBlank()) {
                        snackbarHostState.showSnackbar("IP address cannot be empty")
                        return@launch
                    }

                    // 2. Check for duplicate IP
                    if (userViewModel.isIpAddressDuplicate(well.ipAddress, well.id)) {
                        snackbarHostState.showSnackbar("IP address already used by another well.")
                        return@launch
                    }

                    // 3. Save the data
                    userViewModel.onConfigEvent(WellConfigEvents.SaveWell(well.id))

                    // 4. Clean up
                    userViewModel.clearWellData()
                    onSave()

                    // 5. Navigate back first
                    navController.popBackStack()

                    // 6. Then refresh the specific well after a small delay
                    delay(300) // Small delay to ensure navigation completes
                    val success = userViewModel.refreshSingleWell(well.id, context)

                    if (!success) {
                        snackbarHostState.showSnackbar("Data saved but refresh failed")
                    }
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Save failed: ${e.localizedMessage}")
                }
            }
        },
        functionName = "Save Data",
        textColor = MaterialTheme.colorScheme.onBackground
    )
}

@Composable
fun BackButton(
    navController: NavController,
    userViewModel: WellViewModel,
    clickable: Boolean = true,
    onBack: (() -> Unit)? = null
) {
    val lastClickTime = remember { mutableLongStateOf(0L) }
    val clickCooldown = 100L

    if (!clickable) {
        TextComponent(textValue = "Save in order to go back", textSize = 20.sp)
        return
    }

    RectangleButton(
        textValue = "Go back",
        modifier = Modifier
            .height(110.dp)
            .fillMaxWidth(1f),
        backgroundColor = colorResource(id = R.color.light_gray),
        elevation = 1.dp,
        textSize = 20.sp,
        function = {
            val currentTime = System.currentTimeMillis()
            val canGoBack = navController.previousBackStackEntry != null

            if (canGoBack && (currentTime - lastClickTime.longValue > clickCooldown)) {
                onBack?.invoke() ?: navController.popBackStack()
                lastClickTime.longValue = currentTime
            } else {
                println("Cannot go back")
            }
        },
        functionName = "Go back",
        textColor = MaterialTheme.colorScheme.onBackground
    )
}


@Composable
fun <T> WellField(
    label: String,
    value: T,
    keyId: Int,
    onValueChange: (String) -> Unit,
    isNumeric: Boolean = false
) {
    Spacer(modifier = Modifier.size(10.dp))
    TextComponent(textValue = label, textSize = 18.sp)

    key(keyId) {
        if (isNumeric) {
            NumbersFieldComponent(
                initialValue = value.toString(),
                defaultInputMessage = label,
                onTextChanged = onValueChange
            )
        } else {
            TextFieldComponent(
                initialValue = value.toString(),
                defaultInputMessage = label,
                onTextChanged = onValueChange
            )
        }
    }
}


