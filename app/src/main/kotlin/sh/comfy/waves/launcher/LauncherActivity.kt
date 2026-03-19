package sh.comfy.waves.launcher

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import sh.comfy.waves.launcher.data.AppInfo
import sh.comfy.waves.launcher.data.AppRepository
import sh.comfy.waves.launcher.data.DesktopItem
import sh.comfy.waves.launcher.data.DesktopLayout
import sh.comfy.waves.launcher.data.DesktopLayoutStore
import sh.comfy.waves.launcher.data.FolderApp
import sh.comfy.waves.launcher.data.LauncherSettings
import sh.comfy.waves.launcher.dock.Dock
import sh.comfy.waves.launcher.drawer.AppDrawer
import sh.comfy.waves.launcher.folder.FolderPopup
import sh.comfy.waves.launcher.gesture.GestureHandler
import sh.comfy.waves.launcher.home.DesktopGrid
import sh.comfy.waves.launcher.home.DesktopMenu
import sh.comfy.waves.launcher.home.PageIndicator
import sh.comfy.waves.launcher.home.ScrollEffects
import sh.comfy.waves.launcher.search.SearchOverlay
import sh.comfy.waves.ui.theme.WavesTheme

/**
 * Waves Launcher — main home screen activity.
 *
 * Registered as CATEGORY_HOME, replaces the system launcher.
 * Features: paged desktop, app drawer, dock, gestures, search,
 * folders, widget hosting, foldable awareness.
 */
class LauncherActivity : ComponentActivity(), GestureHandler.GestureCallbacks {

