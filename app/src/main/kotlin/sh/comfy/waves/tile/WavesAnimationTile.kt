package sh.comfy.waves.tile

import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import sh.comfy.waves.wallpaper.WavesWallpaperService

/**
 * Quick Settings tile to toggle wallpaper animation on/off.
 * Shows in notification shade as "Waves" with a wave icon.
 * Tapping sends a broadcast to the wallpaper service.
 */
class WavesAnimationTile : TileService() {

    private var isActive = true

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        isActive = !isActive

        // Send toggle broadcast to wallpaper service
        val intent = Intent(WavesWallpaperService.ACTION_SET_ANIMATION).apply {
            setPackage(packageName)
            putExtra(WavesWallpaperService.EXTRA_ENABLED, isActive)
        }
        sendBroadcast(intent)

        updateTile()
    }

    private fun updateTile() {
        qsTile?.let { tile ->
            tile.state = if (isActive) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            tile.subtitle = if (isActive) "Animating" else "Paused"
            tile.updateTile()
        }
    }
}
