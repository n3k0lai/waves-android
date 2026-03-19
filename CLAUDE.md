# CLAUDE.md — Waves Android

## What This Is

**Waves** (`sh.comfy.waves`) is Nicholai's all-in-one Android personalization app. It replaces Nova Launcher, adds a custom keyboard with 7TV emotes and Chinese input, and provides a live wallpaper using his signature waves aesthetic (drone footage of a person floating in turquoise ocean).

This is a personal daily-driver app, not a product for distribution. Ship fast, break things, iterate.

## Quick Start

```bash
# Debug build (installs on device)
./gradlew assembleDebug

# Install directly via ADB
./gradlew installDebug

# If you need to clean first
./gradlew clean assembleDebug
```

**Requirements:** Android SDK (API 35), JDK 17. Android Studio on this machine has these.

## Architecture

Pure Kotlin, single-module (`app/`), Jetpack Compose UI, no MVVM/DI frameworks (no Hilt, no Dagger). Keep it simple.

```
sh.comfy.waves
├── launcher/          # Home screen launcher (Nova replacement)
│   ├── LauncherActivity.kt    # CATEGORY_HOME entry, singleTask
│   ├── data/                  # AppInfo, DesktopLayout, LauncherSettings, BackupRestore, HiddenApps
│   ├── home/                  # DesktopGrid, ScrollEffects, ContextMenu, PageIndicator, NotificationBadge
│   ├── drawer/                # AppDrawer, DrawerTabs (auto-categorized)
│   ├── dock/                  # Dock (persistent bottom row)
│   ├── folder/                # FolderPopup
│   ├── gesture/               # GestureHandler, SleepService (double-tap-to-lock)
│   ├── search/                # SearchOverlay (type-to-filter + web fallback)
│   └── widget/                # WidgetHost (AppWidgetHost, host ID 1024)
│
├── keyboard/          # IME (custom keyboard)
│   ├── WavesKeyboardService.kt  # InputMethodService entry point
│   ├── KeyboardController.kt    # State machine: QWERTY ↔ SYMBOLS ↔ EMOTES ↔ PINYIN
│   ├── KeyLayout.kt             # Key definitions and layout data
│   ├── core/                    # BackspaceHandler, TextProcessor, KeyboardPreferences (DataStore)
│   ├── emoji/                   # EmoteRepository (fetches from emotes.comfy.sh)
│   ├── pinyin/                  # NinekeyMap, PinyinDictionary, PinyinEngine (九宫格 9-key)
│   └── view/                    # KeyboardView, EmotePanel, NinekeyPanel, WavesBackground
│
├── wallpaper/         # Live wallpaper
│   └── WavesWallpaperService.kt  # OpenGL ES 2.0 + ExoPlayer, focal point crop, foldable hinge splitting
│
├── tile/              # Quick Settings tile (toggle wallpaper animation)
│   └── WavesAnimationTile.kt
│
├── widget/            # Home screen widget
│   ├── WavesWidgetProvider.kt    # Screenfetch-style system info widget
│   └── SystemInfoHelper.kt
│
├── iconpack/          # Icon pack (multi-launcher: Nova, Lawnchair, ADW, Apex, GO)
│   └── IconPackHelper.kt
│
├── ui/
│   ├── settings/      # Compose settings screens (Wallpaper, Keyboard, Launcher, Icon, Widget sections)
│   ├── components/    # FocalPointPicker
│   └── theme/         # Color.kt (waves palette), Theme.kt (always-dark, Material You fallback)
│
├── data/
│   └── SettingsRepository.kt    # SharedPreferences wrapper
│
├── MainActivity.kt    # Settings entry point + icon pack intent filters
└── WavesApp.kt        # Application class
```

## Key Design Decisions

### Waves Palette (hardcoded in `ui/theme/Color.kt`)
- **Primary**: `#00897B` (deep teal ocean)
- **Secondary**: `#D4A574` (sandy gold shoreline)
- **Tertiary**: `#E0F2F1` (white foam/seafoam)
- **Surfaces**: `#0A0A0A` / `#1A1A1A` (void black)
- **Accent**: `#00D4AA` (Ene highlight)
- Always dark theme. No light mode. Material You dynamic colors as fallback.

