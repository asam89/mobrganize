package com.lattice.launcher.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lattice.launcher.data.AppInfo
import com.lattice.launcher.data.HomeLayout
import com.lattice.launcher.ui.components.AppGrid

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    apps: List<AppInfo>,
    layout: HomeLayout,
    isOrganizing: Boolean,
    aiAvailable: Boolean,
    error: String?,
    onOrganizeOffline: () -> Unit,
    onOrganizeWithAi: (String) -> Unit,
    onMoveApp: (packageName: String, categoryName: String) -> Unit,
    onAppClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    onErrorDismissed: () -> Unit
) {
    var prompt by remember { mutableStateOf("") }
    var selectedPackage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            onErrorDismissed()
        }
    }

    selectedPackage?.let { packageName ->
        AlertDialog(
            onDismissRequest = { selectedPackage = null },
            title = { Text("Move app") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = apps.firstOrNull { it.packageName == packageName }?.label
                            ?: packageName.substringAfterLast('.'),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    layout.categories.forEach { category ->
                        TextButton(
                            onClick = {
                                onMoveApp(packageName, category.name)
                                selectedPackage = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(category.name)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedPackage = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lattice") },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Button(
                onClick = onOrganizeOffline,
                enabled = !isOrganizing,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                if (isOrganizing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(if (isOrganizing) "Organizing…" else "Organize offline")
            }
            Text(
                text = "Groups apps locally by purpose. No account or API key needed.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )

            if (aiAvailable) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    OutlinedTextField(
                        value = prompt,
                        onValueChange = { prompt = it },
                        placeholder = { Text("Optional AI layout request…") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        enabled = !isOrganizing
                    )
                    IconButton(
                        onClick = {
                            if (prompt.isNotBlank()) {
                                onOrganizeWithAi(prompt)
                                prompt = ""
                            }
                        },
                        enabled = prompt.isNotBlank() && !isOrganizing
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "Organize with AI")
                    }
                }
            } else {
                Text(
                    text = "Custom AI prompts are optional and can be enabled in Settings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }

            Text(
                text = "Long-press an app to move it to another category.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )

            AppGrid(
                layout = layout,
                onAppClick = onAppClick,
                onAppLongClick = { selectedPackage = it },
                modifier = Modifier.weight(1f)
            )
        }
    }
}
