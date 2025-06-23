package com.bluebridge.bluebridgeapp.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun ScrollableEULADialog(
    onDismiss: () -> Unit,
    onAccept: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "End-User License Agreement (EULA)",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .weight(1f, false)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text("1. Agreement terms...")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("2. Data Privacy and Usage:")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("   a. We are committed to protecting your privacy. We do not sell, trade, or otherwise transfer to outside parties your personally identifiable information.")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("   b. We only collect and store data that is necessary for the proper functioning of the application and to improve your user experience. This may include anonymized usage statistics and preferences.")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("   c. Any data collected is stored securely and handled in accordance with applicable data protection laws.")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("   c. When using this app you agree to share your location with other users. You can change this anytime in the settings. But your defined location will be available for anyone to see. This is not your real-time location but one you chose to make public. ")
                    // More content...
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.align(Alignment.End),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = onAccept) {
                        Text("I Agree")
                    }
                }
            }
        }
    }
}