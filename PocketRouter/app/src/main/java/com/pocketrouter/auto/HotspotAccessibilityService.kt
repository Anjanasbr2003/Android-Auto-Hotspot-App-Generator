package com.pocketrouter.auto

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class HotspotAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile
        var instance: HotspotAccessibilityService? = null
            private set

        @Volatile
        private var isAutomationRunning = false

        fun isHotspotActive(context: Context): Boolean {
            return try {
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                val method = wifiManager.javaClass.getDeclaredMethod("isWifiApEnabled")
                method.isAccessible = true
                method.invoke(wifiManager) as Boolean
            } catch (e: Exception) {
                false
            }
        }
    }

    private val handler = Handler(Looper.getMainLooper())

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        PrefsManager.appendLog(this, "Accessibility Engine connected & ready.")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Handled deterministically via scheduled step workflow in triggerHotspotSequence
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    fun triggerHotspotSequence(forceEvenIfApCheckTrue: Boolean = false) {
        if (isAutomationRunning) return

        if (!forceEvenIfApCheckTrue && isHotspotActive(this)) {
            PrefsManager.appendLog(this, "Hotspot is already ON. Skipping toggle.")
            return
        }

        isAutomationRunning = true
        wakeUpScreen()
        PrefsManager.appendLog(this, "Opening Quick Settings to enable Hotspot...")

        // Step 1: Dismiss notifications/shade first or open Quick Settings directly
        performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)

        // Step 2: Wait 1.5s for Samsung One UI Core Quick Settings animation, then search & click
        handler.postDelayed({
            val clicked = searchAndClickHotspotTile()
            if (clicked) {
                // Step 3: Wait 1.2s in case Samsung shows a "Turn on Mobile Hotspot?" / "Turn off Wi-Fi?" dialog
                handler.postDelayed({
                    handleConfirmationDialogIfPresent()
                    // Step 4: Collapse the Quick Settings shade back to home
                    handler.postDelayed({
                        performGlobalAction(GLOBAL_ACTION_HOME)
                        isAutomationRunning = false
                        PrefsManager.appendLog(this, "Hotspot activation sequence completed.")
                    }, 1000L)
                }, 1200L)
            } else {
                // Expand Quick Settings a second time (full two-finger swipe state) and retry
                performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
                handler.postDelayed({
                    val retryClicked = searchAndClickHotspotTile()
                    if (retryClicked) {
                        PrefsManager.appendLog(this, "Hotspot tile toggled on 2nd pass.")
                    } else {
                        PrefsManager.appendLog(
                            this,
                            "Could not find Hotspot tile! Ensure 'Mobile Hotspot' is in the top 6 Quick Settings tiles."
                        )
                    }
                    handler.postDelayed({
                        handleConfirmationDialogIfPresent()
                        performGlobalAction(GLOBAL_ACTION_HOME)
                        isAutomationRunning = false
                    }, 1200L)
                }, 1400L)
            }
        }, 1500L)
    }

    private fun wakeUpScreen() {
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            @Suppress("DEPRECATION")
            val wl = pm.newWakeLock(
                PowerManager.FULL_WAKE_LOCK or
                        PowerManager.ACQUIRE_CAUSES_WAKEUP or
                        PowerManager.ON_AFTER_RELEASE,
                "PocketRouter:HotspotWakeLock"
            )
            wl.acquire(10_000L)
        } catch (e: Exception) {
            PrefsManager.appendLog(this, "WakeLock note: ${e.localizedMessage}")
        }
    }

    private fun searchAndClickHotspotTile(): Boolean {
        val keywords = PrefsManager.getTileKeywords(this)
            .split(",")
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }

        val roots = mutableListOf<AccessibilityNodeInfo>()
        rootInActiveWindow?.let { roots.add(it) }
        try {
            for (window in windows) {
                window.root?.let { roots.add(it) }
            }
        } catch (e: Exception) {}

        for (root in roots) {
            val match = findHotspotNodeRecursive(root, keywords)
            if (match != null) {
                if (isTileAlreadyOn(match)) {
                    PrefsManager.appendLog(this, "Tile state already reports ON.")
                    return true
                }
                val clicked = performClickOnNodeOrParent(match)
                if (clicked) {
                    PrefsManager.appendLog(this, "Clicked Mobile Hotspot Quick Settings tile!")
                    return true
                }
            }
        }
        return false
    }

    private fun findHotspotNodeRecursive(
        node: AccessibilityNodeInfo?,
        keywords: List<String>
    ): AccessibilityNodeInfo? {
        if (node == null) return null

        val text = node.text?.toString()?.lowercase() ?: ""
        val desc = node.contentDescription?.toString()?.lowercase() ?: ""
        val pkg = node.packageName?.toString()?.lowercase() ?: ""

        // Do not accidentally click inside our own PocketRouter app window
        if (pkg.contains("com.pocketrouter.auto")) return null

        for (kw in keywords) {
            if (text.contains(kw) || desc.contains(kw)) {
                return node
            }
        }

        for (i in 0 until node.childCount) {
            val found = findHotspotNodeRecursive(node.getChild(i), keywords)
            if (found != null) return found
        }
        return null
    }

    private fun isTileAlreadyOn(node: AccessibilityNodeInfo): Boolean {
        var current: AccessibilityNodeInfo? = node
        var depth = 0
        while (current != null && depth < 4) {
            if (current.isChecked) return true
            val desc = current.contentDescription?.toString()?.lowercase() ?: ""
            val text = current.text?.toString()?.lowercase() ?: ""
            if (desc.contains(", on") || desc.endsWith(" on") || text == "on") {
                return true
            }
            current = current.parent
            depth++
        }
        return false
    }

    private fun performClickOnNodeOrParent(node: AccessibilityNodeInfo): Boolean {
        var current: AccessibilityNodeInfo? = node
        var depth = 0
        while (current != null && depth < 6) {
            if (current.isClickable) {
                return current.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            current = current.parent
            depth++
        }
        return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    private fun handleConfirmationDialogIfPresent() {
        val confirmLabels = listOf("turn on", "ok", "yes", "enable", "allow")
        val root = rootInActiveWindow ?: return
        for (label in confirmLabels) {
            val nodes = root.findAccessibilityNodeInfosByText(label)
            for (n in nodes) {
                val nodeText = n.text?.toString()?.trim()?.lowercase() ?: ""
                if (nodeText in confirmLabels) {
                    performClickOnNodeOrParent(n)
                    PrefsManager.appendLog(this, "Accepted system confirmation dialog ('$nodeText').")
                    return
                }
            }
        }
    }
}
