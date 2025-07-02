package com.bluebridgeapp.bluebridge.ui.screens.miscscreens

import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.bluebridgeapp.bluebridge.R
import com.bluebridgeapp.bluebridge.viewmodels.UserViewModel
import kotlinx.coroutines.launch
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSelectionScreen(navController: NavController, userViewModel: UserViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentLanguage by userViewModel.currentLanguage.collectAsState(initial = "system")

    // List of supported languages with their display names and codes
    val languages = listOf(
        Language("System Default", "system"),
        Language("English", "en"),
        Language("Français (French)", "fr"),
        Language("Español (Spanish)", "es"),
        Language("Deutsch (German)", "de"),
        Language("Italiano (Italian)", "it"),
        Language("Português (Portuguese)", "pt"),
        Language("Русский (Russian)", "ru"),
        Language("中文 (Chinese)", "zh"),
        Language("日本語 (Japanese)", "ja"),
        Language("العربية (Arabic)", "ar"),
        Language("हिन्दी (Hindi)", "hi")
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.select_language)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            items(languages) { language ->
                LanguageItem(
                    language = language,
                    isSelected = currentLanguage == language.code,
                    onClick = {
                        scope.launch {
                            if (language.code == "system") {
                                // Reset to system default
                                val systemLocale = Locale.getDefault()
                                setLocale(context, systemLocale.language)
                                userViewModel.updateLanguage("system")
                            } else {
                                setLocale(context, language.code)
                                userViewModel.updateLanguage(language.code)
                            }
                            navController.popBackStack()
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun LanguageItem(
    language: Language,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = language.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                if (language.code != "system") {
                    Text(
                        text = language.code.uppercase(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = stringResource(R.string.selected),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

private fun setLocale(context: Context, language: String) {
    val locale = if (language == "system") {
        Locale.getDefault()
    } else {
        Locale(language)
    }

    Locale.setDefault(locale)
    val config = Configuration(context.resources.configuration)
    config.setLocale(locale)
    context.createConfigurationContext(config)
    context.resources.updateConfiguration(config, context.resources.displayMetrics)
}

data class Language(
    val displayName: String,
    val code: String
)