package com.pocketrouter.auto

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
                            val html = """
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
                            """.trimIndent()

                            val out = PrintWriter(client.getOutputStream(), true)
                            out.print("HTTP/1.1 200 OK\r\n")
                            out.print("Content-Type: text/html; charset=utf-8\r\n")
                            out.print("Content-Length: " + html.toByteArray().size + "\r\n")
                            out.print("Connection: close\r\n\r\n")
                            out.print(html)
                            out.flush()
                            client.close()
                        } catch (e: Exception) {}
                    }
                }
            } catch (e: Exception) {}
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
        } catch (e: Exception) {}
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
