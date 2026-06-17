package com.lattice.launcher

import com.lattice.launcher.data.HomeLayout
import com.lattice.launcher.data.LayoutPlanner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LayoutPlannerTest {

    private val installedPackages = setOf(
        "com.example.browser",
        "com.example.email",
        "com.example.camera",
        "com.example.music",
        "com.example.notes",
        "com.example.calendar"
    )

    // --- extractJson tests ---

    @Test
    fun `extractJson returns JSON from plain object`() {
        val input = """{"categories":[],"hiddenPackages":[]}"""
        val result = LayoutPlanner.extractJson(input)
        assertNotNull(result)
    }

    @Test
    fun `extractJson strips markdown code fences`() {
        val input = """
            Here is the layout:
            ```json
            {"categories":[{"name":"Tools","packageNames":["com.example.browser"]}],"hiddenPackages":[]}
            ```
        """.trimIndent()
        val result = LayoutPlanner.extractJson(input)
        assertNotNull(result)
        assertTrue(result!!.startsWith("{"))
    }

    @Test
    fun `extractJson handles fence without language tag`() {
        val input = """
            ```
            {"categories":[],"hiddenPackages":[]}
            ```
        """.trimIndent()
        assertNotNull(LayoutPlanner.extractJson(input))
    }

    @Test
    fun `extractJson returns null for no JSON`() {
        assertNull(LayoutPlanner.extractJson("no json here"))
    }

    @Test
    fun `extractJson returns null for invalid JSON`() {
        assertNull(LayoutPlanner.extractJson("{broken: json,,,}"))
    }

    @Test
    fun `extractJson handles surrounding prose`() {
        val input = """
            Sure! Here is your layout:
            {"categories":[{"name":"A","packageNames":[]}],"hiddenPackages":[]}
            Hope this helps!
        """.trimIndent()
        val result = LayoutPlanner.extractJson(input)
        assertNotNull(result)
    }

    // --- sanitize tests ---

    @Test
    fun `sanitize removes unknown packages from categories`() {
        val layout = HomeLayout(
            categories = listOf(
                HomeLayout.Category("Tools", listOf("com.example.browser", "com.unknown.app"))
            ),
            hiddenPackages = emptyList()
        )
        val result = LayoutPlanner.sanitize(layout, installedPackages)
        val toolsPackages = result.categories.find { it.name == "Tools" }!!.packageNames
        assertTrue("com.example.browser" in toolsPackages)
        assertTrue("com.unknown.app" !in toolsPackages)
    }

    @Test
    fun `sanitize removes unknown packages from hidden`() {
        val layout = HomeLayout(
            categories = emptyList(),
            hiddenPackages = listOf("com.example.browser", "com.unknown.app")
        )
        val result = LayoutPlanner.sanitize(layout, installedPackages)
        assertTrue("com.example.browser" in result.hiddenPackages)
        assertTrue("com.unknown.app" !in result.hiddenPackages)
    }

    @Test
    fun `sanitize places leftover apps in Unsorted category`() {
        val layout = HomeLayout(
            categories = listOf(
                HomeLayout.Category("Media", listOf("com.example.music", "com.example.camera"))
            ),
            hiddenPackages = emptyList()
        )
        val result = LayoutPlanner.sanitize(layout, installedPackages)
        val unsorted = result.categories.find { it.name == "Unsorted" }
        assertNotNull("Unsorted category should exist for leftover apps", unsorted)
        val unsortedPkgs = unsorted!!.packageNames
        assertTrue("com.example.browser" in unsortedPkgs)
        assertTrue("com.example.email" in unsortedPkgs)
        assertTrue("com.example.notes" in unsortedPkgs)
        assertTrue("com.example.calendar" in unsortedPkgs)
        // Already assigned apps should NOT be in Unsorted
        assertTrue("com.example.music" !in unsortedPkgs)
        assertTrue("com.example.camera" !in unsortedPkgs)
    }

    @Test
    fun `sanitize ensures every installed package in exactly one category or hidden`() {
        val layout = HomeLayout(
            categories = listOf(
                HomeLayout.Category("Productivity", listOf("com.example.email", "com.example.notes")),
                HomeLayout.Category("Media", listOf("com.example.music"))
            ),
            hiddenPackages = listOf("com.example.camera")
        )
        val result = LayoutPlanner.sanitize(layout, installedPackages)

        val allCategorized = result.categories.flatMap { it.packageNames }
        val allHidden = result.hiddenPackages
        val allAccounted = (allCategorized + allHidden).toSet()

        assertEquals(
            "Every installed package must appear exactly once",
            installedPackages,
            allAccounted
        )
        // No duplicates between categories and hidden
        assertEquals(
            "No duplicates across categories + hidden",
            allCategorized.size + allHidden.size,
            allAccounted.size
        )
    }

    @Test
    fun `sanitize deduplicates packages across categories`() {
        val layout = HomeLayout(
            categories = listOf(
                HomeLayout.Category("A", listOf("com.example.browser")),
                HomeLayout.Category("B", listOf("com.example.browser", "com.example.email"))
            ),
            hiddenPackages = emptyList()
        )
        val result = LayoutPlanner.sanitize(layout, installedPackages)
        val allCategorized = result.categories.flatMap { it.packageNames }
        assertEquals(
            "browser should appear only once",
            1,
            allCategorized.count { it == "com.example.browser" }
        )
    }

    @Test
    fun `sanitize removes empty categories`() {
        val layout = HomeLayout(
            categories = listOf(
                HomeLayout.Category("Empty", listOf("com.unknown.only")),
                HomeLayout.Category("Tools", listOf("com.example.browser"))
            ),
            hiddenPackages = emptyList()
        )
        val result = LayoutPlanner.sanitize(layout, installedPackages)
        assertNull(
            "Category with only unknown packages should be removed",
            result.categories.find { it.name == "Empty" }
        )
    }

    @Test
    fun `sanitize with all apps categorized produces no Unsorted`() {
        val layout = HomeLayout(
            categories = listOf(
                HomeLayout.Category("All", installedPackages.toList())
            ),
            hiddenPackages = emptyList()
        )
        val result = LayoutPlanner.sanitize(layout, installedPackages)
        assertNull(
            "No Unsorted when all apps are accounted for",
            result.categories.find { it.name == "Unsorted" }
        )
    }

    @Test
    fun `sanitize with all apps hidden produces no categories except Unsorted-less`() {
        val layout = HomeLayout(
            categories = emptyList(),
            hiddenPackages = installedPackages.toList()
        )
        val result = LayoutPlanner.sanitize(layout, installedPackages)
        assertTrue(
            "No categories needed when all apps hidden",
            result.categories.isEmpty()
        )
        assertEquals(installedPackages.size, result.hiddenPackages.size)
    }

    // --- planLayout end-to-end ---

    @Test
    fun `planLayout full pipeline from markdown-fenced JSON`() {
        val llmResponse = """
            Here's your organized layout:
            ```json
            {
              "categories": [
                {"name": "Productivity", "packageNames": ["com.example.email", "com.example.notes", "com.example.calendar"]},
                {"name": "Media", "packageNames": ["com.example.camera", "com.example.music"]}
              ],
              "hiddenPackages": []
            }
            ```
        """.trimIndent()

        val result = LayoutPlanner.planLayout(llmResponse, installedPackages)
        assertNotNull(result)
        val allCategorized = result!!.categories.flatMap { it.packageNames }.toSet()
        assertEquals(installedPackages, allCategorized + result.hiddenPackages.toSet())
    }

    @Test
    fun `planLayout returns null for unparseable response`() {
        assertNull(LayoutPlanner.planLayout("I don't know what to do", installedPackages))
    }
}