### Emote System
- Emotes load from `https://emotes.comfy.sh/api/emotes.json` (self-hosted 7TV proxy)
- 419 emotes from Nicholai's Twitch 7TV collection
- Keyboard emote panel shows grid, tap inserts CDN URL (image-paste for apps that support it)
- Emote data structure: `{ name, url, animated }` — CDN URLs are 7TV hosted

### Chinese Input (九宫格 Pinyin)
- 9-key T9-style pinyin, NOT full QWERTY pinyin
- Dictionary: `assets/pinyin_dict.json` (400+ syllables, frequency-ordered characters)
- Flow: digit taps → syllable candidates → character candidates → commit

### Launcher
- HorizontalPager desktop with configurable grid (3-9 cols/rows)
- 8 scroll effects (Cube, Stack, Tablet, Zoom, Rotate, Flip, Accordion, Slide)
- App drawer: slide-up overlay, search bar, auto-categorized tabs
- Desktop layout persisted via kotlinx.serialization JSON
- Notification badges via NotificationListenerService

### Live Wallpaper
- `res/raw/waves.mp4` — the signature drone footage loop
- OpenGL ES 2.0 renders video frames as textures
- Focal point picker in settings for crop positioning
- Foldable hinge detection via WindowInfoTracker (rendering split not yet implemented)

## Build System

- **Gradle** with Kotlin DSL (`.gradle.kts`)
- **Version catalog**: `gradle/libs.versions.toml`
- **AGP**: 8.7.3, **Kotlin**: 2.0.21, **Compose BOM**: 2024.12.01
- **Min SDK**: 31 (Android 12), **Target/Compile SDK**: 35
- **Key deps**: Compose, Media3 ExoPlayer, Coil (image loading), DataStore, Window Manager, kotlinx-serialization

### Release Builds
- ProGuard/R8 enabled (`isMinifyEnabled = true`, `isShrinkResources = true`)
- Keep rules in `app/proguard-rules.pro` for all services, serialization, etc.
- **No signing config yet** — release builds need a keystore. For now, use debug builds.

## What Works (never tested on device yet)
- All code compiles (last verified: debug build on CI... theoretically)
- Wallpaper, keyboard, launcher, widget, icon pack, QS tile all have manifest registrations
- All settings screens exist with DataStore persistence

## Known Issues / TODOs
- **No signing config** — `assembleRelease` fails. Need to generate a keystore
- **Never tested on device** — first install will likely surface runtime issues
- Launcher: no drag-and-drop to reposition desktop items
- Launcher: no widget picker / resize handles
- Launcher: icon pack not applied to launcher icons yet
- Foldable: hinge detection wired but GL viewport not split
- Keyboard: emote clipboard copies URL text, not actual image (platform limitation)
- Wallpaper: `waves.mp4` is bundled in APK (~large), could stream instead

## Testing on Device

1. Connect phone via USB, enable USB debugging
2. `./gradlew installDebug`
3. Set as default launcher: Settings → Home app → Waves
4. Enable keyboard: Settings → Languages & input → On-screen keyboard → Waves Keyboard
5. Set wallpaper: long-press home → Wallpapers → Live wallpapers → Waves
6. Grant notification access: Settings → Notifications → Notification access → Waves Badge Service
7. Grant accessibility: Settings → Accessibility → Waves Screen Lock (for double-tap-to-lock)

## Style Guide

- Kotlin, idiomatic. No Java.
- Compose for all UI. No XML layouts (except Android-mandated: widget, wallpaper, keyboard XML configs).
- Keep it flat — avoid unnecessary abstractions. This is a personal app, not enterprise software.
- `object` singletons are fine. No DI framework needed.
- Prefer `remember` + `mutableStateOf` over ViewModel for simple state.
- Comments for "why", not "what".
