# FileSalvage for Android

A real Android port of the iOS FileSalvage app. Built with **Kotlin + Jetpack Compose**.

---

## How to Build the APK

### Requirements
- Android Studio Hedgehog (2023.1.1) or newer — download free at https://developer.android.com/studio
- JDK 17 (bundled with Android Studio — nothing extra needed)
- Android SDK 34 (Android Studio installs this on first launch)
- A device or emulator running Android 8.0+ (API 26+)

### Steps

**1. Open the project**
```
File → Open → select the FileSalvageAndroid folder
```
Android Studio will sync Gradle automatically. First sync takes ~2–5 minutes (downloads dependencies).

**2. Build a debug APK** (fastest — no signing needed)
```
Build → Build Bundle(s) / APK(s) → Build APK(s)
```
Output: `app/build/outputs/apk/debug/app-debug.apk`

**OR use the terminal:**
```bash
./gradlew assembleDebug
```

**3. Install on your device**
Enable USB debugging on your phone, connect via USB, then:
```bash
./gradlew installDebug
```

**4. Build a release APK** (for distribution)
- Create a keystore: `Build → Generate Signed Bundle / APK`
- Follow the wizard to create/use a `.jks` keystore file
- Select APK → fill in keystore details → Release → Finish

Output: `app/build/outputs/apk/release/app-release.apk`

---

## What This App Does (Real Functionality)

### Scanning
The scanner uses Android's **MediaStore API** — the same database Android uses to track all media files. It performs real queries across multiple sources:

| Source | What it scans |
|--------|--------------|
| `IS_TRASHED=1` media | Files in Android's Recycle Bin (deleted in last 30 days) |
| `IS_PENDING=1` media | Incomplete/interrupted file transfers |
| `MediaStore.Downloads` | Deleted downloads |
| Orphaned files | Hidden/temp files on external storage |
| App container orphans | Cache/files dirs with leftover media |
| Removable volumes | SD card and USB OTG storage |

### Recovery
Files are recovered via three real strategies (in priority order):
1. **Un-trash** — If a file is in Android's Recycle Bin (`IS_TRASHED=1`), it's un-trashed instantly via `ContentResolver.update()`. The original file is fully restored.
2. **Stream copy** — If the file has a valid `contentUri`, it's copied via `ContentResolver.openInputStream()` to Pictures/Movies/Downloads.
3. **File copy** — If the file has a known path on disk, it's copied directly.
4. **Recovery record** — For files whose blocks are no longer accessible, a `.txt` metadata record is written noting what was found.

### Permissions Used
| Permission | Why |
|-----------|-----|
| `READ_MEDIA_IMAGES/VIDEO/AUDIO` (API 33+) | Access media to scan |
| `READ_EXTERNAL_STORAGE` (API ≤ 32) | Access storage to scan |
| `MANAGE_EXTERNAL_STORAGE` | Deep scan of non-media files (optional — app works without it) |
| `FOREGROUND_SERVICE` | Keep CPU alive during long Deep/Full scans |
| `WAKE_LOCK` | Prevent sleep during scan |

---

## Project Structure

```
FileSalvageAndroid/
├── app/
│   ├── build.gradle                    — dependencies (Compose, Coil, etc.)
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml         — permissions + components
│       └── java/com/filesalvage/
│           ├── FileSalvageApp.kt       — Application class, notification channels
│           ├── MainActivity.kt         — Entry point, nav host
│           ├── models/
│           │   └── RecoverableFile.kt  — FileType, ScanDepth, data models
│           ├── services/
│           │   ├── FileScanner.kt      — 9-step MediaStore + filesystem scanner
│           │   ├── FileRecoveryService.kt — 4-strategy recovery engine
│           │   └── ScanForegroundService.kt — Notification for long scans
│           ├── viewmodels/
│           │   └── ScanViewModel.kt    — StateFlow-based state management
│           └── ui/
│               ├── theme/Theme.kt      — Dark theme, brand colours
│               ├── HomeScreen.kt       — Animated landing + scan depth picker
│               ├── ScanningScreen.kt   — Radar animation + live progress
│               ├── ResultsScreen.kt    — File list, thumbnails, type filters
│               ├── RecoveryScreen.kt   — Recovery progress + completion stats
│               └── PermissionsScreen.kt — Runtime permissions handler
├── build.gradle
├── settings.gradle
└── gradle/wrapper/gradle-wrapper.properties
```

---

## Minimum Requirements for Full Recovery

- **Android 11+** — Full Recycle Bin (IS_TRASHED) access → best recovery rates
- **Android 10** — MediaStore.Downloads access, scoped storage
- **Android 8–9** — Basic MediaStore scan, READ_EXTERNAL_STORAGE
- **MANAGE_EXTERNAL_STORAGE** — Optional. Enables deep filesystem scan for non-media files. User must grant from Settings → Apps → Special app access.

---

## Notes

- **No root required** for photo/video/audio recovery from the Recycle Bin
- Root would unlock deeper block-level recovery (not implemented — requires NDK + `/proc` access)
- Recovered files are saved to `Pictures/FileSalvage`, `Movies/FileSalvage`, or `Download/FileSalvage`
- The app never deletes or modifies any file without explicit user action
