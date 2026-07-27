package com.lattice.launcher

import com.lattice.launcher.data.AppCategory
import com.lattice.launcher.data.AppInfo
import com.lattice.launcher.data.OfflineOrganizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineOrganizerTest {

    @Test
    fun `organize groups apps using package metadata and local keywords`() {
        val apps = listOf(
            AppInfo("com.google.android.gm", "Gmail"),
            AppInfo("com.example.player", "Player", category = AppCategory.AUDIO),
            AppInfo("com.example.navigation", "Navigation", category = AppCategory.MAPS),
            AppInfo("com.example.arcade", "Arcade", category = AppCategory.GAME),
            AppInfo("com.example.unknown", "Acme")
        )

        val layout = OfflineOrganizer.organize(apps)
        val categoryByPackage = layout.categories.flatMap { category ->
            category.packageNames.map { packageName -> packageName to category.name }
        }.toMap()

        assertEquals("Communication", categoryByPackage["com.google.android.gm"])
        assertEquals("Entertainment", categoryByPackage["com.example.player"])
        assertEquals("Travel", categoryByPackage["com.example.navigation"])
        assertEquals("Games", categoryByPackage["com.example.arcade"])
        assertEquals("Other", categoryByPackage["com.example.unknown"])
    }

    @Test
    fun `organize sorts apps alphabetically within each category`() {
        val apps = listOf(
            AppInfo("com.example.zeta", "Zeta Notes", category = AppCategory.PRODUCTIVITY),
            AppInfo("com.example.alpha", "Alpha Notes", category = AppCategory.PRODUCTIVITY),
            AppInfo("com.example.middle", "Middle Notes", category = AppCategory.PRODUCTIVITY)
        )

        val productivity = OfflineOrganizer.organize(apps).categories.single()

        assertEquals("Work & Productivity", productivity.name)
        assertEquals(
            listOf("com.example.alpha", "com.example.middle", "com.example.zeta"),
            productivity.packageNames
        )
    }

    @Test
    fun `organize assigns every app exactly once`() {
        val apps = listOf(
            AppInfo("com.example.social", "Community", category = AppCategory.SOCIAL),
            AppInfo("com.example.camera", "Camera", category = AppCategory.IMAGE),
            AppInfo("com.example.news", "Daily", category = AppCategory.NEWS),
            AppInfo("com.example.other", "Miscellaneous")
        )

        val packages = OfflineOrganizer.organize(apps).categories.flatMap { it.packageNames }

        assertEquals(apps.map { it.packageName }.toSet(), packages.toSet())
        assertEquals(packages.size, packages.distinct().size)
    }

    @Test
    fun `keyword rules can refine generic Android categories`() {
        val app = AppInfo(
            packageName = "com.example.securechat",
            label = "Secure Chat",
            category = AppCategory.SOCIAL
        )

        assertEquals("Communication", OfflineOrganizer.categoryFor(app))
    }

    @Test
    fun `organize returns an empty layout for no installed apps`() {
        val layout = OfflineOrganizer.organize(emptyList())

        assertTrue(layout.categories.isEmpty())
        assertTrue(layout.hiddenPackages.isEmpty())
    }
}
