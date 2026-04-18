package com.finfocus.app.presentation.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finfocus.app.R
import com.finfocus.app.ui.theme.FinFocusTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateToContracts: () -> Unit = {},
    onNavigateToCategories: () -> Unit = {},
) {
    val context = LocalContext.current
    val snackbarHost = remember { SnackbarHostState() }
    val showResetDialog = remember { mutableStateOf(false) }

    val folderLabel = viewModel.contractsFolderLabel.collectAsStateWithLifecycle().value
    val isFolderSet = viewModel.isFolderSet.collectAsStateWithLifecycle().value

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHost) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            // ── Данные ─────────────────────────────────────────
            item { SectionHeader(stringResource(R.string.settings_section_data)) }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_categories_title)) },
                    supportingContent = { Text(stringResource(R.string.settings_categories_hint)) },
                    leadingContent = {
                        Icon(Icons.Default.FolderOpen, contentDescription = null)
                    },
                    trailingContent = {
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    },
                    modifier = Modifier.clickable { onNavigateToCategories() },
                )
            }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.contracts_title)) },
                    supportingContent = { Text(stringResource(R.string.settings_contracts)) },
                    leadingContent = {
                        Icon(Icons.Default.FolderOpen, contentDescription = null)
                    },
                    trailingContent = {
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    },
                    modifier = Modifier.clickable { onNavigateToContracts() },
                )
            }

            // Текущая папка хранилища
            item {
                ListItem(
                    headlineContent = {
                        Text(stringResource(R.string.settings_storage_folder))
                    },
                    supportingContent = {
                        Text(
                            text = if (isFolderSet && folderLabel.isNotBlank())
                                folderLabel
                            else
                                stringResource(R.string.folder_not_selected),
                            color = if (isFolderSet)
                                MaterialTheme.colorScheme.onSurfaceVariant
                            else
                                MaterialTheme.colorScheme.error,
                        )
                    },
                    leadingContent = {
                        Icon(Icons.Default.Info, contentDescription = null)
                    },
                )
            }

            item {
                ListItem(
                    headlineContent = {
                        Text(stringResource(R.string.settings_reset_onboarding))
                    },
                    supportingContent = {
                        Text(stringResource(R.string.settings_reset_onboarding_hint))
                    },
                    leadingContent = {
                        Icon(Icons.Default.RestartAlt, contentDescription = null)
                    },
                    modifier = Modifier.clickable { showResetDialog.value = true },
                )
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            // ── Внешний вид ────────────────────────────────────
            item { SectionHeader(stringResource(R.string.settings_section_appearance)) }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_theme_system)) },
                    supportingContent = { Text(stringResource(R.string.settings_theme_coming_soon)) },
                )
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            // ── О приложении ───────────────────────────────────
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.app_name)) },
                    supportingContent = { Text(stringResource(R.string.settings_about_subtitle)) },
                )
            }
        }
    }

    if (showResetDialog.value) {
        AlertDialog(
            onDismissRequest = { showResetDialog.value = false },
            title = { Text(stringResource(R.string.settings_reset_onboarding)) },
            text = { Text(stringResource(R.string.settings_reset_onboarding_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetOnboarding()
                        showResetDialog.value = false
                        // Пересоздаём Activity чтобы MainActivity снова прочитал
                        // onboardingCompleted и показал OnboardingScreen
                        (context as? android.app.Activity)?.recreate()
                    },
                ) { Text(stringResource(R.string.apply_action)) }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog.value = false }) {
                    Text(stringResource(R.string.cancel_action))
                }
            },
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsPreview() {
    FinFocusTheme { SettingsScreen() }
}
