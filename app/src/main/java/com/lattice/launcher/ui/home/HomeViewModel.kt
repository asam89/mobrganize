package com.lattice.launcher.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lattice.launcher.data.AppInfo
import com.lattice.launcher.data.AppRepository
import com.lattice.launcher.data.HomeLayout
import com.lattice.launcher.data.LayoutPlanner
import com.lattice.launcher.data.OfflineOrganizer
import com.lattice.launcher.data.SettingsStore
import com.lattice.launcher.network.AnthropicClient
import com.lattice.launcher.network.ApiMode
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

    private val _proxyUrl = MutableStateFlow("")
    val proxyUrl: StateFlow<String> = _proxyUrl

    private val _useProxy = MutableStateFlow(false)
    val useProxy: StateFlow<Boolean> = _useProxy

    private val _isOrganizing = MutableStateFlow(false)
    val isOrganizing: StateFlow<Boolean> = _isOrganizing

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        viewModelScope.launch {
            settingsStore.apiKey.collect { _apiKey.value = it }
        }
        viewModelScope.launch {
            settingsStore.proxyUrl.collect { _proxyUrl.value = it }
        }
        viewModelScope.launch {
            settingsStore.useProxy.collect { _useProxy.value = it }
        }
        loadApps()
    }

    fun loadApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val installed = appRepository.getInstalledApps()
            val installedPackages = installed.map { it.packageName }.toSet()
            val savedLayout = settingsStore.savedLayout.first()?.let { json ->
                try {
                    LayoutPlanner.sanitize(HomeLayout.deserialize(json), installedPackages)
                } catch (_: Exception) {
                    null
                }
            }

            _apps.value = installed
            _layout.value = savedLayout ?: HomeLayout(
                categories = listOf(
                    HomeLayout.Category(
                        name = "All Apps",
                        packageNames = installed.map { it.packageName }
                    )
                )
            )
        }
    }

    fun saveSettings(apiKey: String, proxyUrl: String, useProxy: Boolean) {
        viewModelScope.launch {
            settingsStore.setApiKey(apiKey)
            settingsStore.setProxyUrl(proxyUrl)
            settingsStore.setUseProxy(useProxy)
            _apiKey.value = apiKey
            _proxyUrl.value = proxyUrl
            _useProxy.value = useProxy
        }
    }

    fun organizeOffline() {
        val installed = _apps.value
        if (installed.isEmpty()) return

        _isOrganizing.value = true
        _error.value = null

        viewModelScope.launch(Dispatchers.Default) {
            try {
                val newLayout = OfflineOrganizer.organize(installed)
                _layout.value = newLayout
                settingsStore.saveLayout(newLayout.serialize())
            } catch (e: Exception) {
                _error.value = e.message ?: "Could not organize apps."
            } finally {
                _isOrganizing.value = false
            }
        }
    }

    fun moveApp(packageName: String, targetCategoryName: String) {
        val currentLayout = _layout.value
        if (currentLayout.categories.none { it.name == targetCategoryName }) return

        val labels = _apps.value.associate { it.packageName to it.label.lowercase() }
        val updatedCategories = currentLayout.categories.map { category ->
            val packagesWithoutApp = category.packageNames.filterNot { it == packageName }
            if (category.name == targetCategoryName) {
                category.copy(
                    packageNames = (packagesWithoutApp + packageName)
                        .distinct()
                        .sortedBy { labels[it] ?: it }
                )
            } else {
                category.copy(packageNames = packagesWithoutApp)
            }
        }.filter { it.packageNames.isNotEmpty() }

        val updatedLayout = currentLayout.copy(categories = updatedCategories)
        _layout.value = updatedLayout
        viewModelScope.launch {
            settingsStore.saveLayout(updatedLayout.serialize())
        }
    }

    fun organizeWithPrompt(prompt: String) {
        val useProxyMode = _useProxy.value
        val key = _apiKey.value
        val proxy = _proxyUrl.value

        val apiMode = if (useProxyMode) {
            if (proxy.isBlank()) {
                _error.value = "Set your proxy URL in Settings first."
                return
            }
            ApiMode.Proxy(proxy)
        } else {
            if (key.isBlank()) {
                _error.value = "Set your Anthropic API key in Settings first."
                return
            }
            ApiMode.DirectKey(key)
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

                val client = AnthropicClient(apiMode)
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
