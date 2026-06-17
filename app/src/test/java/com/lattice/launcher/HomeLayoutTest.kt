package com.lattice.launcher

import com.lattice.launcher.data.HomeLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeLayoutTest {

    @Test
    fun `serialize and deserialize round-trip preserves data`() {
        val original = HomeLayout(
            categories = listOf(
                HomeLayout.Category("Tools", listOf("com.example.browser", "com.example.email")),
                HomeLayout.Category("Media", listOf("com.example.camera", "com.example.music")),
                HomeLayout.Category("Notes", listOf("com.example.notes"))
            ),
            hiddenPackages = listOf("com.example.hidden")
        )

        val json = original.serialize()
        val restored = HomeLayout.deserialize(json)

        assertEquals(original, restored)
    }

    @Test
    fun `serialize produces valid JSON`() {
        val layout = HomeLayout(
            categories = listOf(
                HomeLayout.Category("A", listOf("com.a"))
            ),
            hiddenPackages = listOf("com.b")
        )
        val json = layout.serialize()
        assertTrue("Should contain categories key", json.contains("\"categories\""))
        assertTrue("Should contain hiddenPackages key", json.contains("\"hiddenPackages\""))
    }

    @Test
    fun `deserialize handles empty layout`() {
        val json = """{"categories":[],"hiddenPackages":[]}"""
        val layout = HomeLayout.deserialize(json)
        assertTrue(layout.categories.isEmpty())
        assertTrue(layout.hiddenPackages.isEmpty())
    }

    @Test
    fun `round-trip preserves category order`() {
        val original = HomeLayout(
            categories = listOf(
                HomeLayout.Category("Zebra", listOf("com.z")),
                HomeLayout.Category("Alpha", listOf("com.a")),
                HomeLayout.Category("Middle", listOf("com.m"))
            ),
            hiddenPackages = emptyList()
        )
        val restored = HomeLayout.deserialize(original.serialize())
        assertEquals(
            listOf("Zebra", "Alpha", "Middle"),
            restored.categories.map { it.name }
        )
    }

    @Test
    fun `round-trip preserves package order within category`() {
        val packages = listOf("com.z", "com.a", "com.m", "com.b")
        val original = HomeLayout(
            categories = listOf(HomeLayout.Category("All", packages)),
            hiddenPackages = emptyList()
        )
        val restored = HomeLayout.deserialize(original.serialize())
        assertEquals(packages, restored.categories.first().packageNames)
    }

    @Test
    fun `deserialize ignores unknown keys`() {
        val json = """{"categories":[],"hiddenPackages":[],"extraField":"value"}"""
        val layout = HomeLayout.deserialize(json)
        assertTrue(layout.categories.isEmpty())
    }

    @Test
    fun `default HomeLayout has empty categories and hidden`() {
        val layout = HomeLayout()
        assertTrue(layout.categories.isEmpty())
        assertTrue(layout.hiddenPackages.isEmpty())
    }

    @Test
    fun `serialize default layout round-trips`() {
        val original = HomeLayout()
        val restored = HomeLayout.deserialize(original.serialize())
        assertEquals(original, restored)
    }
}
