#!/usr/bin/env python3
import os
import stat

PROJECT_ROOT = "PocketRouter"

FILES = {
    # ---------------------------------------------------------
    # 1. GRADLE CONFIGURATION FILES
    # ---------------------------------------------------------
    "settings.gradle.kts": """pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "PocketRouter"
include(":app")
""",

    "build.gradle.kts": """plugins {
    id("com.android.application") version "8.2.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
}
""",

    "gradle.properties": """org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
kotlin.code.style=official
android.nonTransitiveRClass=true
""",

    "app/build.gradle.kts": """plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.pocketrouter.auto"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.pocketrouter.auto"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
}
""",

    "app/proguard-rules.pro": """# Keep Accessibility and Boot classes
-keep class com.pocketrouter.auto.** { *; }
""",

    # ---------------------------------------------------------
    # 2. ANDROID MANIFEST
    # ---------------------------------------------------------
    "app/src/main/AndroidManifest.xml": """<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
    <uses-permission android:name="android.permission.WAKE_LOCK" />
    <uses-permission android:name="android.permission.DISABLE_KEYGUARD" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
    <uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

    <application
        android:allowBackup="true"
        android:directBootAware="true"
        android:icon="@drawable/ic_router_logo"
        android:label="PocketRouter AutoHotspot"
        android:roundIcon="@drawable/ic_router_logo"
        android:supportsRtl="true"
        android:theme="@style/Theme.MaterialComponents.DayNight.NoActionBar"
        tools:targetApi="34">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:launchMode="singleTop">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <!-- Direct-Boot Aware Receiver for Samsung Galaxy M11 -->
        <receiver
            android:name=".BootReceiver"
            android:directBootAware="true"
            android:enabled="true"
            android:exported="true">
            <intent-filter android:priority="999">
                <action android:name="android.intent.action.LOCKED_BOOT_COMPLETED" />
                <action android:name="android.intent.action.BOOT_COMPLETED" />
                <action android:name="android.intent.action.QUICKBOOT_POWERON" />
                <action android:name="com.htc.intent.action.QUICKBOOT_POWERON" />
                <action android:name="android.intent.action.MY_PACKAGE_REPLACED" />
            </intent-filter>
        </receiver>

        <!-- Foreground Watchdog & Local Web Telemetry Server -->
        <service
            android:name=".RouterWatchdogService"
            android:directBootAware="true"
            android:enabled="true"
            android:exported="false"
            android:foregroundServiceType="specialUse">
            <property
                android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"
                android:value="Automated Wi-Fi Hotspot boot recovery and router watchdog" />
        </service>

        <!-- Accessibility Engine to Toggle Quick Settings Hotspot Tile -->
        <service
            android:name=".HotspotAccessibilityService"
            android:directBootAware="true"
            android:enabled="true"
            android:exported="true"
            android:label="PocketRouter Auto-Hotspot Engine"
            android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE">
            <intent-filter>
                <action android:name="android.accessibilityservice.AccessibilityService" />
            </intent-filter>
            <meta-data
                android:name="android.accessibilityservice"
                android:resource="@xml/accessibility_service_config" />
        </service>

    </application>
</manifest>
""",

    # ---------------------------------------------------------
    # 3. AI-DESIGNED VECTOR ICON & XML RESOURCES
    # ---------------------------------------------------------
    "app/src/main/res/drawable/ic_router_logo.xml": """<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <!-- Deep Cyber Navy Background Circle -->
    <path
        android:fillColor="#0F172A"
        android:pathData="M54,54m-50,0a50,50 0,1 1,100 0a50,50 0,1 1,-100 0" />
    <!-- Outer Glow Ring -->
    <path
        android:strokeColor="#00E5FF"
        android:strokeWidth="2.5"
        android:fillColor="#00000000"
        android:pathData="M54,54m-45,0a45,45 0,1 1,90 0a45,45 0,1 1,-90 0" />
    <!-- Outer Wi-Fi Signal Arc -->
    <path
        android:strokeColor="#00E5FF"
        android:strokeWidth="4"
        android:strokeLineCap="round"
        android:fillColor="#00000000"
        android:pathData="M30,38 C43,25 65,25 78,38" />
    <!-- Inner Wi-Fi Signal Arc -->
    <path
        android:strokeColor="#10B981"
        android:strokeWidth="4"
        android:strokeLineCap="round"
        android:fillColor="#00000000"
        android:pathData="M39,47 C47,39 61,39 69,47" />
    <!-- Center Antenna Node -->
    <path
        android:fillColor="#10B981"
        android:pathData="M54,54m-4,0a4,4 0,1 1,8 0a4,4 0,1 1,-8 0" />
    <!-- Router Base Body -->
    <path
        android:fillColor="#1E293B"
        android:strokeColor="#38BDF8"
        android:strokeWidth="2.5"
        android:pathData="M28,64 L80,64 C83,64 85,66 85,69 L85,79 C85,82 83,84 80,84 L28,84 C25,84 23,82 23,79 L23,69 C23,66 25,64 28,64 Z" />
    <!-- Status LEDs on Router -->
    <path
        android:fillColor="#10B981"
        android:pathData="M35,74m-3,0a3,3 0,1 1,6 0a3,3 0,1 1,-6 0" />
    <path
        android:fillColor="#00E5FF"
        android:pathData="M47,74m-3,0a3,3 0,1 1,6 0a3,3 0,1 1,-6 0" />
    <path
        android:fillColor="#F59E0B"
        android:pathData="M73,74m-3,0a3,3 0,1 1,6 0a3,3 0,1 1,-6 0" />
</vector>
""",

    "app/src/main/res/xml/accessibility_service_config.xml": """<?xml version="1.0" encoding="utf-8"?>
<accessibility-service xmlns:android="http://schemas.android.com/apk/res/android"
    android:accessibilityEventTypes="typeWindowStateChanged|typeWindowContentChanged"
    android:accessibilityFeedbackType="feedbackGeneric"
    android:accessibilityFlags="flagDefault|flagRetrieveInteractiveWindows|flagReportViewIds|flagIncludeNotImportantViews"
    android:canPerformGestures="true"
    android:canRetrieveWindowContent="true"
    android:description="@string/accessibility_service_description"
    android:notificationTimeout="100" />
""",

    "app/src/main/res/values/strings.xml": """<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">PocketRouter AutoHotspot</string>
    <string name="accessibility_service_description">Automatically turns on the Mobile Hotspot Quick Settings tile when your Samsung Galaxy M11 powers on after a battery drain or reboot.</string>
</resources>
""",

    # ---------------------------------------------------------
    # 4. KOTLIN SOURCE CODE: DIRECT-BOOT PREFERENCES MANAGER
    # ---------------------------------------------------------
    "app/src/main/java/com/pocketrouter/auto/PrefsManager.kt": """package com.pocketrouter.auto

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
        val lines = (listOf(entry) + existing.lines()).take(12).joinToString("\\n")
        getPrefs(context).edit().putString(KEY_LAST_LOG, lines).apply()
    }

    fun getLogs(context: Context): String =
        getPrefs(context).getString(KEY_LAST_LOG, "Ready. Waiting for boot or test trigger.")
            ?: "Ready."
}
""",

    # ---------------------------------------------------------
    # 5. KOTLIN SOURCE CODE: ACCESSIBILITY HOTSPOT ENGINE
    # ---------------------------------------------------------
    "app/src/main/java/com/pocketrouter/auto/HotspotAccessibilityService.kt": """package com.pocketrouter.auto

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
        } catch (_: Exception) {}

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
""",

    # ---------------------------------------------------------
    # 6. KOTLIN SOURCE CODE: FOREGROUND WATCHDOG & WEB DASHBOARD
    # ---------------------------------------------------------
    "app/src/main/java/com/pocketrouter/auto/RouterWatchdogService.kt": """package com.pocketrouter.auto

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import kotlin.concurrent.thread

class RouterWatchdogService : Service() {

    companion object {
        const val ACTION_BOOT_TRIGGER = "com.pocketrouter.auto.ACTION_BOOT_TRIGGER"
        const val ACTION_MANUAL_TEST = "com.pocketrouter.auto.ACTION_MANUAL_TEST"
        private const val CHANNEL_ID = "pocket_router_watchdog_channel"
        private const val NOTIFICATION_ID = 1001
    }

    private val handler = Handler(Looper.getMainLooper())
    @Volatile
    private var serverSocket: ServerSocket? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, buildNotification("PocketRouter Watchdog Active (Port 8080)"))
        startEmbeddedWebServer()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_BOOT_TRIGGER -> {
                if (PrefsManager.isAutoStartEnabled(this)) {
                    val delaySec = PrefsManager.getBootDelaySeconds(this)
                    PrefsManager.appendLog(
                        this,
                        "Device boot detected! Waiting ${delaySec}s for cellular network..."
                    )
                    handler.postDelayed({
                        executeHotspotEnable(force = false)
                    }, delaySec * 1000L)
                }
            }
            ACTION_MANUAL_TEST -> {
                PrefsManager.appendLog(this, "Manual test triggered by user.")
                executeHotspotEnable(force = true)
            }
        }
        return START_STICKY
    }

    private fun executeHotspotEnable(force: Boolean) {
        val a11y = HotspotAccessibilityService.instance
        if (a11y != null) {
            a11y.triggerHotspotSequence(forceEvenIfApCheckTrue = force)
        } else {
            PrefsManager.appendLog(
                this,
                "Waiting for Accessibility Service to bind (retrying in 4s)..."
            )
            handler.postDelayed({
                HotspotAccessibilityService.instance?.triggerHotspotSequence(
                    forceEvenIfApCheckTrue = force
                ) ?: PrefsManager.appendLog(
                    this,
                    "ERROR: Accessibility Service is not enabled in Android Settings!"
                )
            }, 4000L)
        }
    }

    private fun startEmbeddedWebServer() {
        thread(isDaemon = true, name = "PocketRouterWebDashboard") {
            try {
                val socket = ServerSocket(8080)
                serverSocket = socket
                while (!socket.isClosed) {
                    val client = socket.accept()
                    thread(isDaemon = true) {
                        try {
                            val reader = BufferedReader(InputStreamReader(client.getInputStream()))
                            val requestLine = reader.readLine() ?: ""
                            if (requestLine.contains("GET /trigger")) {
                                handler.post { executeHotspotEnable(force = true) }
                            }
                            val batteryPct = getBatteryPercentage()
                            val hotspotOn = HotspotAccessibilityService.isHotspotActive(this)
                            val logs = PrefsManager.getLogs(this)
                            val html = \"\"\"
                                <!DOCTYPE html>
                                <html>
                                <head>
                                  <meta charset="utf-8"/>
                                  <meta name="viewport" content="width=device-width,initial-scale=1"/>
                                  <title>PocketRouter M11 Dashboard</title>
                                  <style>
                                    body { background:#0F172A; color:#F8FAFC; font-family:sans-serif; padding:24px; }
                                    .card { background:#1E293B; border-radius:12px; padding:20px; margin-bottom:16px; border:1px solid #334155; }
                                    .badge { display:inline-block; padding:6px 12px; border-radius:6px; font-weight:bold; background:${if (hotspotOn) "#065F46" else "#7F1D1D"}; }
                                    a.btn { display:inline-block; background:#0284C7; color:white; padding:10px 16px; border-radius:8px; text-decoration:none; font-weight:bold; }
                                    pre { background:#090D16; padding:12px; border-radius:8px; overflow-x:auto; color:#38BDF8; }
                                  </style>
                                </head>
                                <body>
                                  <h2>PocketRouter Galaxy M11 Control Panel</h2>
                                  <div class="card">
                                    <p><strong>Hotspot Status:</strong> <span class="badge">${if (hotspotOn) "ACTIVE" else "STANDBY / CHECKING"}</span></p>
                                    <p><strong>Battery Level:</strong> $batteryPct%</p>
                                    <p><a class="btn" href="/trigger">Re-Trigger Hotspot Toggle</a></p>
                                  </div>
                                  <div class="card">
                                    <h3>Recent Automation Logs</h3>
                                    <pre>$logs</pre>
                                  </div>
                                </body>
                                </html>
                            \"\"\".trimIndent()

                            val out = PrintWriter(client.getOutputStream(), true)
                            out.print("HTTP/1.1 200 OK\\r\\n")
                            out.print("Content-Type: text/html; charset=utf-8\\r\\n")
                            out.print("Content-Length: ${html.toByteArray().size}\\r\\n")
                            out.print("Connection: close\\r\\n\\r\\n")
                            out.print(html)
                            out.flush()
                            client.close()
                        } catch (_: Exception) {}
                    }
                }
            } catch (_: Exception) {}
        }
    }

    private fun getBatteryPercentage(): Int {
        val intent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        return if (level >= 0 && scale > 0) (level * 100) / scale else 100
    }

    private fun buildNotification(contentText: String): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "PocketRouter Auto-Hotspot Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("PocketRouter Auto-Hotspot Ready")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_router_logo)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    override fun onDestroy() {
        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
""",

    # ---------------------------------------------------------
    # 7. KOTLIN SOURCE CODE: DIRECT-BOOT BROADCAST RECEIVER
    # ---------------------------------------------------------
    "app/src/main/java/com/pocketrouter/auto/BootReceiver.kt": """package com.pocketrouter.auto

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
            action == Intent.ACTION_BOOT_COMPLETED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            PrefsManager.appendLog(context, "Boot broadcast received: ${action.substringAfterLast('.')}")
            val serviceIntent = Intent(context, RouterWatchdogService::class.java).apply {
                this.action = RouterWatchdogService.ACTION_BOOT_TRIGGER
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }
    }
}
""",

    # ---------------------------------------------------------
    # 8. KOTLIN SOURCE CODE: MAIN ACTIVITY + FIRST-START WIZARD
    # ---------------------------------------------------------
    "app/src/main/java/com/pocketrouter/auto/MainActivity.kt": """package com.pocketrouter.auto

import android.Manifest
import android.app.KeyguardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var rootContainer: LinearLayout
    private lateinit var statusSummaryText: TextView
    private lateinit var step1StatusBadge: TextView
    private lateinit var step2StatusBadge: TextView
    private lateinit var step3StatusBadge: TextView
    private lateinit var logViewText: TextView
    private lateinit var delayInput: EditText
    private lateinit var keywordsInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Start foreground watchdog service so web dashboard & boot listener are active
        val startSvc = Intent(this, RouterWatchdogService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(startSvc)
        } else {
            startService(startSvc)
        }

        buildUserInterface()
    }

    override fun onResume() {
        super.onResume()
        refreshPermissionStates()
    }

    private fun buildUserInterface() {
        val scrollView = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#0F172A"))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        rootContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(28), dp(20), dp(40))
        }

        // App Header with Generated Vector Logo
        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(20))
        }
        val logoImage = ImageView(this).apply {
            setImageResource(R.drawable.ic_router_logo)
            layoutParams = LinearLayout.LayoutParams(dp(64), dp(64)).apply {
                rightMargin = dp(16)
            }
        }
        val titleColumn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        val appTitle = TextView(this).apply {
            text = "PocketRouter OS"
            textSize = 24f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.parseColor("#F8FAFC"))
        }
        val appSubtitle = TextView(this).apply {
            text = "Galaxy M11 Auto-Hotspot on Boot"
            textSize = 14f
            setTextColor(Color.parseColor("#38BDF8"))
        }
        titleColumn.addView(appTitle)
        titleColumn.addView(appSubtitle)
        headerRow.addView(logoImage)
        headerRow.addView(titleColumn)
        rootContainer.addView(headerRow)

        // Overall Readiness Banner
        statusSummaryText = TextView(this).apply {
            textSize = 15f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.WHITE)
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = roundedBox("#1E293B", "#38BDF8")
        }
        rootContainer.addView(statusSummaryText)
        addSpacer(16)

        // =========================================================================
        // FIRST-RUN ONBOARDING & PERMISSION WIZARD SECTION
        // =========================================================================
        val wizardHeader = TextView(this).apply {
            text = "FIRST-START SETUP GUIDE (3 STEPS)"
            textSize = 13f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.parseColor("#94A3B8"))
            setPadding(0, dp(4), 0, dp(8))
        }
        rootContainer.addView(wizardHeader)

        // STEP 1 CARD: Accessibility Service
        step1StatusBadge = createBadge()
        val step1Card = createStepCard(
            stepTitle = "Step 1: Enable Auto-Hotspot Accessibility",
            description = "Required so PocketRouter can automatically pull down Quick Settings and tap the Mobile Hotspot tile when your Galaxy M11 turns on.",
            badgeView = step1StatusBadge,
            buttonText = "Open Accessibility Settings"
        ) {
            Toast.makeText(
                this,
                "Tap 'Installed Apps' -> 'PocketRouter Auto-Hotspot Engine' -> Turn ON",
                Toast.LENGTH_LONG
            ).show()
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        rootContainer.addView(step1Card)
        addSpacer(12)

        // STEP 2 CARD: Unrestricted Battery Optimization
        step2StatusBadge = createBadge()
        val step2Card = createStepCard(
            stepTitle = "Step 2: Allow Unrestricted Battery",
            description = "Prevents Samsung One UI Core from putting the boot watchdog to sleep in the background.",
            badgeView = step2StatusBadge,
            buttonText = "Grant Battery Exemption"
        ) {
            requestBatteryExemption()
        }
        rootContainer.addView(step2Card)
        addSpacer(12)

        // STEP 3 CARD: Lock Screen & Quick Settings Tile Check
        step3StatusBadge = createBadge()
        val step3Card = createStepCard(
            stepTitle = "Step 3: Remove Lock Screen PIN & Check Tile",
            description = "1. Set Screen Lock to 'None' or 'Swipe' so the phone boots without waiting for a PIN.\\n2. Make sure 'Mobile Hotspot' is in the top 6 icons of your Quick Settings panel.",
            badgeView = step3StatusBadge,
            buttonText = "Open Lock Screen Settings"
        ) {
            try {
                startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
            } catch (_: Exception) {
                startActivity(Intent(Settings.ACTION_SETTINGS))
            }
        }
        rootContainer.addView(step3Card)
        addSpacer(20)

        // =========================================================================
        // ROUTER CONFIGURATION & LIVE TEST SECTION
        // =========================================================================
        val configCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = roundedBox("#1E293B", "#334155")
        }

        val autoStartSwitch = Switch(this).apply {
            text = "Automatically start Hotspot when phone turns on"
            setTextColor(Color.WHITE)
            textSize = 15f
            isChecked = PrefsManager.isAutoStartEnabled(this@MainActivity)
            setOnCheckedChangeListener { _, isChecked ->
                PrefsManager.setAutoStartEnabled(this@MainActivity, isChecked)
                PrefsManager.appendLog(
                    this@MainActivity,
                    "Auto-start on boot set to: $isChecked"
                )
                refreshPermissionStates()
            }
        }
        configCard.addView(autoStartSwitch)
        addSpacerIn(configCard, 12)

        val delayLabel = TextView(this).apply {
            text = "Boot Delay Seconds (wait for 4G SIM connection before toggling):"
            setTextColor(Color.parseColor("#CBD5E1"))
            textSize = 13f
        }
        delayInput = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(PrefsManager.getBootDelaySeconds(this@MainActivity).toString())
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
        }
        configCard.addView(delayLabel)
        configCard.addView(delayInput)
        addSpacerIn(configCard, 8)

        val kwLabel = TextView(this).apply {
            text = "Quick Settings Tile Names (comma-separated):"
            setTextColor(Color.parseColor("#CBD5E1"))
            textSize = 13f
        }
        keywordsInput = EditText(this).apply {
            setText(PrefsManager.getTileKeywords(this@MainActivity))
            setTextColor(Color.WHITE)
        }
        configCard.addView(kwLabel)
        configCard.addView(keywordsInput)
        addSpacerIn(configCard, 12)

        val saveBtn = Button(this).apply {
            text = "Save Settings"
            setBackgroundColor(Color.parseColor("#0284C7"))
            setTextColor(Color.WHITE)
            setOnClickListener {
                val sec = delayInput.text.toString().toIntOrNull() ?: 12
                PrefsManager.setBootDelaySeconds(this@MainActivity, sec)
                PrefsManager.setTileKeywords(this@MainActivity, keywordsInput.text.toString())
                PrefsManager.appendLog(this@MainActivity, "Saved settings (Delay: ${sec}s).")
                refreshPermissionStates()
                Toast.makeText(this@MainActivity, "Settings Saved!", Toast.LENGTH_SHORT).show()
            }
        }
        configCard.addView(saveBtn)
        addSpacerIn(configCard, 10)

        val testNowBtn = Button(this).apply {
            text = "TEST AUTO-HOTSPOT NOW (LIVE DEMO)"
            setBackgroundColor(Color.parseColor("#10B981"))
            setTextColor(Color.parseColor("#0F172A"))
            setTypeface(typeface, Typeface.BOLD)
            setOnClickListener {
                if (!isAccessibilityServiceEnabled()) {
                    Toast.makeText(
                        this@MainActivity,
                        "Please complete Step 1 (Accessibility Permission) first!",
                        Toast.LENGTH_LONG
                    ).show()
                    return@setOnClickListener
                }
                val testIntent = Intent(this@MainActivity, RouterWatchdogService::class.java).apply {
                    action = RouterWatchdogService.ACTION_MANUAL_TEST
                }
                startService(testIntent)
            }
        }
        configCard.addView(testNowBtn)
        rootContainer.addView(configCard)
        addSpacer(16)

        // =========================================================================
        // DIAGNOSTIC LOGS & WEB ADMIN INFO
        // =========================================================================
        val logCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = roundedBox("#090D16", "#334155")
        }
        val logTitle = TextView(this).apply {
            text = "LIVE EVENT LOG (Web Dashboard on Port 8080)"
            textSize = 12f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.parseColor("#38BDF8"))
        }
        logViewText = TextView(this).apply {
            textSize = 12f
            typeface = Typeface.MONOSPACE
            setTextColor(Color.parseColor("#E2E8F0"))
            setPadding(0, dp(8), 0, 0)
        }
        logCard.addView(logTitle)
        logCard.addView(logViewText)
        rootContainer.addView(logCard)

        scrollView.addView(rootContainer)
        setContentView(scrollView)

        requestNotificationPermissionIfNeeded()
    }

    private fun refreshPermissionStates() {
        val a11yOk = isAccessibilityServiceEnabled()
        val batteryOk = isBatteryOptimizationIgnored()
        val keyguardOk = !isDevicePinLocked()

        updateBadge(step1StatusBadge, a11yOk, "ENABLED", "ACTION REQUIRED")
        updateBadge(step2StatusBadge, batteryOk, "UNRESTRICTED", "OPTIMIZED (TAP TO FIX)")
        updateBadge(step3StatusBadge, keyguardOk, "SWIPE / NONE", "PIN LOCK ACTIVE")

        if (a11yOk && batteryOk && keyguardOk) {
            statusSummaryText.text =
                "SYSTEM READY: Your Galaxy M11 will automatically start its Hotspot ${PrefsManager.getBootDelaySeconds(this)}s after turning on."
            statusSummaryText.background = roundedBox("#064E3B", "#10B981")
        } else {
            statusSummaryText.text =
                "SETUP INCOMPLETE: Complete the highlighted steps below so Hotspot can start automatically after reboot."
            statusSummaryText.background = roundedBox("#78350F", "#F59E0B")
        }

        logViewText.text = PrefsManager.getLogs(this)
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val expectedComponent = ComponentName(this, HotspotAccessibilityService::class.java)
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabledServices.split(":")
            .mapNotNull { ComponentName.unflattenFromString(it) }
            .any { it == expectedComponent }
    }

    private fun isBatteryOptimizationIgnored(): Boolean {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pm.isIgnoringBatteryOptimizations(packageName)
        } else true
    }

    private fun isDevicePinLocked(): Boolean {
        val km = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            km.isDeviceSecure
        } else false
    }

    private fun requestBatteryExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            } catch (_: Exception) {
                startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    200
                )
            }
        }
    }

    private fun createStepCard(
        stepTitle: String,
        description: String,
        badgeView: TextView,
        buttonText: String,
        onClick: () -> Unit
    ): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = roundedBox("#1E293B", "#334155")
        }
        val titleView = TextView(this).apply {
            text = stepTitle
            textSize = 16f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.WHITE)
        }
        val descView = TextView(this).apply {
            text = description
            textSize = 13f
            setTextColor(Color.parseColor("#CBD5E1"))
            setPadding(0, dp(6), 0, dp(10))
        }
        val btn = Button(this).apply {
            text = buttonText
            setBackgroundColor(Color.parseColor("#334155"))
            setTextColor(Color.parseColor("#38BDF8"))
            setOnClickListener { onClick() }
        }
        card.addView(badgeView)
        card.addView(titleView)
        card.addView(descView)
        card.addView(btn)
        return card
    }

    private fun createBadge(): TextView {
        return TextView(this).apply {
            textSize = 11f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(dp(8), dp(4), dp(8), dp(4))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(6) }
        }
    }

    private fun updateBadge(badge: TextView, ok: Boolean, okText: String, warnText: String) {
        if (ok) {
            badge.text = "✓ $okText"
            badge.setTextColor(Color.parseColor("#A7F3D0"))
            badge.background = roundedBox("#065F46", "#10B981")
        } else {
            badge.text = "⚠ $warnText"
            badge.setTextColor(Color.parseColor("#FDE68A"))
            badge.background = roundedBox("#78350F", "#F59E0B")
        }
    }

    private fun roundedBox(fillHex: String, strokeHex: String): GradientDrawable {
        return GradientDrawable().apply {
            setColor(Color.parseColor(fillHex))
            cornerRadius = dp(12).toFloat()
            setStroke(dp(1), Color.parseColor(strokeHex))
        }
    }

    private fun addSpacer(heightDp: Int) {
        rootContainer.addView(
            View(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(heightDp)
                )
            }
        )
    }

    private fun addSpacerIn(parent: LinearLayout, heightDp: Int) {
        parent.addView(
            View(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(heightDp)
                )
            }
        )
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
"""
}

def generate_project():
    for rel_path, content in FILES.items():
        full_path = os.path.join(PROJECT_ROOT, rel_path)
        os.makedirs(os.path.dirname(full_path), exist_ok=True)
        with open(full_path, "w", encoding="utf-8") as f:
            f.write(content.lstrip())
        print(f"Created: {full_path}")

    print("\nProject files generated successfully in ./PocketRouter!")
    print("Next: Open ./PocketRouter in Android Studio or run `gradle assembleDebug` to generate the APK.")

if __name__ == "__main__":
    generate_project()
