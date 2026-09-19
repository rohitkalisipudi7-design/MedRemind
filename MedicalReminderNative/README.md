# 💊 MedReminder — Medical Reminder & Adherence Tracker

A modern, high-precision native Android application built with **Kotlin** and **Jetpack Compose** designed to help users track medication schedules, manage multiple daily doses, prevent missed medications with loud alarm wake screens, and monitor inventory supply.

---

## ✨ Key Features

### 1. 🔔 High-Priority Alarms & Loud Ringtones
- **Exact Minute-Level Timing**: Uses Android `AlarmManager.setAlarmClock` to ensure alarms fire with zero lag, even in deep Doze mode.
- **Full-Screen Wake Alert**: Wakes the phone screen on the lock screen with audio attributes set to the device's alarm stream.
- **Continuous Audible Ringtone**: Plays the loud alarm ringtone continuously until the user takes their medication, snoozes (15 min), or dismisses.

### 2. 🕒 Multiple Doses Per Single Day
- Schedule multiple intake times for a single medicine (e.g., Morning `08:00`, Afternoon `14:00`, Night `20:00`).
- **1-Tap Quick Presets**: Easily choose from `1x`, `2x`, `3x`, or `4x` daily presets or add custom times via the time picker.
- **Independent Dose Tracking**: Each dose appears chronologically on your daily schedule with its own "Take" action.

### 3. 📅 Flexible Frequency & Interactive Calendar Picker
- **Everyday**: Daily recurring reminders.
- **Alternate Days**: Schedules doses every 2 days from the start date.
- **Specific Weekdays**: Choose active days of the week (e.g., Mon, Wed, Fri).
- **Interactive Calendar Dates**: Tap arbitrary dates across any month using an interactive multi-date month calendar view (ideal for random or custom-interval medication courses).

### 4. 📦 Inventory Vault & Low Stock Alerts
- Automatically deducts stock count when a dose is taken and restores stock if unmarked.
- Urgent low-inventory warning banner when stock drops below threshold.
- Quick restock buttons (`+10`, `+30 units`, or custom amount).

### 5. 📊 Monthly Compliance Matrix & Adherence Report
- Visual monthly adherence rate scorecard (e.g. `95% Optimal Adherence`).
- Color-coded daily compliance heatmap (Green = 100% taken, Yellow = delayed/partial, Red = missed).
- Monthly taken vs. missed dosage metrics.

---

## 🛠️ Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (Material 3 with Dark Editorial theme)
- **Alarm Engine**: `AlarmManager` (`setAlarmClock`), `BroadcastReceiver`, `WakeLock`
- **Storage**: SharedPreferences with Kotlinx Serialization (`@Serializable`)
- **Architecture**: Clean Architecture with Reactive StateFlow

---

## 📱 Installation & Distribution

### Install on Any Android Device:
1. Locate the built APK file:
   `app/build/outputs/apk/debug/app-debug.apk`
2. Send `app-debug.apk` to any Android phone via WhatsApp, Telegram, Drive, or Email.
3. Tap the file on the phone to install. *(If prompted, allow "Install from unknown sources" and tap "Install anyway" on Play Protect)*.
4. Open the app and grant **Notification & Alarm** permissions.

### Build from Source:
```bash
# Build Debug APK
./gradlew assembleDebug

# Compile Kotlin Code
./gradlew compileDebugKotlin
```
