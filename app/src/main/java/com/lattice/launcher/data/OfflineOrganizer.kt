package com.lattice.launcher.data

object OfflineOrganizer {

    private data class CategoryRule(
        val name: String,
        val appCategories: Set<AppCategory> = emptySet(),
        val keywords: Set<String> = emptySet()
    )

    private val categoryRules = listOf(
        CategoryRule(
            name = "Web",
            keywords = setOf("browser", "chrome", "firefox", "opera")
        ),
        CategoryRule(
            name = "Communication",
            keywords = setOf(
                "call", "chat", "contact", "dialer", "email", "gmail", "mail", "meet",
                "message", "messenger", "phone", "signal", "telegram", "text", "whatsapp", "zoom"
            )
        ),
        CategoryRule(
            name = "Work & Productivity",
            appCategories = setOf(AppCategory.PRODUCTIVITY),
            keywords = setOf(
                "asana", "calendar", "docs", "drive", "excel", "github", "jira", "keep",
                "notes", "notion", "office", "onedrive", "powerpoint", "sheets", "slack",
                "teams", "trello", "word"
            )
        ),
        CategoryRule(
            name = "Social",
            appCategories = setOf(AppCategory.SOCIAL),
            keywords = setOf(
                "bluesky", "facebook", "instagram", "linkedin", "mastodon", "pinterest",
                "reddit", "snapchat", "threads", "tiktok", "twitter"
            )
        ),
        CategoryRule(
            name = "Photos & Camera",
            appCategories = setOf(AppCategory.IMAGE),
            keywords = setOf("camera", "gallery", "photo", "photos", "picsart", "snapseed")
        ),
        CategoryRule(
            name = "Entertainment",
            appCategories = setOf(AppCategory.AUDIO, AppCategory.VIDEO),
            keywords = setOf(
                "audible", "disney", "hulu", "music", "netflix", "podcast", "primevideo",
                "soundcloud", "spotify", "twitch", "youtube"
            )
        ),
        CategoryRule(
            name = "Games",
            appCategories = setOf(AppCategory.GAME),
            keywords = setOf("game", "gaming", "playgames")
        ),
        CategoryRule(
            name = "News & Reading",
            appCategories = setOf(AppCategory.NEWS),
            keywords = setOf("book", "kindle", "news", "reader", "reading")
        ),
        CategoryRule(
            name = "Travel",
            appCategories = setOf(AppCategory.MAPS),
            keywords = setOf(
                "airbnb", "airline", "booking", "flight", "lyft", "maps", "transit", "travel", "uber"
            )
        ),
        CategoryRule(
            name = "Shopping",
            keywords = setOf("amazon", "bestbuy", "ebay", "etsy", "shop", "shopping", "store", "walmart")
        ),
        CategoryRule(
            name = "Finance",
            keywords = setOf(
                "bank", "banking", "coinbase", "finance", "invest", "paypal", "stripe", "wallet", "wealth"
            )
        ),
        CategoryRule(
            name = "Health & Fitness",
            keywords = setOf(
                "exercise", "fitbit", "fitness", "googlefit", "health", "meditation", "peloton", "strava", "workout"
            )
        ),
        CategoryRule(
            name = "Utilities",
            appCategories = setOf(AppCategory.ACCESSIBILITY),
            keywords = setOf(
                "authenticator", "calculator", "clock", "files", "safety", "scanner", "security",
                "settings", "simtoolkit", "tools", "vpn", "weather"
            )
        )
    )

    fun organize(apps: List<AppInfo>): HomeLayout {
        if (apps.isEmpty()) return HomeLayout()

        val sortedApps = apps.sortedWith(
            compareBy<AppInfo> { it.label.lowercase() }.thenBy { it.packageName }
        )
        val groupedPackages = linkedMapOf<String, MutableList<String>>()

        for (app in sortedApps) {
            val categoryName = categoryFor(app)
            groupedPackages.getOrPut(categoryName) { mutableListOf() }.add(app.packageName)
        }

        val categories = categoryRules.mapNotNull { rule ->
            groupedPackages[rule.name]?.let { packages ->
                HomeLayout.Category(name = rule.name, packageNames = packages)
            }
        }.toMutableList()

        groupedPackages[OTHER_CATEGORY]?.let { packages ->
            categories.add(HomeLayout.Category(name = OTHER_CATEGORY, packageNames = packages))
        }

        return HomeLayout(categories = categories)
    }

    internal fun categoryFor(app: AppInfo): String {
        val searchable = "${app.label} ${app.packageName}".lowercase()
        return categoryRules.firstOrNull { rule ->
            rule.keywords.any(searchable::contains) || app.category in rule.appCategories
        }?.name ?: OTHER_CATEGORY
    }

    private const val OTHER_CATEGORY = "Other"
}
