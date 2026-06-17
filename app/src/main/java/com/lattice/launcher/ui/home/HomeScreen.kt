package com.lattice.launcher.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lattice.launcher.data.HomeLayout
import com.lattice.launcher.ui.components.AppGrid

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    layout: HomeLayout,
    isOrganizing: Boolean,
    error: String?,
    onOrganize: (String) -> Unit,
    onAppClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    onErrorDismissed: () -> Unit
) {
    var prompt by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            onErrorDismissed()
        }
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    placeholder = { Text("Organize my apps by\u2026") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    enabled = !isOrganizing
                )
                if (isOrganizing) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(start = 8.dp)
                    )
                } else {
                    IconButton(
                        onClick = {
                            if (prompt.isNotBlank()) {
                                onOrganize(prompt)
                                prompt = ""
                            }
                        }
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "Organize")
                    }
                }
            }

            AppGrid(
                layout = layout,
                onAppClick = onAppClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
