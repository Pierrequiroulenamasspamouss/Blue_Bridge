package com.bluebridgeapp.bluebridge.ui.dialogs

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.bluebridgeapp.bluebridge.R

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
                    Text(stringResource(R.string.agreement_terms))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(stringResource(R.string.data_privacy_usage))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(stringResource(R.string.privacy_commitment))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(stringResource(R.string.data_collection))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(stringResource(R.string.data_storage))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(stringResource(R.string.location_sharing))
                    // More content...
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.align(Alignment.End),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = onAccept) {
                        Text(stringResource(R.string.i_agree))
                    }
                }
            }
        }
    }
}