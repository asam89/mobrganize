package com.lattice.launcher.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager

class AppRepository(private val context: Context) {

    fun getInstalledApps(): List<AppInfo> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        return pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .mapNotNull { resolveInfo ->
                val appInfo = resolveInfo.activityInfo?.applicationInfo ?: return@mapNotNull null
                val packageName = appInfo.packageName
                // Exclude ourselves
                if (packageName == context.packageName) return@mapNotNull null
                AppInfo(
                    packageName = packageName,
                    label = appInfo.loadLabel(pm).toString(),
                    isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                    category = appInfo.category.toAppCategory()
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    private fun Int.toAppCategory(): AppCategory = when (this) {
        ApplicationInfo.CATEGORY_GAME -> AppCategory.GAME
        ApplicationInfo.CATEGORY_AUDIO -> AppCategory.AUDIO
        ApplicationInfo.CATEGORY_VIDEO -> AppCategory.VIDEO
        ApplicationInfo.CATEGORY_IMAGE -> AppCategory.IMAGE
        ApplicationInfo.CATEGORY_SOCIAL -> AppCategory.SOCIAL
        ApplicationInfo.CATEGORY_NEWS -> AppCategory.NEWS
        ApplicationInfo.CATEGORY_MAPS -> AppCategory.MAPS
        ApplicationInfo.CATEGORY_PRODUCTIVITY -> AppCategory.PRODUCTIVITY
        ApplicationInfo.CATEGORY_ACCESSIBILITY -> AppCategory.ACCESSIBILITY
        else -> AppCategory.OTHER
    }
}
