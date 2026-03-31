# System-Backup in BatterySentinel

## Was wird gesichert?

Beim **jedem Start** der App speichert BatterySentinel automatisch einen Schnappschuss aller
System-Einstellungen, die die App verändern kann:

| Einstellung | Beschreibung |
|---|---|
| Helligkeit (Brightness) | Aktueller Helligkeitswert (0–255) |
| Adaptive Helligkeit | An/Aus |
| Screen-Timeout | Bildschirmabschalt-Verzögerung in ms |
| Energiesparmodus (Battery Saver) | An/Aus |
| Hintergrund-Synchronisierung (Sync) | An/Aus |
| Doze-Konstanten | `device_idle_constants` aus den globalen Einstellungen |

Zusätzlich wird der Zeitstempel des Backups gespeichert (`savedAt`).

## Wo wird das Backup gespeichert?

Das Backup wird in einer dedizierten **DataStore-Datei** auf dem Gerät gesichert:

```
/data/data/com.flamefox.batterysentinel/files/datastore/system_backup.preferences_pb
```

Diese Datei ist eine verschlüsselte Protobuf-Datei und nur für die App selbst zugänglich.
Das Backup verlässt das Gerät **nie** — es gibt keine Cloud-Synchronisierung.

## Wann wird das Backup angelegt?

Das Backup wird **einmal pro App-Start** aktualisiert, direkt in `MainActivity.onCreate()`,
bevor die Benutzeroberfläche gerendert wird. Das bedeutet:

- Beim allerersten Start: Backup spiegelt den werkseitigen/vorherigen Zustand wider.
- Bei jedem weiteren Start: Das Backup wird auf den aktuellen Zustand aktualisiert.

> **Empfehlung:** Wenn du weißt, dass du kritische Einstellungen ausprobieren möchtest,
> **starte die App erst, dann nimm die Änderungen vor** — so ist der Backup-Zeitpunkt
> der Zustand *vor* deinen Änderungen.

## Wie wird wiederhergestellt?

1. Öffne **Settings** (Einstellungen) in der App.
2. Scrolle nach unten bis zum Abschnitt **„System-Backup"**.
3. Du siehst den gespeicherten Zustand (Zeitstempel + Werte).
4. Tippe auf **„Backup wiederherstellen"** (roter Button).
5. Bestätige den Sicherheitsdialog mit **„Wiederherstellen"**.
6. Ein Ergebnis-Dialog meldet:
   - ✅ **Erfolgreich** — alle Einstellungen wurden vollständig zurückgesetzt.
   - ⚠️ **Teilweise** — einige Einstellungen konnten nicht gesetzt werden
     (fehlendes `WRITE_SETTINGS`- oder `WRITE_SECURE_SETTINGS`-Permission).
   - ❌ **Kein Backup** — noch kein Backup vorhanden (App neu starten).

## Welche Berechtigungen werden benötigt?

Für die vollständige Wiederherstellung braucht die App dieselben Berechtigungen,
die auch für die Optimierungs-Funktionen benötigt werden:

| Einstellung | Benötigte Permission |
|---|---|
| Helligkeit, Adaptive Helligkeit, Screen-Timeout | `WRITE_SETTINGS` (über System-UI erteilbar) |
| Energiesparmodus, Doze-Konstanten | `WRITE_SECURE_SETTINGS` (nur per ADB) |
| Hintergrund-Synchronisierung | Keine spezielle Permission nötig |

Fehlen Berechtigungen, wird die Wiederherstellung mit dem Status „Teilweise" abgeschlossen —
die Einstellungen, für die Berechtigungen vorhanden sind, werden trotzdem wiederhergestellt.

## Technische Implementierung

```
MainActivity.onCreate()
  └─► settingsRepository.saveSystemBackup()         # liest IST-Zustand, speichert in DataStore

Settings → "Backup wiederherstellen"
  └─► SettingsViewModel.restoreSystemBackup()
        └─► SystemSettingsRepository.restoreSystemBackup()
              └─► liest SystemBackup aus SystemBackupDataStore
              └─► schreibt jeden Wert zurück via SystemSettingsDataSource
```

**Beteiligte Dateien:**

- `domain/model/AppUsage.kt` — `SystemBackup` Datenklasse
- `domain/repository/SystemSettingsRepository.kt` — Interface: `saveSystemBackup()`, `restoreSystemBackup()`, `getSystemBackup()`
- `data/datastore/SystemBackupDataStore.kt` — Persistenz in DataStore
- `data/repository/SystemSettingsRepositoryImpl.kt` — Implementierung
- `app/MainActivity.kt` — Backup beim App-Start auslösen
- `presentation/settings/SettingsViewModel.kt` — `restoreSystemBackup()`, `RestoreResult`
- `presentation/settings/SettingsScreen.kt` — `SystemBackupCard` Composable mit Button und Dialogen
