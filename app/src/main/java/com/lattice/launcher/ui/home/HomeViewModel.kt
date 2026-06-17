package com.lattice.launcher.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lattice.launcher.data.AppInfo
import com.lattice.launcher.data.AppRepository
import com.lattice.launcher.data.HomeLayout
import com.lattice.launcher.data.LayoutPlanner
import com.lattice.launcher.data.SettingsStore
import com.lattice.launcher.network.AnthropicClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val appRepository = AppRepository(application)
    private val settingsStore = SettingsStore(application)

    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps

    private val _layout = MutableStateFlow(HomeLayout())
    val layout: StateFlow<HomeLayout> = _layout

    private val _apiKey = MutableStateFlow("")
    val apiKey: StateFlow<String> = _apiKey

    private val _isOrganizing = MutableStateFlow(false)
    val isOrganizing: StateFlow<Boolean> = _isOrganizing

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        viewModelScope.launch {
            settingsStore.apiKey.collect { _apiKey.value = it }
        }
        loadApps()
        loadSavedLayout()
    }

    fun loadApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val installed = appRepository.getInstalledApps()
            _apps.value = installed

            if (_layout.value.categories.isEmpty()) {
                _layout.value = HomeLayout(
                    categories = listOf(
                        HomeLayout.Category(
                            name = "All Apps",
                            packageNames = installed.map { it.packageName }
                        )
                    )
                )
            }
        }
    }

    private fun loadSavedLayout() {
        viewModelScope.launch {
            settingsStore.savedLayout.first()?.let { json ->
                try {
                    val saved = HomeLayout.deserialize(json)
                    val installed = _apps.value.map { it.packageName }.toSet()
                    _layout.value = LayoutPlanner.sanitize(saved, installed)
                } catch (_: Exception) { /* use default */ }
            }
        }
    }

    fun saveApiKey(key: String) {
        viewModelScope.launch {
            settingsStore.setApiKey(key)
            _apiKey.value = key
        }
    }

    fun organizeWithPrompt(prompt: String) {
        val key = _apiKey.value
        if (key.isBlank()) {
            _error.value = "Set your Anthropic API key in Settings first."
            return
        }
        _isOrganizing.value = true
        _error.value = null

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val installed = _apps.value.map { it.packageName }.toSet()
                val appList = _apps.value.joinToString("\n") { "${it.packageName} — ${it.label}" }
                val systemPrompt = buildString {
                    append("You are a home-screen organizer. The user has these apps:\n")
                    append(appList)
                    append("\n\nRespond with ONLY a JSON object: ")
                    append("{\"categories\":[{\"name\":\"...\",\"packageNames\":[...]}],")
                    append("\"hiddenPackages\":[...]}")
                }

                val client = AnthropicClient(key)
                val response = client.sendMessage(
                    userPrompt = prompt,
                    systemPrompt = systemPrompt
                )

                if (response == null) {
                    _error.value = "Failed to get a response from Anthropic."
                    return@launch
                }

                val newLayout = LayoutPlanner.planLayout(response, installed)
                if (newLayout == null) {
                    _error.value = "Could not parse the AI response."
                    return@launch
                }

                _layout.value = newLayout
                settingsStore.saveLayout(newLayout.serialize())
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error"
            } finally {
                _isOrganizing.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
