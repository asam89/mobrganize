package com.lattice.launcher.data

data class AppInfo(
    val packageName: String,
    val label: String,
    val isSystemApp: Boolean = false,
    val category: AppCategory = AppCategory.OTHER
)
