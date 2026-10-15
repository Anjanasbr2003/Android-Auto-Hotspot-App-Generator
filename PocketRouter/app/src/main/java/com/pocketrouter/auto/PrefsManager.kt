package com.pocketrouter.auto

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PrefsManager {
    private const val PREFS_NAME = "pocket_router_direct_boot_prefs"
    private const val KEY_AUTO_START = "auto_start_enabled"
    private const val KEY_BOOT_DELAY = "boot_delay_seconds"
    private const val KEY_TILE_KEYWORDS = "tile_keywords"
    private const val KEY_ONBOARDING_DONE = "onboarding_done"
    private const val KEY_LAST_LOG = "last_action_log"

    private fun getPrefs(context: Context): SharedPreferences {
        val storageContext = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.createDeviceProtectedStorageContext()
        } else {
            context
        }
        return storageContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isAutoStartEnabled(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_AUTO_START, true)

    fun setAutoStartEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_AUTO_START, enabled).apply()
    }

    fun getBootDelaySeconds(context: Context): Int =
        getPrefs(context).getInt(KEY_BOOT_DELAY, 12)

    fun setBootDelaySeconds(context: Context, seconds: Int) {
        getPrefs(context).edit().putInt(KEY_BOOT_DELAY, seconds.coerceIn(5, 60)).apply()
    }

    fun getTileKeywords(context: Context): String =
        getPrefs(context).getString(KEY_TILE_KEYWORDS, "Mobile Hotspot,Hotspot,Wi-Fi hotspot")
            ?: "Mobile Hotspot,Hotspot,Wi-Fi hotspot"

    fun setTileKeywords(context: Context, keywords: String) {
        getPrefs(context).edit().putString(KEY_TILE_KEYWORDS, keywords).apply()
    }

    fun isOnboardingDone(context: Context): Boolean =
        getPrefs(context).getBoolean(KEY_ONBOARDING_DONE, false)

    fun setOnboardingDone(context: Context, done: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_ONBOARDING_DONE, done).apply()
    }

    fun appendLog(context: Context, message: String) {
        val timestamp = SimpleDateFormat("MMM dd, HH:mm:ss", Locale.US).format(Date())
        val entry = "[$timestamp] $message"
        val existing = getPrefs(context).getString(KEY_LAST_LOG, "No events logged yet.") ?: ""
        val lines = (listOf(entry) + existing.lines()).take(12).joinToString("\n")
        getPrefs(context).edit().putString(KEY_LAST_LOG, lines).apply()
    }

    fun getLogs(context: Context): String =
        getPrefs(context).getString(KEY_LAST_LOG, "Ready. Waiting for boot or test trigger.")
            ?: "Ready."
}
