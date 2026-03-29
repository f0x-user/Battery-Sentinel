# BatterySentinel

A production-ready Android battery monitoring and optimization app for Google Pixel 8 Pro (Android 16 / Baklava). Works **without root** using the deepest available Android APIs.

## Features

### Tier 1 — Auto-granted (no setup required)
- **Real-time Dashboard**: Current (mA), voltage (mV), temperature (°C), percentage, charge status, plugged type — polled every 1 second via ForegroundService
- **Charge Alarm**: Configurable threshold (default 80%) notification when charging reaches target
- **Battery Health Estimation**: Coulomb-counting via `BATTERY_PROPERTY_CHARGE_COUNTER`, compared against Pixel 8 Pro design capacity (5050 mAh)
- **Drain Rate Tracking**: %/hour calculated separately for screen-on and screen-off periods
- **Charging Session History**: Stored in Room, shown in list with Vico line chart
- **Temperature Alerts**: Warning when battery temp ≥ 40°C
- **Anomaly Detection**: WorkManager periodic check (15 min), alerts when screen-off drain > 2× 7-day baseline
- **Boot Receiver**: Restarts ForegroundService on device boot

### Tier 2 — User grants in Settings
- **App Usage Stats** (`PACKAGE_USAGE_STATS`): Per-app foreground time last 24h, sorted by time
- **Write Settings** (`WRITE_SETTINGS`): Brightness slider (0–255), screen timeout selector, adaptive brightness toggle

### Tier 3 — One-time ADB grant
- **Battery Stats** (`BATTERY_STATS`): Per-app battery attribution (mAh) via `BatteryStatsManager`
- **Write Secure Settings** (`WRITE_SECURE_SETTINGS`): Toggle battery saver, control Doze aggressiveness

## Screenshots

| Dashboard | Charging History | App Usage | Optimize | Settings |
|-----------|-----------------|-----------|----------|---------|
| _(placeholder)_ | _(placeholder)_ | _(placeholder)_ | _(placeholder)_ | _(placeholder)_ |

## Permissions

| Permission | Type | Purpose | How to Grant |
|------------|------|---------|--------------|
| `FOREGROUND_SERVICE` | Normal | Run background battery monitor | Auto |
| `FOREGROUND_SERVICE_SPECIAL_USE` | Normal | Foreground service type | Auto |
| `POST_NOTIFICATIONS` | Runtime | Charge/temp/anomaly alerts | Runtime dialog |
| `RECEIVE_BOOT_COMPLETED` | Normal | Restart service on boot | Auto |
| `WAKE_LOCK` | Normal | Keep service running | Auto |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Special | Prevent service from being killed | Onboarding dialog |
| `PACKAGE_USAGE_STATS` | AppOps | Per-app foreground time | Settings → Usage access |
| `WRITE_SETTINGS` | Special | Brightness/timeout control | Settings → Modify system settings |
| `BATTERY_STATS` | Signature | Per-app battery attribution | ADB (see below) |
| `WRITE_SECURE_SETTINGS` | Signature | Battery saver / Doze control | ADB (see below) |
| `READ_SYNC_SETTINGS` | Normal | Read sync state | Auto |
| `WRITE_SYNC_SETTINGS` | Normal | Toggle background sync | Auto |
| `KILL_BACKGROUND_PROCESSES` | Normal | Kill own background processes | Auto |

## ADB Commands for Tier 3 Permissions

Connect your Pixel 8 Pro via USB with USB debugging enabled, then run:

```bash
# Grant battery stats permission (per-app mAh attribution)
adb shell pm grant com.flamefox.batterysentinel android.permission.BATTERY_STATS

# Grant write secure settings (battery saver + Doze control)
adb shell pm grant com.flamefox.batterysentinel android.permission.WRITE_SECURE_SETTINGS
```

To verify grants:
```bash
adb shell dumpsys package com.flamefox.batterysentinel | grep -E "BATTERY_STATS|WRITE_SECURE"
```

## Build Instructions

### Requirements
- Android Studio Meerkat (2025.x) or newer
- JDK 21
- Android SDK 36 (Baklava)
- Gradle 8.14+

### Build
```bash
git clone <repo-url>
cd BatterySentinel
./gradlew assembleDebug
```

### Run Tests
```bash
# Unit tests (all modules)
./gradlew test

# Instrumented tests (requires connected device)
./gradlew connectedAndroidTest
```

## Architecture

```
┌─────────────────────────────────────────────────────┐
│                        app/                          │
│  (Hilt DI setup, Navigation, MainActivity,          │
│   BatterySentinelApp, AppModule)                    │
└───────────────┬─────────────────────────────────────┘
                │ depends on
    ┌───────────▼───────────┐
    │    presentation/       │  ViewModels + Compose Screens
    │  (Dashboard, Charging, │  6 screens, Hilt ViewModels
    │   Apps, Optimize,      │  StateFlow + Turbine tests
    │   Settings, Onboarding)│
    └───────────┬────────────┘
                │ depends on
    ┌───────────▼────────────┐
    │       domain/          │  Pure Kotlin
    │  (Models, Repository   │  No Android deps
    │   interfaces, UseCases)│  JUnit5 + MockK tests
    └───────────┬────────────┘
                │ implemented by
    ┌───────────▼────────────┐
    │        data/           │  Repository impls
    │  (BatteryManagerSource,│  ForegroundService
    │   DataStore, Room DAOs,│  WorkManager Worker
    │   BootReceiver, Worker)│  Hilt DI
    └───┬────────────────────┘
        │ uses
    ┌───▼──────────────────────────────────────────┐
    │              core/                            │
    │  common/  — Extensions, Constants            │
    │  ui/      — Compose Theme (M3), Components   │
    │  database/— Room DB, DAOs, Entities, DI      │
    └──────────────────────────────────────────────┘
```

## Tech Stack

- **Kotlin 2.x** + Coroutines / StateFlow / SharedFlow
- **Jetpack Compose** + Material 3
- **Hilt** (Dependency Injection)
- **Room** (Charging session + battery sample persistence)
- **DataStore Preferences** (App settings)
- **WorkManager** (Periodic anomaly detection)
- **Vico** (Charts — charging history health trend)
- **Navigation Compose** (Single-activity navigation)
- **KSP** (Annotation processing — Room, Hilt)

## License

MIT License — see [LICENSE](LICENSE) for details.
