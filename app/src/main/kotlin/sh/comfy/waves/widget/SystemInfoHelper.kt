package sh.comfy.waves.widget

import android.app.ActivityManager
import android.content.Context
import android.os.BatteryManager
import android.os.Build
import android.os.SystemClock
import android.util.DisplayMetrics
import android.view.WindowManager
import java.io.File

data class SystemInfo(
    val user: String,
    val host: String,
    val distro: String,
    val kernel: String,
    val uptime: String,
    val resolution: String,
    val cpu: String,
    val memUsed: String,
    val memTotal: String,
    val storage: String,
    val battery: String,
)

object SystemInfoHelper {

    fun gather(context: Context): SystemInfo {
        return SystemInfo(
            user = Build.MODEL,
            host = Build.DEVICE,
            distro = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            kernel = System.getProperty("os.version") ?: "unknown",
            uptime = formatUptime(SystemClock.elapsedRealtime()),
            resolution = getResolution(context),
            cpu = getCpuInfo(),
            memUsed = getMemUsed(context),
            memTotal = getMemTotal(context),
            storage = getStorage(),
            battery = getBattery(context),
        )
    }

    fun format(info: SystemInfo): String {
        val tag = "${info.user}@${info.host}"
        val separator = "─".repeat(tag.length.coerceAtMost(30))
        return buildString {
            appendLine(tag)
            appendLine(separator)
            appendLine("OS      ${info.distro}")
            appendLine("Kernel  ${info.kernel}")
            appendLine("Uptime  ${info.uptime}")
            appendLine("Res     ${info.resolution}")
            appendLine("CPU     ${info.cpu}")
            appendLine("Memory  ${info.memUsed} / ${info.memTotal}")
            appendLine("Disk    ${info.storage}")
            append("Battery ${info.battery}")
        }
    }

    private fun formatUptime(millis: Long): String {
        val totalSeconds = millis / 1000
        val days = totalSeconds / 86400
        val hours = (totalSeconds % 86400) / 3600
        val minutes = (totalSeconds % 3600) / 60
        return buildString {
            if (days > 0) append("${days}d ")
            if (hours > 0) append("${hours}h ")
            append("${minutes}m")
        }
    }

    private fun getResolution(context: Context): String {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val bounds = wm.maximumWindowMetrics.bounds
        return "${bounds.width()}x${bounds.height()}"
    }

    private fun getCpuInfo(): String {
        return try {
            val lines = File("/proc/cpuinfo").readLines()
            val hardware = lines.firstOrNull { it.startsWith("Hardware") }
                ?.substringAfter(":")?.trim()
            val model = lines.firstOrNull { it.startsWith("model name") }
                ?.substringAfter(":")?.trim()
            val cores = lines.count { it.startsWith("processor") }
            val name = hardware ?: model ?: Build.HARDWARE
            "$name (${cores}c)"
        } catch (_: Exception) {
            "${Build.HARDWARE} (${Runtime.getRuntime().availableProcessors()}c)"
        }
    }

    private fun getMemUsed(context: Context): String {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memInfo)
        val usedMb = (memInfo.totalMem - memInfo.availMem) / (1024 * 1024)
        return "${usedMb}MB"
    }

    private fun getMemTotal(context: Context): String {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memInfo)
        val totalMb = memInfo.totalMem / (1024 * 1024)
        return "${totalMb}MB"
    }

    private fun getStorage(): String {
        val stat = android.os.StatFs(android.os.Environment.getDataDirectory().path)
        val totalGb = stat.totalBytes / (1024 * 1024 * 1024)
        val freeGb = stat.availableBytes / (1024 * 1024 * 1024)
        val usedGb = totalGb - freeGb
        return "${usedGb}GB / ${totalGb}GB"
    }

    private fun getBattery(context: Context): String {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val charging = bm.isCharging
        return if (charging) "$level% (charging)" else "$level%"
    }
}
