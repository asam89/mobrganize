package com.lattice.launcher.ui.home

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lattice.launcher.ui.settings.SettingsScreen
import com.lattice.launcher.ui.settings.SettingsState
import com.lattice.launcher.ui.theme.LatticeTheme

class HomeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LatticeTheme {
                val vm: HomeViewModel = viewModel()
                val apps by vm.apps.collectAsState()
                val layout by vm.layout.collectAsState()
                val isOrganizing by vm.isOrganizing.collectAsState()
                val error by vm.error.collectAsState()
                val apiKey by vm.apiKey.collectAsState()
                val proxyUrl by vm.proxyUrl.collectAsState()
                val useProxy by vm.useProxy.collectAsState()
                val aiAvailable = if (useProxy) proxyUrl.isNotBlank() else apiKey.isNotBlank()
                var showSettings by remember { mutableStateOf(false) }

                if (showSettings) {
                    SettingsScreen(
                        state = SettingsState(
                            apiKey = apiKey,
                            proxyUrl = proxyUrl,
                            useProxy = useProxy
                        ),
                        onSave = { key, proxy, proxyEnabled ->
                            vm.saveSettings(key, proxy, proxyEnabled)
                            showSettings = false
                        },
                        onBack = { showSettings = false }
                    )
                } else {
                    HomeScreen(
                        apps = apps,
                        layout = layout,
                        isOrganizing = isOrganizing,
                        aiAvailable = aiAvailable,
                        error = error,
                        onOrganizeOffline = { vm.organizeOffline() },
                        onOrganizeWithAi = { prompt -> vm.organizeWithPrompt(prompt) },
                        onMoveApp = { packageName, categoryName ->
                            vm.moveApp(packageName, categoryName)
                        },
                        onAppClick = { packageName -> launchApp(packageName) },
                        onSettingsClick = { showSettings = true },
                        onErrorDismissed = { vm.clearError() }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
    }

    private fun launchApp(packageName: String) {
        val intent = packageManager.getLaunchIntentForPackage(packageName) ?: return
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }
}
