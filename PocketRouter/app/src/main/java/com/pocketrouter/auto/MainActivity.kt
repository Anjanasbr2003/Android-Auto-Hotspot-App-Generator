package com.pocketrouter.auto

import android.Manifest
import android.animation.ValueAnimator
import android.app.AlertDialog
import android.app.KeyguardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.card.MaterialCardView
import com.google.android.material.switchmaterial.SwitchMaterial
import java.net.Inet4Address
import java.net.NetworkInterface

class MainActivity : AppCompatActivity() {

    private val mainHandler = Handler(Looper.getMainLooper())

    private lateinit var mainScrollView: ScrollView
    private lateinit var heroCard: MaterialCardView
    private lateinit var heroIconBadge: ImageView
    private lateinit var heroStatusText: TextView
    private lateinit var heroSubtext: TextView
    private lateinit var heroActionBtn: TextView

    // 2x2 Grid Cards (MaterialCardViews for interactive touch & selection)
    private val gridCards = mutableListOf<MaterialCardView>()
    private val cardStatusDots = mutableListOf<TextView>()
    private val cardStatusLabels = mutableListOf<TextView>()
    private var selectedGridCardIndex = 0

    // Selected Card Detail Panel
    private lateinit var selectedDetailPanel: LinearLayout
    private lateinit var selectedDetailTitle: TextView
    private lateinit var selectedDetailDesc: TextView
    private lateinit var selectedDetailActionBtn: Button

    // Bottom Navigation Bar Views
    private val navItemViews = mutableListOf<LinearLayout>()
    private val navIconViews = mutableListOf<ImageView>()
    private val navTextViews = mutableListOf<TextView>()
    private val navIndicatorViews = mutableListOf<View>()
    private var currentNavTab = 0

    // Config & Status views
    private lateinit var autoStartSwitch: SwitchMaterial
    private lateinit var delayInput: EditText
    private lateinit var keywordsInput: EditText
    private lateinit var logViewText: TextView
    private lateinit var telemetryIpText: TextView
    private lateinit var headerGreeting: TextView

    // Layout Sections for Navigation Targets
    private lateinit var servicesSectionLayout: LinearLayout
    private lateinit var telemetrySectionLayout: LinearLayout
    private lateinit var configSectionLayout: LinearLayout
    private lateinit var logSectionLayout: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Start background watchdog service
        val startSvc = Intent(this, RouterWatchdogService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(startSvc)
        } else {
            startService(startSvc)
        }

