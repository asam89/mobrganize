package com.lattice.launcher.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Extracts a HomeLayout from LLM JSON and ensures consistency with installed apps.
 */
object LayoutPlanner {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Extracts the first JSON object from a potentially markdown-fenced LLM response.
     * Returns null if no valid JSON object is found.
     */
    internal fun extractJson(raw: String): String? {
        val trimmed = raw.trim()
        // Try stripping markdown code fences first
        val fencePattern = Regex("```(?:json)?\\s*\\n?(.*?)\\n?```", RegexOption.DOT_MATCHES_ALL)
        val fenceMatch = fencePattern.find(trimmed)
        val candidate = fenceMatch?.groupValues?.get(1)?.trim() ?: trimmed

        // Find the first '{' and last '}' to extract JSON object
        val start = candidate.indexOf('{')
        val end = candidate.lastIndexOf('}')
        if (start < 0 || end <= start) return null

        val jsonCandidate = candidate.substring(start, end + 1)
        return try {
            json.parseToJsonElement(jsonCandidate)
            jsonCandidate
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Parses LLM JSON into a HomeLayout, then sanitizes against installed packages.
     */
    internal fun parseLayout(jsonString: String): HomeLayout {
        val root = json.parseToJsonElement(jsonString).jsonObject
        val categories = root["categories"]?.jsonArray?.mapNotNull { element ->
            val obj = element.jsonObject
            val name = obj["name"]?.jsonPrimitive?.content ?: return@mapNotNull null
            val packages = obj["packageNames"]?.jsonArray
                ?.map { it.jsonPrimitive.content }
                ?: emptyList()
            HomeLayout.Category(name = name, packageNames = packages)
        } ?: emptyList()

        val hidden = root["hiddenPackages"]?.jsonArray
            ?.map { it.jsonPrimitive.content }
            ?: emptyList()

        return HomeLayout(categories = categories, hiddenPackages = hidden)
    }

    /**
     * Sanitize a layout against the set of installed packages:
     * - Remove any package from categories/hidden that is not installed.
     * - Any installed package not in any category or hidden goes to "Unsorted".
     * - Remove empty categories.
     * - Every installed package appears in exactly one category OR in hidden.
     */
    internal fun sanitize(layout: HomeLayout, installedPackages: Set<String>): HomeLayout {
        val assigned = mutableSetOf<String>()

        // Filter hidden packages to only installed ones
        val validHidden = layout.hiddenPackages.filter { it in installedPackages }.distinct()
        assigned.addAll(validHidden)

        // Filter categories: keep only installed packages, skip duplicates
        val sanitizedCategories = mutableListOf<HomeLayout.Category>()
        for (category in layout.categories) {
            val validPackages = category.packageNames
                .filter { it in installedPackages && it !in assigned }
                .distinct()
            assigned.addAll(validPackages)
            if (validPackages.isNotEmpty()) {
                sanitizedCategories.add(category.copy(packageNames = validPackages))
            }
        }

        // Collect leftover installed apps not yet assigned
        val unsorted = installedPackages.filter { it !in assigned }.sorted()
        if (unsorted.isNotEmpty()) {
            sanitizedCategories.add(
                HomeLayout.Category(name = "Unsorted", packageNames = unsorted)
            )
        }

        return HomeLayout(
            categories = sanitizedCategories,
            hiddenPackages = validHidden
        )
    }

    /**
     * Full pipeline: extract JSON from LLM response → parse → sanitize.
     */
    fun planLayout(llmResponse: String, installedPackages: Set<String>): HomeLayout? {
        val jsonStr = extractJson(llmResponse) ?: return null
        val parsed = parseLayout(jsonStr)
        return sanitize(parsed, installedPackages)
    }
}
