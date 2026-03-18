package sh.comfy.waves.launcher.drawer

import sh.comfy.waves.launcher.data.AppInfo

/**
 * App drawer tab system.
 * Nova-style: custom tabs + auto-categorization.
 */
data class DrawerTab(
    val name: String,
    val filter: (AppInfo) -> Boolean,
)

object DrawerTabs {

    /**
     * Default auto-generated tabs based on app categories.
     */
    fun defaultTabs(): List<DrawerTab> = listOf(
        DrawerTab("All") { true },
        DrawerTab("Recent") { false }, // Populated by usage tracking
        DrawerTab("Games") { app ->
            // Simple heuristic: check package name patterns
            val pkg = app.packageName.lowercase()
            pkg.contains("game") || pkg.contains("play") ||
                pkg.contains("puzzle") || pkg.contains("chess") ||
                pkg.contains("minecraft") || pkg.contains("unity")
        },
        DrawerTab("Social") { app ->
            val pkg = app.packageName.lowercase()
            pkg.contains("whatsapp") || pkg.contains("telegram") ||
                pkg.contains("discord") || pkg.contains("instagram") ||
                pkg.contains("twitter") || pkg.contains("facebook") ||
                pkg.contains("signal") || pkg.contains("messenger")
        },
        DrawerTab("Media") { app ->
            val pkg = app.packageName.lowercase()
            pkg.contains("youtube") || pkg.contains("spotify") ||
                pkg.contains("music") || pkg.contains("video") ||
                pkg.contains("player") || pkg.contains("camera") ||
                pkg.contains("photo") || pkg.contains("gallery") ||
                pkg.contains("twitch") || pkg.contains("netflix")
        },
        DrawerTab("Tools") { app ->
            val pkg = app.packageName.lowercase()
            pkg.contains("calculator") || pkg.contains("clock") ||
                pkg.contains("calendar") || pkg.contains("settings") ||
                pkg.contains("file") || pkg.contains("browser") ||
                pkg.contains("chrome") || pkg.contains("firefox")
        },
    )

    /**
     * Filter apps for a specific tab.
     */
    fun appsForTab(tab: DrawerTab, allApps: List<AppInfo>): List<AppInfo> {
        return allApps.filter(tab.filter)
    }
}