        buildUserInterface()
        requestNotificationPermissionIfNeeded()
    }

    override fun onResume() {
        super.onResume()
        refreshAllStates()
    }

    private fun buildUserInterface() {
        // ROOT CONTAINER: Clean vertical LinearLayout with zero overlap
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#F8F9FD"))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        // =============================================================
        // 1. SCROLLABLE MAIN CONTENT (takes weight = 1.0f above nav bar)
        // =============================================================
        mainScrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1.0f
            )
            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
            isVerticalScrollBarEnabled = false
            isNestedScrollingEnabled = true
        }

        val scrollContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setPadding(0, 0, 0, dp(28))
        }

        // =============================================================
        // 2. PURPLE HEADER BANNER (Reference UI Match)
        // =============================================================
        val headerContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = ContextCompat.getDrawable(this@MainActivity, R.drawable.bg_header_purple)
            setPadding(dp(20), dp(38), dp(20), dp(28))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // Top Greeting Row + Bell Button
        val topBarRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val greetingCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f)
        }

        headerGreeting = TextView(this).apply {
            text = "Good day! 👋"
            textSize = 25f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.WHITE)
        }

        val headerSubtitle = TextView(this).apply {
            text = "PocketRouter • Galaxy M11 Auto-Hotspot"
            textSize = 13.5f
            setTextColor(Color.parseColor("#E9D5FF"))
            setPadding(0, dp(2), 0, 0)
        }
        greetingCol.addView(headerGreeting)
        greetingCol.addView(headerSubtitle)

        // Notification Bell Icon Button
        val bellContainer = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            val size = dp(42)
            layoutParams = LinearLayout.LayoutParams(size, size)
            background = roundedBox("#3B0764", "#7E22CE", radiusDp = 21)
            isClickable = true
            isFocusable = true
            setOnClickListener { showReadinessDialog() }
        }
        val bellIcon = ImageView(this).apply {
            setImageResource(R.drawable.ic_bell)
            layoutParams = LinearLayout.LayoutParams(dp(22), dp(22))
        }
        bellContainer.addView(bellIcon)

        topBarRow.addView(greetingCol)
        topBarRow.addView(bellContainer)
        headerContainer.addView(topBarRow)
        addSpacerIn(headerContainer, 18)

        // -------------------------------------------------------------
        // HERO CARD: LIVE HOTSPOT STATUS WITH MATERIAL RIPPLE & SELECTION
        // -------------------------------------------------------------
        heroCard = MaterialCardView(this).apply {
            radius = dp(20).toFloat()
            cardElevation = dp(4).toFloat()
            maxCardElevation = dp(6).toFloat()
            setCardBackgroundColor(Color.WHITE)
            strokeColor = Color.parseColor("#E9D5FF")
            strokeWidth = dp(1.5f)
            rippleColor = ColorStateList.valueOf(Color.parseColor("#DDD6FE"))
            isClickable = true
            isFocusable = true
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setOnClickListener {
                animateCardTouch(this)
                executeLiveDemo()
            }
        }

        val heroContent = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }

        // Squircle Icon Badge on left
        val heroIconBox = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            val boxSize = dp(48)
            layoutParams = LinearLayout.LayoutParams(boxSize, boxSize).apply {
                rightMargin = dp(14)
            }
            background = roundedBox("#F3E8FF", "#DDD6FE", radiusDp = 14)
        }
        heroIconBadge = ImageView(this).apply {
            setImageResource(R.drawable.ic_wifi)
            layoutParams = LinearLayout.LayoutParams(dp(26), dp(26))
            setColorFilter(Color.parseColor("#7C3AED"))
        }
        heroIconBox.addView(heroIconBadge)

        val heroTextCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f)
        }
        heroStatusText = TextView(this).apply {
            text = "Hotspot: STANDBY"
            textSize = 16f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.parseColor("#1E1B4B"))
        }
        heroSubtext = TextView(this).apply {
            text = "Tap to test auto-hotspot sequence"
            textSize = 12f
            setTextColor(Color.parseColor("#6B7280"))
            setPadding(0, dp(2), 0, 0)
        }
        heroTextCol.addView(heroStatusText)
        heroTextCol.addView(heroSubtext)

        heroActionBtn = TextView(this).apply {
            text = "TEST NOW >"
            textSize = 12f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.WHITE)
            setPadding(dp(14), dp(8), dp(14), dp(8))
            background = roundedBox("#5E17EB", "#4A0ECC", radiusDp = 14)
        }

        heroContent.addView(heroIconBox)
        heroContent.addView(heroTextCol)
        heroContent.addView(heroActionBtn)
        heroCard.addView(heroContent)
        headerContainer.addView(heroCard)
        scrollContent.addView(headerContainer)

        // =============================================================
        // 3. MAIN BODY SECTION (Cards, 2x2 Grid with Selection Feedback)
        // =============================================================
        val bodyContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(20), dp(18), dp(16))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // Section Title
        servicesSectionLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val servicesHeaderRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(12))
        }
        val servicesTitle = TextView(this).apply {
            text = "System services"
            textSize = 18f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.parseColor("#1E1B4B"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f)
        }
        val servicesHint = TextView(this).apply {
            text = "Tap any tile to inspect"
            textSize = 11.5f
            setTextColor(Color.parseColor("#7C3AED"))
            setTypeface(typeface, Typeface.BOLD)
        }
        servicesHeaderRow.addView(servicesTitle)
        servicesHeaderRow.addView(servicesHint)
        servicesSectionLayout.addView(servicesHeaderRow)

        // -------------------------------------------------------------
        // 2x2 GRID (Energy, Broadband, Mobile, Insurance Style)
        // -------------------------------------------------------------
        val gridRow1 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // Card 0: Accessibility Engine
        val dot0 = createDotView()
        val lbl0 = createStatusLabelView()
        cardStatusDots.add(dot0)
        cardStatusLabels.add(lbl0)
        val card0 = buildInteractiveGridCard(
            index = 0,
            title = "Accessibility",
            iconRes = R.drawable.ic_flash,
            iconBadgeBg = "#E6F9F0",
            statusDot = dot0,
            statusLabel = lbl0,
            actionText = "Configure",
            weight = 1.0f,
            rightMarginDp = 6
        )

        // Card 1: Hotspot Quick Tile
        val dot1 = createDotView()
        val lbl1 = createStatusLabelView()
        cardStatusDots.add(dot1)
        cardStatusLabels.add(lbl1)
        val card1 = buildInteractiveGridCard(
            index = 1,
            title = "Hotspot Tile",
            iconRes = R.drawable.ic_wifi,
            iconBadgeBg = "#F3E8FF",
            statusDot = dot1,
            statusLabel = lbl1,
            actionText = "Top 6 Check",
            weight = 1.0f,
            leftMarginDp = 6
        )

        gridRow1.addView(card0)
        gridRow1.addView(card1)
        servicesSectionLayout.addView(gridRow1)
        addSpacerIn(servicesSectionLayout, 12)

        val gridRow2 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // Card 2: Battery Shield
        val dot2 = createDotView()
        val lbl2 = createStatusLabelView()
        cardStatusDots.add(dot2)
        cardStatusLabels.add(lbl2)
        val card2 = buildInteractiveGridCard(
            index = 2,
            title = "Battery Shield",
            iconRes = R.drawable.ic_battery,
            iconBadgeBg = "#FEF3C7",
            statusDot = dot2,
            statusLabel = lbl2,
            actionText = "Exempt",
            weight = 1.0f,
            rightMarginDp = 6
        )

        // Card 3: Screen Security
        val dot3 = createDotView()
        val lbl3 = createStatusLabelView()
        cardStatusDots.add(dot3)
        cardStatusLabels.add(lbl3)
        val card3 = buildInteractiveGridCard(
            index = 3,
            title = "Lock Screen",
            iconRes = R.drawable.ic_lock,
            iconBadgeBg = "#FEE2E2",
            statusDot = dot3,
            statusLabel = lbl3,
            actionText = "Verify",
            weight = 1.0f,
            leftMarginDp = 6
        )

        gridRow2.addView(card2)
        gridRow2.addView(card3)
        servicesSectionLayout.addView(gridRow2)
        addSpacerIn(servicesSectionLayout, 14)

        // -------------------------------------------------------------
        // DYNAMIC SELECTION DETAIL PANEL (Reveals status when tile is selected)
        // -------------------------------------------------------------
        selectedDetailPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = roundedBox("#FAF5FF", "#DDD6FE", radiusDp = 16)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val detailHeaderRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val detailSelectedBadge = TextView(this).apply {
            text = "SELECTED SERVICE"
            textSize = 10f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.parseColor("#5E17EB"))
            setPadding(dp(6), dp(2), dp(6), dp(2))
            background = roundedBox("#E9D5FF", "#C4B5FD", radiusDp = 6)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                rightMargin = dp(8)
            }
        }
        selectedDetailTitle = TextView(this).apply {
            text = "Accessibility Engine"
            textSize = 14f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.parseColor("#1E1B4B"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f)
        }
        detailHeaderRow.addView(detailSelectedBadge)
        detailHeaderRow.addView(selectedDetailTitle)
        selectedDetailPanel.addView(detailHeaderRow)

        selectedDetailDesc = TextView(this).apply {
            text = "Automatically toggles the Mobile Hotspot tile on power-on."
            textSize = 12f
            setTextColor(Color.parseColor("#4B5563"))
            setPadding(0, dp(6), 0, dp(10))
        }
        selectedDetailPanel.addView(selectedDetailDesc)

        selectedDetailActionBtn = Button(this).apply {
            text = "Open Accessibility Settings"
            textSize = 12.5f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.WHITE)
            background = roundedBox("#5E17EB", "#4A0ECC", radiusDp = 10)
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }
        selectedDetailPanel.addView(selectedDetailActionBtn)
        servicesSectionLayout.addView(selectedDetailPanel)

        bodyContent.addView(servicesSectionLayout)
        addSpacerIn(bodyContent, 22)

        // =============================================================
        // 4. ACTION & TELEMETRY CARDS (Soft Lavender Promo Cards)
        // =============================================================
        telemetrySectionLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val telemetryTitle = TextView(this).apply {
            text = "Web Dashboard & Telemetry"
            textSize = 17f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.parseColor("#1E1B4B"))
            setPadding(0, 0, 0, dp(12))
        }
        telemetrySectionLayout.addView(telemetryTitle)

        // Lavender Promo Card with Material Ripple
        val telemetryCard = MaterialCardView(this).apply {
            radius = dp(18).toFloat()
            cardElevation = dp(2).toFloat()
            setCardBackgroundColor(Color.parseColor("#F5EEFD"))
            strokeColor = Color.parseColor("#DDD6FE")
            strokeWidth = dp(1f)
            rippleColor = ColorStateList.valueOf(Color.parseColor("#E9D5FF"))
            isClickable = true
            isFocusable = true
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setOnClickListener {
                animateCardTouch(this)
                openWebDashboard()
            }
        }

        val telInner = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }

        val telHeaderRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val globeBox = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            val size = dp(40)
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                rightMargin = dp(12)
            }
            background = roundedBox("#FFFFFF", "#E9D5FF", radiusDp = 12)
        }
        val globeIcon = ImageView(this).apply {
            setImageResource(R.drawable.ic_globe)
            layoutParams = LinearLayout.LayoutParams(dp(22), dp(22))
        }
        globeBox.addView(globeIcon)

        val telTitleCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f)
        }
        val telHeading = TextView(this).apply {
            text = "Local Web Telemetry Active"
            textSize = 15f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.parseColor("#4C1D95"))
        }
        telemetryIpText = TextView(this).apply {
            text = "http://192.168.43.1:8080"
            textSize = 13f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.parseColor("#6D28D9"))
            setPadding(0, dp(2), 0, 0)
        }
        telTitleCol.addView(telHeading)
        telTitleCol.addView(telemetryIpText)

        telHeaderRow.addView(globeBox)
        telHeaderRow.addView(telTitleCol)
        telInner.addView(telHeaderRow)

        val telDesc = TextView(this).apply {
            text = "Control this router, monitor battery, and view real-time events from any connected laptop or phone browser on port 8080."
            textSize = 12.5f
            setTextColor(Color.parseColor("#5B21B6"))
            setPadding(0, dp(10), 0, dp(12))
        }
        telInner.addView(telDesc)

        val openBrowserBtn = TextView(this).apply {
            text = "Open Web Control Panel >"
            textSize = 13f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.parseColor("#4338CA"))
            setPadding(0, dp(4), 0, dp(4))
        }
        telInner.addView(openBrowserBtn)
        telemetryCard.addView(telInner)
        telemetrySectionLayout.addView(telemetryCard)
        bodyContent.addView(telemetrySectionLayout)
        addSpacerIn(bodyContent, 22)

        // =============================================================
        // 5. TIMING & ROUTER SETTINGS CARD
        // =============================================================
        configSectionLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val configTitle = TextView(this).apply {
            text = "Router Automation Settings"
            textSize = 17f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.parseColor("#1E1B4B"))
            setPadding(0, 0, 0, dp(12))
        }
        configSectionLayout.addView(configTitle)

        val configCard = MaterialCardView(this).apply {
            radius = dp(18).toFloat()
            cardElevation = dp(2).toFloat()
            setCardBackgroundColor(Color.WHITE)
            strokeColor = Color.parseColor("#EEF0F4")
            strokeWidth = dp(1f)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val configInner = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(18))
        }

        // Auto-start Switch
        val switchRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        val switchLabelCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f)
        }
        val switchTitle = TextView(this).apply {
            text = "Auto-Start Hotspot on Boot"
            textSize = 15f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.parseColor("#1E1B4B"))
        }
        val switchSub = TextView(this).apply {
            text = "Toggles hotspot when phone powers on"
            textSize = 12f
            setTextColor(Color.parseColor("#6B7280"))
        }
        switchLabelCol.addView(switchTitle)
        switchLabelCol.addView(switchSub)

        autoStartSwitch = SwitchMaterial(this).apply {
            isChecked = PrefsManager.isAutoStartEnabled(this@MainActivity)
            thumbTintList = ColorStateList.valueOf(Color.parseColor("#5E17EB"))
            trackTintList = ColorStateList.valueOf(Color.parseColor("#DDD6FE"))
            setOnCheckedChangeListener { _, isChecked ->
                PrefsManager.setAutoStartEnabled(this@MainActivity, isChecked)
                PrefsManager.appendLog(this@MainActivity, "Auto-start on boot: $isChecked")
                refreshAllStates()
            }
        }
        switchRow.addView(switchLabelCol)
        switchRow.addView(autoStartSwitch)
        configInner.addView(switchRow)
        addDividerIn(configInner)

        // Boot Delay Config
        val delayLabel = TextView(this).apply {
            text = "Boot Delay (Seconds to wait for 4G SIM):"
            textSize = 13f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.parseColor("#374151"))
            setPadding(0, dp(4), 0, dp(4))
        }
        delayInput = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(PrefsManager.getBootDelaySeconds(this@MainActivity).toString())
            textSize = 14f
            setTextColor(Color.parseColor("#111827"))
            background = roundedBox("#F3F4F6", "#E5E7EB", radiusDp = 10)
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }
        configInner.addView(delayLabel)
        configInner.addView(delayInput)
        addSpacerIn(configInner, 10)

        // Tile Keywords Config
        val kwLabel = TextView(this).apply {
            text = "Quick Settings Tile Keywords:"
            textSize = 13f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.parseColor("#374151"))
            setPadding(0, dp(4), 0, dp(4))
        }
        keywordsInput = EditText(this).apply {
            setText(PrefsManager.getTileKeywords(this@MainActivity))
            textSize = 14f
            setTextColor(Color.parseColor("#111827"))
            background = roundedBox("#F3F4F6", "#E5E7EB", radiusDp = 10)
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }
        configInner.addView(kwLabel)
        configInner.addView(keywordsInput)
        addSpacerIn(configInner, 14)

        // Save Settings Button
        val saveBtn = Button(this).apply {
            text = "Save Settings"
            setBackgroundColor(Color.parseColor("#5E17EB"))
            setTextColor(Color.WHITE)
            setTypeface(typeface, Typeface.BOLD)
            background = roundedBox("#5E17EB", "#4A0ECC", radiusDp = 12)
            setOnClickListener {
                val sec = delayInput.text.toString().toIntOrNull() ?: 12
                PrefsManager.setBootDelaySeconds(this@MainActivity, sec)
                PrefsManager.setTileKeywords(this@MainActivity, keywordsInput.text.toString())
                PrefsManager.appendLog(this@MainActivity, "Saved settings (Delay: ${sec}s).")
                refreshAllStates()
                Toast.makeText(this@MainActivity, "Settings Saved!", Toast.LENGTH_SHORT).show()
            }
        }
        configInner.addView(saveBtn)
        configCard.addView(configInner)
        configSectionLayout.addView(configCard)
        bodyContent.addView(configSectionLayout)
        addSpacerIn(bodyContent, 22)

        // =============================================================
        // 6. LIVE ACTIVITY LOG TERMINAL CARD
        // =============================================================
        logSectionLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val logSectionTitle = TextView(this).apply {
            text = "Real-Time Telemetry Log"
            textSize = 17f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.parseColor("#1E1B4B"))
            setPadding(0, 0, 0, dp(12))
        }
        logSectionLayout.addView(logSectionTitle)

        val logTerminalCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(16))
            background = roundedBox("#0F172A", "#1E293B", radiusDp = 16)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val logHeader = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val terminalDot = TextView(this).apply {
            text = "●"
            setTextColor(Color.parseColor("#10B981"))
            textSize = 12f
            setPadding(0, 0, dp(8), 0)
        }
        val terminalTitle = TextView(this).apply {
            text = "pocketrouter.local:8080"
            textSize = 12f
            setTypeface(Typeface.MONOSPACE, Typeface.BOLD)
            setTextColor(Color.parseColor("#38BDF8"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f)
        }
        val refreshLogsBtn = TextView(this).apply {
            text = "Refresh"
            textSize = 11.5f
            setTextColor(Color.parseColor("#94A3B8"))
            isClickable = true
            setOnClickListener {
                refreshAllStates()
                Toast.makeText(this@MainActivity, "Logs updated", Toast.LENGTH_SHORT).show()
            }
        }
        logHeader.addView(terminalDot)
        logHeader.addView(terminalTitle)
        logHeader.addView(refreshLogsBtn)
        logTerminalCard.addView(logHeader)

        logViewText = TextView(this).apply {
            textSize = 11.5f
            typeface = Typeface.MONOSPACE
            setTextColor(Color.parseColor("#E2E8F0"))
            setPadding(0, dp(10), 0, 0)
        }
        logTerminalCard.addView(logViewText)
        logSectionLayout.addView(logTerminalCard)
        bodyContent.addView(logSectionLayout)

        scrollContent.addView(bodyContent)
        mainScrollView.addView(scrollContent)
        rootLayout.addView(mainScrollView)

        // =============================================================
        // 7. BOTTOM NAVIGATION BAR (Perfect Alignment with WindowInsets)
        // =============================================================
        val bottomNav = createBottomNavigationBar()
        rootLayout.addView(bottomNav)

        setContentView(rootLayout)

        // Select initial card 0 (Accessibility)
        selectGridCard(0, animate = false)
    }

    // -----------------------------------------------------------------
    // BOTTOM NAVIGATION BAR (Aligned cleanly with Window Insets)
    // -----------------------------------------------------------------
    private fun createBottomNavigationBar(): LinearLayout {
        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.WHITE)
            elevation = dp(14).toFloat()
            setPadding(dp(8), dp(6), dp(8), dp(6))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // Apply Window Insets so navigation bar never overlaps Android system gesture/button bar
        ViewCompat.setOnApplyWindowInsetsListener(nav) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                dp(8),
                dp(6),
                dp(8),
                dp(6) + systemBars.bottom
            )
            insets
        }

        navItemViews.clear()
        navIconViews.clear()
        navTextViews.clear()
        navIndicatorViews.clear()

        // Tab 0: Home
        nav.addView(createNavItem(0, R.drawable.ic_nav_home, "Home") {
            selectNavTab(0)
            mainScrollView.smoothScrollTo(0, 0)
        })

        // Tab 1: Services
        nav.addView(createNavItem(1, R.drawable.ic_nav_services, "Services") {
            selectNavTab(1)
            mainScrollView.smoothScrollTo(0, servicesSectionLayout.top)
        })

        // Tab 2: Dashboard
        nav.addView(createNavItem(2, R.drawable.ic_globe, "Dashboard") {
            selectNavTab(2)
            mainScrollView.smoothScrollTo(0, telemetrySectionLayout.top)
        })

        // Tab 3: Guide / Help
        nav.addView(createNavItem(3, R.drawable.ic_nav_help, "Guide") {
            selectNavTab(3)
            showReadinessDialog()
        })

        return nav
    }

    private fun createNavItem(
        index: Int,
        iconRes: Int,
        label: String,
        onClick: () -> Unit
    ): LinearLayout {
        val item = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f)
            setPadding(0, dp(4), 0, dp(4))
            isClickable = true
            isFocusable = true
            val outValue = TypedValue()
            context.theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true)
            setBackgroundResource(outValue.resourceId)
            setOnClickListener { onClick() }
        }

        val isSelected = (index == 0)

        val icon = ImageView(this).apply {
            setImageResource(iconRes)
            layoutParams = LinearLayout.LayoutParams(dp(24), dp(24)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            }
            setColorFilter(if (isSelected) Color.parseColor("#5E17EB") else Color.parseColor("#94A3B8"))
        }

        val text = TextView(this).apply {
            this.text = label
            textSize = 10.5f
            gravity = Gravity.CENTER
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = dp(2)
                bottomMargin = dp(1)
            }
            setTypeface(typeface, if (isSelected) Typeface.BOLD else Typeface.NORMAL)
            setTextColor(if (isSelected) Color.parseColor("#5E17EB") else Color.parseColor("#94A3B8"))
            maxLines = 1
        }

        val indicator = View(this).apply {
            background = ContextCompat.getDrawable(this@MainActivity, R.drawable.bg_nav_indicator)
            layoutParams = LinearLayout.LayoutParams(dp(16), dp(3)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = dp(1)
            }
            visibility = if (isSelected) View.VISIBLE else View.INVISIBLE
        }

        item.addView(icon)
        item.addView(text)
        item.addView(indicator)

        navItemViews.add(item)
        navIconViews.add(icon)
        navTextViews.add(text)
        navIndicatorViews.add(indicator)

        return item
    }

    private fun selectNavTab(tabIndex: Int) {
        currentNavTab = tabIndex
        for (i in navItemViews.indices) {
            val isSelected = (i == tabIndex)
            val color = if (isSelected) Color.parseColor("#5E17EB") else Color.parseColor("#94A3B8")
            navIconViews[i].setColorFilter(color)
            navTextViews[i].setTextColor(color)
            navTextViews[i].setTypeface(null, if (isSelected) Typeface.BOLD else Typeface.NORMAL)
            navIndicatorViews[i].visibility = if (isSelected) View.VISIBLE else View.INVISIBLE
        }
    }

    // -----------------------------------------------------------------
    // 2x2 GRID CARD BUILDER (MaterialCardView with Selection State)
    // -----------------------------------------------------------------
    private fun buildInteractiveGridCard(
        index: Int,
        title: String,
        iconRes: Int,
        iconBadgeBg: String,
        statusDot: TextView,
        statusLabel: TextView,
        actionText: String,
        weight: Float,
        leftMarginDp: Int = 0,
        rightMarginDp: Int = 0
    ): MaterialCardView {
        val card = MaterialCardView(this).apply {
            radius = dp(18).toFloat()
            cardElevation = dp(2).toFloat()
            maxCardElevation = dp(5).toFloat()
            setCardBackgroundColor(Color.WHITE)
            strokeColor = Color.parseColor("#EEF0F4")
            strokeWidth = dp(1f)
            rippleColor = ColorStateList.valueOf(Color.parseColor("#DDD6FE"))
            isClickable = true
            isFocusable = true
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, weight).apply {
                leftMargin = dp(leftMarginDp)
                rightMargin = dp(rightMarginDp)
            }
            setOnClickListener {
                selectGridCard(index, animate = true)
            }
        }

        val cardInner = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(14), dp(14), dp(12))
        }

        // Top Row: Title + Circular Icon Badge
        val topRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val cardTitle = TextView(this).apply {
            text = title
            textSize = 14.5f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.parseColor("#1E1B4B"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f)
        }

        val iconBadge = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            val badgeSize = dp(32)
            layoutParams = LinearLayout.LayoutParams(badgeSize, badgeSize)
            background = roundedBox(iconBadgeBg, iconBadgeBg, radiusDp = 16)
        }
        val iconView = ImageView(this).apply {
            setImageResource(iconRes)
            layoutParams = LinearLayout.LayoutParams(dp(18), dp(18))
        }
        iconBadge.addView(iconView)

        topRow.addView(cardTitle)
        topRow.addView(iconBadge)
        cardInner.addView(topRow)
        addSpacerIn(cardInner, 10)

        // Middle Row: Status description with dot
        val statusRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        statusRow.addView(statusDot)
        statusRow.addView(statusLabel)
        cardInner.addView(statusRow)
        addSpacerIn(cardInner, 12)

        // Divider
        val divider = View(this).apply {
            setBackgroundColor(Color.parseColor("#F3F4F6"))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(1)
            )
        }
        cardInner.addView(divider)
        addSpacerIn(cardInner, 10)

        // Bottom Row: Action link with chevron
        val bottomRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val actionTextView = TextView(this).apply {
            text = actionText
            textSize = 12.5f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.parseColor("#5E17EB"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f)
        }
        val chevron = ImageView(this).apply {
            setImageResource(R.drawable.ic_chevron_right)
            layoutParams = LinearLayout.LayoutParams(dp(16), dp(16))
        }
        bottomRow.addView(actionTextView)
        bottomRow.addView(chevron)
        cardInner.addView(bottomRow)

        card.addView(cardInner)
        gridCards.add(card)

        return card
    }

    // -----------------------------------------------------------------
    // TILE SELECTION MANAGER (Visual Feedback + Detail Inspection)
    // -----------------------------------------------------------------
    private fun selectGridCard(index: Int, animate: Boolean) {
        selectedGridCardIndex = index

        for (i in gridCards.indices) {
            val card = gridCards[i]
            if (i == index) {
                // SELECTED TILE: Vibrant Purple border + subtle lavender glow
                card.strokeColor = Color.parseColor("#5E17EB")
                card.strokeWidth = dp(2f)
                card.setCardBackgroundColor(Color.parseColor("#FAF5FF"))
                card.cardElevation = dp(4).toFloat()
                if (animate) animateCardTouch(card)
            } else {
                // UNSELECTED TILE: Soft neutral border
                card.strokeColor = Color.parseColor("#EEF0F4")
                card.strokeWidth = dp(1f)
                card.setCardBackgroundColor(Color.WHITE)
                card.cardElevation = dp(2).toFloat()
            }
        }

        // Update Detail Panel below the 2x2 grid
        when (index) {
            0 -> {
                selectedDetailTitle.text = "Accessibility Engine"
                val a11yOk = isAccessibilityServiceEnabled()
                selectedDetailDesc.text = if (a11yOk)
                    "✓ Active & Running: HotspotAccessibilityService is ready to swipe and toggle the Mobile Hotspot tile automatically when your phone powers on."
                else
                    "⚠ Inactive: PocketRouter requires accessibility permission to swipe down Quick Settings and tap your Hotspot tile after boot."
                selectedDetailActionBtn.text = if (a11yOk) "Reconfigure Accessibility >" else "Enable Accessibility Service >"
                selectedDetailActionBtn.setOnClickListener {
                    Toast.makeText(this, "Select 'PocketRouter Auto-Hotspot Engine' -> Turn ON", Toast.LENGTH_LONG).show()
                    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
            }
            1 -> {
                selectedDetailTitle.text = "Mobile Hotspot Quick Tile"
                val isHotspotOn = HotspotAccessibilityService.isHotspotActive(this)
                selectedDetailDesc.text = if (isHotspotOn)
                    "✓ Hotspot is currently ACTIVE: Broadcasting cellular network."
                else
                    "ℹ Standby: Ensure the 'Mobile Hotspot' icon is placed among the TOP 6 tiles in your notification shade for instant one-swipe toggling."
                selectedDetailActionBtn.text = "Test Hotspot Toggle Now >"
                selectedDetailActionBtn.setOnClickListener {
                    executeLiveDemo()
                }
            }
            2 -> {
                selectedDetailTitle.text = "Battery Optimization Shield"
                val batteryOk = isBatteryOptimizationIgnored()
                selectedDetailDesc.text = if (batteryOk)
                    "✓ Unrestricted: Android & Samsung One UI Core will not put the boot watchdog service to sleep."
                else
                    "⚠ Optimized: Samsung may sleep the background watchdog. Grant an exemption for 100% reliable boot recovery."
                selectedDetailActionBtn.text = "Grant Battery Exemption >"
                selectedDetailActionBtn.setOnClickListener {
                    requestBatteryExemption()
                }
            }
            3 -> {
                selectedDetailTitle.text = "Screen Lock Protection"
                val keyguardOk = !isDevicePinLocked()
                selectedDetailDesc.text = if (keyguardOk)
                    "✓ Safe (Swipe / None): Device automatically enters launcher on restart after battery drain."
                else
                    "⚠ PIN Lock Detected: Android stops services before PIN entry. Set screen lock to 'Swipe' or 'None' for unattended boot."
                selectedDetailActionBtn.text = "Open Lock Screen Settings >"
                selectedDetailActionBtn.setOnClickListener {
                    try {
                        startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
                    } catch (e: Exception) {
                        startActivity(Intent(Settings.ACTION_SETTINGS))
                    }
                }
            }
        }
    }

    private fun animateCardTouch(view: View) {
        view.scaleX = 0.97f
        view.scaleY = 0.97f
        view.animate()
            .scaleX(1.0f)
            .scaleY(1.0f)
            .setDuration(150)
            .start()
    }

    private fun createDotView(): TextView {
        return TextView(this).apply {
            text = "●"
            textSize = 10f
            setTextColor(Color.parseColor("#10B981"))
            setPadding(0, 0, dp(4), 0)
        }
    }

    private fun createStatusLabelView(): TextView {
        return TextView(this).apply {
            text = "Checking..."
            textSize = 11.5f
            setTextColor(Color.parseColor("#6B7280"))
            maxLines = 1
        }
    }

    // -----------------------------------------------------------------
    // STATE REFRESH LOGIC
    // -----------------------------------------------------------------
    private fun refreshAllStates() {
        val a11yOk = isAccessibilityServiceEnabled()
        val batteryOk = isBatteryOptimizationIgnored()
        val keyguardOk = !isDevicePinLocked()
        val isHotspotOn = HotspotAccessibilityService.isHotspotActive(this)
        val batteryPct = getBatteryPercentage()

        // 1. Update Hero Card
        if (isHotspotOn) {
            heroStatusText.text = "Hotspot: ACTIVE"
            heroStatusText.setTextColor(Color.parseColor("#059669"))
            heroSubtext.text = "Sharing Wi-Fi • Battery: $batteryPct%"
            heroIconBadge.setColorFilter(Color.parseColor("#10B981"))
            heroActionBtn.text = "RE-TRIGGER >"
        } else {
            heroStatusText.text = "Hotspot: STANDBY"
            heroStatusText.setTextColor(Color.parseColor("#1E1B4B"))
            heroSubtext.text = "Auto-boot ready • Battery: $batteryPct%"
            heroIconBadge.setColorFilter(Color.parseColor("#7C3AED"))
            heroActionBtn.text = "TEST NOW >"
        }

        // 2. Card 0: Accessibility
        if (a11yOk) {
            cardStatusDots[0].setTextColor(Color.parseColor("#10B981"))
            cardStatusLabels[0].text = "Engine is live"
            cardStatusLabels[0].setTextColor(Color.parseColor("#059669"))
        } else {
            cardStatusDots[0].setTextColor(Color.parseColor("#F59E0B"))
            cardStatusLabels[0].text = "Action required"
            cardStatusLabels[0].setTextColor(Color.parseColor("#D97706"))
        }

        // 3. Card 1: Hotspot Tile
        if (isHotspotOn) {
            cardStatusDots[1].setTextColor(Color.parseColor("#10B981"))
            cardStatusLabels[1].text = "Broadcasting"
            cardStatusLabels[1].setTextColor(Color.parseColor("#059669"))
        } else {
            cardStatusDots[1].setTextColor(Color.parseColor("#7C3AED"))
            cardStatusLabels[1].text = "Ready to toggle"
            cardStatusLabels[1].setTextColor(Color.parseColor("#6D28D9"))
        }

        // 4. Card 2: Battery Shield
        if (batteryOk) {
            cardStatusDots[2].setTextColor(Color.parseColor("#10B981"))
            cardStatusLabels[2].text = "Unrestricted"
            cardStatusLabels[2].setTextColor(Color.parseColor("#059669"))
        } else {
            cardStatusDots[2].setTextColor(Color.parseColor("#F59E0B"))
            cardStatusLabels[2].text = "Tap to exempt"
            cardStatusLabels[2].setTextColor(Color.parseColor("#D97706"))
        }

        // 5. Card 3: Lock Screen
        if (keyguardOk) {
            cardStatusDots[3].setTextColor(Color.parseColor("#10B981"))
            cardStatusLabels[3].text = "Swipe/None (Safe)"
            cardStatusLabels[3].setTextColor(Color.parseColor("#059669"))
        } else {
            cardStatusDots[3].setTextColor(Color.parseColor("#EF4444"))
            cardStatusLabels[3].text = "PIN lock active"
            cardStatusLabels[3].setTextColor(Color.parseColor("#DC2626"))
        }

        // 6. Update local IP in telemetry card
        val localIp = getDeviceIpAddress()
        telemetryIpText.text = "http://$localIp:8080"

        // 7. Update logs
        logViewText.text = PrefsManager.getLogs(this)

        // Refresh detail panel text with latest state
        selectGridCard(selectedGridCardIndex, animate = false)
    }

    private fun executeLiveDemo() {
        if (!isAccessibilityServiceEnabled()) {
            Toast.makeText(
                this,
                "Please enable Accessibility Service first!",
                Toast.LENGTH_LONG
            ).show()
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            return
        }

        heroActionBtn.text = "RUNNING..."
        val testIntent = Intent(this, RouterWatchdogService::class.java).apply {
            action = RouterWatchdogService.ACTION_MANUAL_TEST
        }
        startService(testIntent)
        Toast.makeText(this, "Starting Auto-Hotspot Sequence...", Toast.LENGTH_SHORT).show()

        mainHandler.postDelayed({
            refreshAllStates()
        }, 3500L)
    }

    private fun openWebDashboard() {
        try {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("http://localhost:8080"))
            startActivity(browserIntent)
        } catch (e: Exception) {
            Toast.makeText(this, "Could not open browser", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showReadinessDialog() {
        val a11yOk = isAccessibilityServiceEnabled()
        val batteryOk = isBatteryOptimizationIgnored()
        val keyguardOk = !isDevicePinLocked()

        val msg = StringBuilder()
        msg.append(if (a11yOk) "✓ Accessibility Engine: ACTIVE\n" else "⚠ Accessibility Engine: INACTIVE\n")
        msg.append(if (batteryOk) "✓ Battery Exemption: UNRESTRICTED\n" else "⚠ Battery Exemption: RESTRICTED\n")
        msg.append(if (keyguardOk) "✓ Screen Lock: SAFE (SWIPE/NONE)\n" else "⚠ Screen Lock: PIN ACTIVE (Remove PIN for boot)\n\n")
        msg.append("Ensure 'Mobile Hotspot' is in the top 6 Quick Settings tiles so PocketRouter can tap it on reboot.")

        AlertDialog.Builder(this)
            .setTitle("PocketRouter Readiness Guide")
            .setMessage(msg.toString())
            .setPositiveButton("Close", null)
            .show()
    }

    // -----------------------------------------------------------------
    // SYSTEM INTROSPECTION HELPERS
    // -----------------------------------------------------------------
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
            } catch (e: Exception) {
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

    private fun getBatteryPercentage(): Int {
        val intent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        return if (level >= 0 && scale > 0) (level * 100) / scale else 100
    }

    private fun getDeviceIpAddress(): String {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val intf = interfaces.nextElement()
                val addrs = intf.inetAddresses
                while (addrs.hasMoreElements()) {
                    val addr = addrs.nextElement()
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        return addr.hostAddress ?: "192.168.43.1"
                    }
                }
            }
            "192.168.43.1"
        } catch (e: Exception) {
            "192.168.43.1"
        }
    }

    // -----------------------------------------------------------------
    // UI STYLING UTILITIES
    // -----------------------------------------------------------------
    private fun roundedBox(fillHex: String, strokeHex: String, radiusDp: Int = 16): GradientDrawable {
        return GradientDrawable().apply {
            setColor(Color.parseColor(fillHex))
            cornerRadius = dp(radiusDp).toFloat()
            setStroke(dp(1), Color.parseColor(strokeHex))
        }
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

    private fun addDividerIn(parent: LinearLayout) {
        parent.addView(
            View(this).apply {
                setBackgroundColor(Color.parseColor("#F3F4F6"))
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dp(1)
                ).apply {
                    topMargin = dp(14)
                    bottomMargin = dp(14)
                }
            }
        )
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun dp(value: Float): Int =
        (value * resources.displayMetrics.density).toInt()
}