    private val settings by lazy { LauncherSettings(this) }
    private var showDrawer by mutableStateOf(false)
    private var showSearch by mutableStateOf(false)
    private var showDesktopMenu by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            WavesTheme {
                LauncherScreen(
                    settings = settings,
                    showDrawer = showDrawer,
                    showSearch = showSearch,
                    showDesktopMenu = showDesktopMenu,
                    callbacks = this@LauncherActivity,
                    onDrawerDismiss = { showDrawer = false },
                    onSearchDismiss = { showSearch = false },
                    onDesktopMenuToggle = { showDesktopMenu = it },
                )
            }
        }
    }

    // Home key pressed while already home — close any open overlays
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        showDrawer = false
        showSearch = false
        showDesktopMenu = false
    }

    override fun onOpenDrawer() { showDrawer = true }
    override fun onOpenSearch() { showSearch = true }
    override fun onOpenOverview() {
        // TODO: overview mode (pinch to show all pages)
    }
    override fun onOpenWidgets() {
        // TODO: widget picker
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LauncherScreen(
    settings: LauncherSettings,
    showDrawer: Boolean,
    showSearch: Boolean,
    showDesktopMenu: Boolean,
    callbacks: GestureHandler.GestureCallbacks,
    onDrawerDismiss: () -> Unit,
    onSearchDismiss: () -> Unit,
    onDesktopMenuToggle: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Load settings
    val desktopCols by settings.desktopColumns.collectAsState(initial = 5)
    val desktopRows by settings.desktopRows.collectAsState(initial = 5)
    val pageCount by settings.desktopPageCount.collectAsState(initial = 5)
    val dockEnabled by settings.dockEnabled.collectAsState(initial = true)
    val dockCols by settings.dockColumns.collectAsState(initial = 5)
    val dockBgAlpha by settings.dockBackground.collectAsState(initial = 0.5f)
    val drawerCols by settings.drawerColumns.collectAsState(initial = 4)
    val iconScale by settings.iconSize.collectAsState(initial = 1.0f)
    val labelSize by settings.labelSize.collectAsState(initial = 12f)
    val showLabels by settings.showLabels.collectAsState(initial = true)
    val showDockLabels by settings.showDockLabels.collectAsState(initial = false)
    val searchEnabled by settings.searchBarEnabled.collectAsState(initial = true)
    val swipeUpAction by settings.swipeUpAction.collectAsState(initial = "drawer")
    val doubleTapAction by settings.doubleTapAction.collectAsState(initial = "lock")
    val swipeDownAction by settings.swipeDownAction.collectAsState(initial = "notifications")
    val scrollEffect by settings.scrollEffect.collectAsState(initial = "slide")

    // Load apps
    var allApps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    LaunchedEffect(Unit) {
        allApps = AppRepository.loadApps(context)
    }

    // Load desktop layout
    var layout by remember { mutableStateOf(DesktopLayoutStore.load(context)) }

    // Dock apps (from layout, resolved against installed apps)
    val dockApps = remember(layout, allApps) {
        layout.dock.mapNotNull { dockItem ->
            allApps.find {
                it.packageName == dockItem.packageName &&
                    it.activityName == dockItem.activityName
            }
        }
    }

    // App resolver for desktop items
    val resolveApp: (String, String) -> AppInfo? = remember(allApps) {
        { pkg, activity -> allApps.find { it.packageName == pkg && it.activityName == activity } }
    }

    // Folder state
    var openFolder by remember { mutableStateOf<DesktopItem.FolderItem?>(null) }

    // Pager state
    val pagerState = rememberPagerState(
        initialPage = currentPageCount / 2,
        pageCount = { currentPageCount },
    )

    // Dynamic page count
    var currentPageCount by remember { mutableStateOf(pageCount) }
    LaunchedEffect(pageCount) { currentPageCount = pageCount }

    // Handle back — close drawer/search/folder/menu
    BackHandler(enabled = showDrawer || showSearch || openFolder != null || showDesktopMenu) {
        when {
            showDesktopMenu -> onDesktopMenuToggle(false)
            showDrawer -> onDrawerDismiss()
            showSearch -> onSearchDismiss()
            openFolder != null -> openFolder = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            // Gesture detection on the home screen
            .pointerInput(swipeUpAction, doubleTapAction, swipeDownAction) {
                detectTapGestures(
                    onDoubleTap = {
                        GestureHandler.execute(
                            context,
                            GestureHandler.resolveAction(doubleTapAction),
                            callbacks,
                        )
                    },
                    onLongPress = {
                        onDesktopMenuToggle(true)
                    },
                )
            }
            .pointerInput(swipeUpAction, swipeDownAction) {
                detectDragGestures { _, dragAmount ->
                    val (dx, dy) = dragAmount
                    if (kotlin.math.abs(dy) > kotlin.math.abs(dx) * 1.5f) {
                        if (dy < -50) {
                            // Swipe up
                            GestureHandler.execute(
                                context,
                                GestureHandler.resolveAction(swipeUpAction),
                                callbacks,
                            )
                        } else if (dy > 50) {
                            // Swipe down
                            GestureHandler.execute(
                                context,
                                GestureHandler.resolveAction(swipeDownAction),
                                callbacks,
                            )
                        }
                    }
                }
            },
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Desktop pages
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) { page ->
                val pageOffset = (pagerState.currentPage - page) +
                    pagerState.currentPageOffsetFraction
                val pageItems = layout.pages.getOrNull(page)?.items ?: emptyList()
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(ScrollEffects.applyEffect(scrollEffect, pageOffset)),
                ) {
                    DesktopGrid(
                        items = pageItems,
                        columns = desktopCols,
                        rows = desktopRows,
                        iconScale = iconScale,
                        labelSize = labelSize,
                        showLabels = showLabels,
                        resolveApp = resolveApp,
                        onItemClick = { item ->
                            when (item) {
                                is DesktopItem.AppShortcut -> {
                                    val app = resolveApp(item.packageName, item.activityName)
                                    app?.let { context.startActivity(it.launchIntent) }
                                }
                                is DesktopItem.FolderItem -> {
                                    openFolder = item
                                }
                                is DesktopItem.WidgetItem -> { /* widgets handle their own clicks */ }
                            }
                        },
                        onItemLongClick = { item ->
                            // TODO: drag-to-reposition / remove / edit
                        },
                        onEmptyCellClick = { col, row ->
                            // TODO: add item picker (app, widget, folder)
                        },
                    )
                }
            }

            // Page indicator
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                PageIndicator(
                    pageCount = pageCount,
                    currentPage = pagerState.currentPage,
                )
            }

            // Dock
            if (dockEnabled) {
                Dock(
                    apps = dockApps,
                    columns = dockCols,
                    backgroundAlpha = dockBgAlpha,
                    showLabels = showDockLabels,
                    onAppClick = { app ->
                        context.startActivity(app.launchIntent)
                    },
                    onAppLongClick = { /* TODO: dock edit */ },
                )
            }
        }

        // App drawer overlay
        AppDrawer(
            visible = showDrawer,
            apps = allApps,
            columns = drawerCols,
            labelSize = labelSize,
            showLabels = showLabels,
            onAppClick = { app ->
                context.startActivity(app.launchIntent)
                onDrawerDismiss()
            },
            onAppLongClick = { /* TODO: add to desktop / dock */ },
            onDismiss = onDrawerDismiss,
            modifier = Modifier.fillMaxSize(),
        )

        // Search overlay
        SearchOverlay(
            visible = showSearch,
            apps = allApps,
            onAppClick = { app ->
                context.startActivity(app.launchIntent)
            },
            onDismiss = onSearchDismiss,
            modifier = Modifier.fillMaxSize(),
        )

        // Folder popup
        openFolder?.let { folder ->
            FolderPopup(
                name = folder.name,
                apps = folder.apps,
                resolveApp = resolveApp,
                onAppClick = { folderApp ->
                    val app = resolveApp(folderApp.packageName, folderApp.activityName)
                    app?.let { context.startActivity(it.launchIntent) }
                    openFolder = null
                },
                onDismiss = { openFolder = null },
            )
        }

        // Desktop long-press menu
        DesktopMenu(
            visible = showDesktopMenu,
            pageCount = currentPageCount,
            onDismiss = { onDesktopMenuToggle(false) },
            onAddPage = {
                currentPageCount++
                scope.launch { settings.setPageCount(currentPageCount) }
            },
            onRemovePage = {
                if (currentPageCount > 1) {
                    currentPageCount--
                    scope.launch { settings.setPageCount(currentPageCount) }
                }
            },
        )
    }
}
