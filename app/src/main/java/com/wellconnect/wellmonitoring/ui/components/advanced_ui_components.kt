package com.wellconnect.wellmonitoring.ui.components

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.wellconnect.wellmonitoring.R
import com.wellconnect.wellmonitoring.data.WellConfigEvents
import com.wellconnect.wellmonitoring.data.WellData
import com.wellconnect.wellmonitoring.utils.PasswordStrength
import com.wellconnect.wellmonitoring.viewmodels.WellViewModel
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


                    // 2. Check for duplicate espId
                    if (userViewModel.isUniqueEspId(well.espId, well.id)) {
                        snackbarHostState.showSnackbar("IP address already used by another well.")
                        return@launch
                    }

                    // 3. Save the data
                    userViewModel.handleConfigEvent(WellConfigEvents.SaveWell(well.id))
                    // 4. Clean up
                    userViewModel.resetWellDataState()
                    onSave()



                    navController.popBackStack()
                    Log.e("SaveDataButton", "Well saved successfully")

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
        TextComponent(text = "Save in order to go back", fontSize = 20.sp)
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
    TextComponent(text = label, fontSize = 18.sp)

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

@Composable

fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isVisible: Boolean,
    onVisibilityChange: () -> Unit,
    passwordStrength: PasswordStrength? = null // Optional parameter
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done
        ),
        trailingIcon = {
            IconButton(onClick = onVisibilityChange) {
                Icon(
                    imageVector = if (isVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    contentDescription = if (isVisible) "Hide password" else "Show password"
                )
            }
        },
        modifier = Modifier.fillMaxWidth()
    )

    // Display password strength if provided
    passwordStrength?.let {
        if (value.isNotEmpty()) {
            LinearProgressIndicator(
                progress = when (it.strength) {
                    "Very Weak" -> 0.2f
                    "Weak" -> 0.4f
                    "Medium" -> 0.6f
                    "Strong" -> 0.8f
                    "Very Strong" -> 1f
                    else -> 0f
                },
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                color = it.color,
            )
            Text(
                text = "${it.strength}: ${it.message}",
                color = it.color,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}