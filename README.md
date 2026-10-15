# PocketRouter AutoHotspot 📶📱

> Turn any Android phone into an automatic, plug-and-play 4G/5G pocket Wi-Fi router.

[![Android Version](https://img.shields.io/badge/Android-8.0%2B%20(API%2026%2B)-brightgreen.svg)](https://developer.android.com)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Release](https://img.shields.io/badge/Release-APK-orange.svg)](https://github.com/Anjanasbr2003/Android-Auto-Hotspot-App-Generator/releases)

**PocketRouter AutoHotspot** is a lightweight, background automation app designed for Android. Whenever your phone turns on, reboots, or receives power (such as when connected to a car ignition or power outlet), PocketRouter automatically turns on your mobile hotspot without requiring any manual unlocking or tapping.

---

## 🌟 Key Features

- ⚡ **Automatic Hotspot on Boot (Direct-Boot Aware)**: Automatically triggers hotspot activation on system startup (`BOOT_COMPLETED`, `LOCKED_BOOT_COMPLETED`, and `QUICKBOOT_POWERON`).
- 🤖 **Accessibility Automation Engine**: Bypasses Android's restricted hotspot APIs on modern Android versions (Android 9 to 14+) by automating the Quick Settings panel securely.
- ⏱️ **Configurable Boot Delay**: Allows a custom countdown (5–60 seconds, default: 12s) after boot to ensure cellular network & SIM data connection are ready before activating the hotspot.
- 🌐 **Built-in Web Telemetry & Remote Dashboard (Port 8080)**:
  - Access a real-time web dashboard from any connected laptop, phone, or tablet at `http://<phone-ip>:8080`.
  - Check live hotspot status, battery level (%), and view automation logs.
  - Remotely re-trigger the hotspot toggle with one click via HTTP GET `/trigger`.
- 🔋 **Battery & Power Friendly**: Foreground watchdog service keeps the process alive without excessive battery drain.
- 🔍 **Customizable Tile Keywords**: Works with multiple languages and OEM skins (Samsung One UI, Xiaomi MIUI/HyperOS, Stock Android, etc.) by configuring tile keywords (`Mobile Hotspot`, `Hotspot`, `Wi-Fi hotspot`).
- 🎨 **Modern Dashboard UI**: Clean Material Design card interface displaying real-time system status, logs, and interactive diagnostics.

---

## 📋 Prerequisites

Before using PocketRouter, ensure the following on your Android device:

1. **Arrange Quick Settings Tile**:
   - Pull down your notification bar twice to view all Quick Settings tiles.
   - Tap the edit (pencil) icon and make sure the **Mobile Hotspot** tile is among the **top 6 tiles** (visible on the first pull-down page).
2. **Disable Hotspot Timeout**:
   - Go to Android **Settings > Network & internet (or Connections) > Mobile Hotspot and Tethering**.
   - Set "Turn off hotspot when idle" or "Hotspot timeout" to **Never**.
3. **Configure Hotspot SSID & Password**:
   - Configure your preferred Wi-Fi network name and password in Android settings once.

---

## 🚀 How to Install and Set Up

### Step 1: Install the APK
1. Download the latest `PocketRouter-debug.apk` from the [Releases](https://github.com/Anjanasbr2003/Android-Auto-Hotspot-App-Generator/releases) section.
2. Transfer and open the APK on your Android device.
3. If prompted by Android, tap **Settings** and allow **Install unknown apps** for your file manager or browser.
4. Complete the installation and open **PocketRouter**.

### Step 2: Grant Permissions
When you open the app for the first time, configure the necessary system permissions:
1. **Disable Battery Optimization**:
   - In the app, tap the prompt to ignore battery optimizations (set to **Unrestricted** / **Don't optimize**). This prevents Android from killing the background service.
2. **Enable Notification Permission** *(Android 13+)*:
   - Allow notifications so the foreground watchdog service remains persistently active.
3. **Enable Accessibility Service (Crucial)**:
   - Go to Android **Settings > Accessibility > Installed Apps (or Downloaded Apps)**.
   - Find **PocketRouter Auto-Hotspot Engine** and toggle it **ON**.
   - Confirm the accessibility permission prompt.

### Step 3: Configure Settings in App
- **Auto-Start on Boot**: Ensure the toggle switch is turned **ON**.
- **Boot Delay**: Set your preferred delay in seconds (default is `12` seconds, recommended for cellular data signal stabilization).
- **Tile Keywords**: Enter the name of your hotspot tile as it appears on your phone (e.g., `Mobile Hotspot`, `Hotspot`).

---

## 🧪 Testing

1. **In-App Test**:
   - Tap **"Test Hotspot Toggle"** in the app.
   - The app will automatically pull down the Quick Settings shade, locate and toggle the Hotspot tile, handle any confirmation prompts, and return to the home screen.
2. **Reboot Test**:
   - Restart your phone.
   - Wait for the countdown delay (e.g., 12 seconds).
   - The hotspot will automatically switch on.

---

## 🌐 Web Dashboard & Remote Control

Once your hotspot is running, any device connected to your hotspot Wi-Fi can view device telemetry:

1. Connect your computer or secondary device to the phone's Wi-Fi hotspot.
2. Open your web browser and navigate to:
   ```
   http://192.168.43.1:8080
   ```
   *(Note: Default Android hotspot gateway IP is usually `192.168.43.1` or check your device's Wi-Fi gateway IP)*.
3. **Available features on dashboard**:
   - Real-time **Hotspot Status** (Active / Standby).
   - Live **Battery Level (%)**.
   - **Recent Automation Logs**.
   - **"Re-Trigger Hotspot Toggle"** button to toggle the hotspot remotely.

---

## 🛠️ Building from Source

If you want to build the project yourself:

### Using Android Studio
1. Clone this repository:
   ```bash
   git clone https://github.com/Anjanasbr2003/Android-Auto-Hotspot-App-Generator.git
   ```
2. Open Android Studio and choose **Open an Existing Project**, selecting the `PocketRouter` directory.
3. Wait for Gradle sync to complete.
4. Run `Build > Build Bundle(s) / APK(s) > Build APK(s)`.

### Using Command Line (Gradle)
```bash
cd PocketRouter
# On Windows
.\gradlew.bat assembleDebug

# On macOS/Linux
./gradlew assembleDebug
```
The compiled APK will be located at:
`PocketRouter/app/build/outputs/apk/debug/app-debug.apk`

---

## ❓ Troubleshooting & Tips

- **Samsung Devices (One UI / One UI Core)**:
  - If a dialog pops up asking "Turn off Wi-Fi to use Mobile Hotspot?", PocketRouter automatically detects and confirms this dialog.
  - Disable "Auto power off" in Samsung Hotspot settings.
- **Xiaomi / Redmi / POCO (MIUI / HyperOS)**:
  - Enable **Autostart** in `Settings > Apps > Permissions > Autostart`.
  - Set Battery Saver to **No restrictions**.
- **Lock Screen / Direct Boot**:
  - The service is Direct-Boot aware (`LOCKED_BOOT_COMPLETED`), but for best results on devices acting as dedicated routers, consider setting screen lock to **None** or **Swipe**.

---

## 📄 License

This project is licensed under the Apache 2.0 License.
