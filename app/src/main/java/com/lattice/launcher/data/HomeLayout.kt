package com.lattice.launcher.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class HomeLayout(
    val categories: List<Category> = emptyList(),
    val hiddenPackages: List<String> = emptyList()
) {
    @Serializable
    data class Category(
        val name: String,
        val packageNames: List<String> = emptyList()
    )

    fun serialize(): String = jsonCodec.encodeToString(this)

    companion object {
        internal val jsonCodec = Json {
            ignoreUnknownKeys = true
            prettyPrint = false
            encodeDefaults = true
        }

        fun deserialize(json: String): HomeLayout =
            jsonCodec.decodeFromString<HomeLayout>(json)
    }
}
