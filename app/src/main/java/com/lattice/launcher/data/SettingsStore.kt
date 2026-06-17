package com.lattice.launcher.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "lattice_settings")

class SettingsStore(private val context: Context) {

    companion object {
        private val API_KEY = stringPreferencesKey("anthropic_api_key")
        private val PROXY_URL = stringPreferencesKey("proxy_url")
        private val USE_PROXY = booleanPreferencesKey("use_proxy")
        private val SAVED_LAYOUT = stringPreferencesKey("saved_layout")
    }

    val apiKey: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[API_KEY] ?: ""
    }

    val proxyUrl: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[PROXY_URL] ?: ""
    }

    val useProxy: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[USE_PROXY] ?: false
    }

    val savedLayout: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[SAVED_LAYOUT]
    }

    suspend fun setApiKey(key: String) {
        context.dataStore.edit { prefs -> prefs[API_KEY] = key }
    }

    suspend fun setProxyUrl(url: String) {
        context.dataStore.edit { prefs -> prefs[PROXY_URL] = url }
    }

    suspend fun setUseProxy(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[USE_PROXY] = enabled }
    }

    suspend fun saveLayout(json: String) {
        context.dataStore.edit { prefs -> prefs[SAVED_LAYOUT] = json }
    }
}
