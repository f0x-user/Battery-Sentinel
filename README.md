# BatterySentinel

A battery monitoring and optimization app for Android. No root required.

Real-time stats, charging session history, per-app usage, and system setting controls — all in one place, all stored locally on the device.

## What it does

- Live dashboard: percentage, current (mA), voltage, temperature, charge cycles, health, drain rate
- Charging sessions: recorded automatically, browsable history
- App usage: foreground time per app (last 24 h), per-cycle view linked to charging sessions
- Optimize: brightness, screen timeout, sync toggle — apply in one tap
- System backups: up to 5 rotating snapshots of system settings, individually restorable
- Alerts: configurable charge threshold and temperature warnings

## Permissions

Most features work without any setup. Two optional grants unlock more:

- **Usage Statistics** — required for the Apps screen (Settings → Usage access)
- **Write Settings** — required for brightness and timeout control (Settings → Modify system settings)

## Quick Start

```bash
git clone https://github.com/f0x-user/Battery-Sentinel.git
cd Battery-Sentinel
./gradlew assembleDebug
```

Requires Android Studio Meerkat, JDK 17, and SDK 36.

## Tech Stack

Kotlin · Jetpack Compose · Hilt · Room · DataStore · WorkManager · Material 3

## License

MIT — see [LICENSE](LICENSE).
