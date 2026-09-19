# MedRemind 💊
> Precision Medication Adherence & Inventory Management Native Android App

MedRemind is an intelligent medication scheduling, adherence tracking, and inventory refill management Android application built natively with **Kotlin**, **Jetpack Compose**, **Material Design 3**, and Android **AlarmManager**.

---

## 🌟 Key Features

- **🔔 High-Priority Lock-Screen Alarms**: Uses `AlarmManager.setAlarmClock` with full-screen lock-wake activity and continuous audio ringtones to guarantee you never miss a dose.
- **🕒 Multi-Dose Daily Regimens**: 1x, 2x, 3x, 4x daily dose presets and custom intake time slots with individual intake confirmations.
- **📅 Flexible Regimen Scheduling**: Schedule by Everyday, Alternate Days, Specific Weekdays (e.g., Mon/Wed/Fri), or an interactive multi-date calendar picker.
- **📦 Inventory Vault & Low Supply Alerts**: Real-time stock tracking with auto-deduction upon intake confirmation and 1-tap quick refill actions (+10, +30, custom amount).
- **📊 Clinical Ledger & Monthly Compliance Matrix**: Adherence scorecards, daily compliance breakdown, and exportable CSV reports for doctor consultations.
- **💊 User-Driven Custom Medications**: Easily add, edit, and manage your own custom medications with dosage amounts, units, forms (tablets, capsules, syrups, injections, drops), and meal conditions.

---

## 📁 Repository Structure

```
MedRemind/
├── MedicalReminderNative/       # Complete Native Android Studio Project
│   ├── app/                     # App module (Jetpack Compose UI, ViewModels, Alarm Scheduler)
│   ├── gradle/                  # Gradle wrapper configuration
│   ├── build.gradle.kts         # Root Gradle build script
│   └── settings.gradle.kts      # Project module settings
├── .gitignore                   # Ignore rules for build artifacts & IDE configs
└── README.md                    # Project documentation
```

---

## 🚀 Getting Started

1. Clone or download the repository:
   ```bash
   git clone https://github.com/rohitkalisipudi7-design/MedRemind.git
   ```
2. Open the `MedicalReminderNative` directory in **Android Studio** (Ladybug / Hedgehog or newer).
3. Allow Gradle to sync dependencies.
4. Run the app on an Android device or emulator running **Android 8.0 (API 26) or higher**.

---

## 🛠️ Architecture & Tech Stack

- **UI Framework**: Jetpack Compose with Material 3 Design
- **Language**: Kotlin 2.0+
- **Architecture**: MVVM with `StateFlow` & `Coroutines`
- **Notifications & Alarms**: `AlarmManager` with Exact Alarms & Full-Screen Intent `AlarmActivity`
- **Data Persistence**: Local on-device storage with JSON serialization

---

## 🔒 Privacy
All medication schedules, logs, and stock counts are stored 100% locally on-device (`SharedPreferences`) with no external tracking or telemetry.
