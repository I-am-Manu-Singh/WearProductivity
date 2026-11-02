# LifeOS: Companion & Wear OS Ecosystem

LifeOS is an ultra-modern, local-first, privacy-respecting Personal Operating System and Productivity Suite engineered with **Kotlin Multiplatform (KMP)**, **Jetpack Compose / Compose for Wear OS (Material 3)**, and **Google Play Services Wearable Data Layer**.

It features an end-to-end synchronized Phone + Watch ecosystem providing sub-second latency state reconciliation, hardware audio synthesis, offline-first health and biometric telemetry, Wi-Fi Direct P2P file transfers, and cloud backup.

---

## 📑 Table of Contents

1. [System Architecture](#-system-architecture)
2. [Module Breakdown](#-module-breakdown)
3. [Companion & Wear OS Feature Parity](#-companion--wear-os-feature-parity)
    - [1. Dashboard & Watch Face Studio](#1-dashboard--watch-face-studio)
    - [2. Unified Calendar, Kanban & Eisenhower Matrix](#2-unified-calendar-kanban--eisenhower-matrix)
    - [3. Habit Formation & Routine Timetable](#3-habit-formation--routine-timetable)
    - [4. Deep Work Focus & Ambient Audio Synthesizer](#4-deep-work-focus--ambient-audio-synthesizer)
    - [5. Intermittent Fasting & Macro Nutrition](#5-intermittent-fasting--macro-nutrition)
    - [6. Strength Gym & C25K Running Intervals](#6-strength-gym--c25k-running-intervals)
    - [7. Audio Studio, DSP Equalizer & LRCLIB Lyrics](#7-audio-studio-dsp-equalizer--lrclib-lyrics)
    - [8. File Explorer, Storage Analytics & P2P Beaming](#8-file-explorer-storage-analytics--p2p-beaming)
    - [9. Settings & Remote Watch Studio](#9-settings--remote-watch-studio)
4. [Omni-Sync & Multi-Transport Engine](#-omni-sync--multi-transport-engine)
5. [Visual Showcase & Screenshots](#-visual-showcase--screenshots)
6. [Tech Stack & Key Dependencies](#-tech-stack--key-dependencies)
7. [Permissions & Privacy](#-permissions--privacy)
8. [Getting Started & Building](#-getting-started--building)

---

## 🏗 System Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              LifeOS Ecosystem                               │
└─────────────────────────────────────────────────────────────────────────────┘
                                       ▲
                                       │
            ┌──────────────────────────┴──────────────────────────┐
            ▼                                                     ▼
┌─────────────────────────┐                             ┌───────────────────┐
│     :mobile (Android)   │                             │  :wear (Wear OS)  │
│  • Material 3 Suite     │                             │  • Dial Canvas UI │
│  • Companion Studio     │                             │  • Complications  │
│  • MediaSession/Exo     │                             │  • Tiles & Haptic │
│  • Firebase Realtime    │                             │  • Sensor Engine  │
└───────────┬─────────────┘                             └─────────┬─────────┘
            │                                                     │
            │          Google Play Services Data Layer            │
            │      (Urgent PutDataMap, ChannelClient, P2P)        │
            └──────────────────────────┬──────────────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                                :shared (KMP)                                │
│  • Room Multiplatform SQLite Database (17 DAOs, WAL Mode, Migration Safe)   │
│  • Conflict-Free Replication (`SyncStamp` Lamport clocks, Version Bumpers)   │
│  • Property-level 3-way merge engine (`LifeSyncEngine`)                     │
│  • Media Scanning, P2P Socket Protocols & File Transfer Acknowledgment      │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 📦 Module Breakdown

- **`:shared` (Kotlin Multiplatform / Android Main)**:
  - **Room Database**: 17 compiled entities and DAOs (`TaskDao`, `HabitDao`, `FastDao`, `FoodLogDao`, `GymDao`, `RunDao`, `EventDao`, `AlarmDao`, `BudgetDao`, `ExpenseDao`, `FocusDao`, `WellnessDao`, `ImportantDateDao`, etc.).
  - **State Engine**: Thread-safe `LifeRepository` backed by `MutableStateFlow` and persistent transaction mutexes.
  - **Synchronization**: `AndroidWearSyncTransport`, `SyncPacket`, `LifeSyncEngine` property-level merge rules.
  - **File Protocol**: `FileTransferProtocol`, `FileTransferAckRegistry`, and channel streaming parsers.
  - **Media Engine**: `LocalMediaScanner` for system audio and storage traversal.
- **`:mobile` (Companion Application)**:
  - 10-screen navigation suite matching 100% of Wear OS watch features.
  - Ambient procedural PCM 16-bit sound generator (`AudioTrack`).
  - System background services: `MobileDataService` and `MobileMediaLibraryService`.
  - Google Cloud / Firebase Firestore live synchronization and offline JSON backup/restore.
- **`:wear` (Wear OS Round Dial Application)**:
  - Bespoke Wear Compose components (`TransformingLazyColumn`, `ScreenScaffold`, `EdgeButton`).
  - Active Complication Data Sources and Wear OS Glanceable Tiles.
  - On-wrist sensor acquisition: Continuous heart rate BPM, accelerometer steps, and sleep stages.
  - Hardware AudioTrack player and local Wi-Fi Direct Watch-to-Watch P2P sockets.

---

## 🎯 Companion & Wear OS Feature Parity

### 1. Dashboard & Watch Face Studio
- **Complication Studio**: Visual live preview for all 6 watch slots (Top, Upper-L, Upper-R, Bottom-L, Bottom-R, Bottom Ring) with 1-tap remote push to watch.
- **Remote Watch Diagnostics**: Live ping test (`/lifeos/ping` $\rightarrow$ `pong`), "Find My Watch" loud alert trigger, battery telemetry gauge, and full-state broadcast.
- **Glanceable Telemetry**: Real-time Fasting progress, Biometrics & Sleep recovery, Daily Routines, Alarms, Workout generators, and Milestone countdowns.

### 2. Unified Calendar, Kanban & Eisenhower Matrix
- **5 Planner Tabs**:
  - `SPOTLIGHT`: Hero daily mission with day completion percentage ring.
  - `DAY_AGENDA`: Interactive horizontal date strip + synced device calendars + expandable task sub-checklists.
  - `KANBAN_MATRIX`: 3-column Kanban Board (`To Do`, `In Progress`, `Done`) and Eisenhower 2x2 Matrix (`Urgent/Important` quadrants) with live drag-and-drop status cycling.
  - `MONTH_VIEW`: Full interactive month grid displaying colored markers for tasks, events, and alarms.
  - `COUNTDOWNS_ALARMS`: Wake-up alarms with remote ping + milestone countdown chips (Anniversaries, Exams, Birthdays).

### 3. Habit Formation & Routine Timetable
- **4 Habits Tabs**:
  - `SPOTLIGHT`: Featured priority habit streak card with live routine progression slot.
  - `HABITS_DECK`: Multi-increment steppers (`+1`, `+2`, `+5`), category filters, and horizon scopes (`Today`, `Weekly`, `Monthly`).
  - `TIMETABLE`: 24-hour daily routine timeline tracking state cycles (`Pending` $\rightarrow$ `Doing` $\rightarrow$ `Done`).
  - `SUMMARY_SCORE`: Algorithmic Discipline Consistency Score (%) with automated next-day backlog rollover engine.

### 4. Deep Work Focus & Ambient Audio Synthesizer
- **Synthesizer Engine**: Real-time procedural 16-bit mono PCM `AudioTrack` ambient sound generator:
  - *White Noise*, *Natural Rainfall*, *Ocean Waves* (sine-modulated low-frequency oscillator), and *40Hz Gamma Binaural Beats*.
- **Timer & Topic Presets**: Customizable Pomodoro countdown clock (`25m`, `+15m`, pause/resume) with topic library (`Deep Coding`, `Writing`, `Reading`, `Math`) and remote watch alert ping.
- **Analytics**: Deep work daily quota tracker and session history log.

### 5. Intermittent Fasting & Macro Nutrition
- **Active Fasting**: Real-time boundary countdown, circular progress ring, and controls (`Start`, `Stop`, `Pause`, `To Eat`, `To Fast`).
- **Fasting Protocols**: 1-tap switching between `16:8`, `18:6`, `20:4`, and `14:10` schedules + dynamic water hydration stepper.
- **Diet & Macros**: Calorie goal gauge, Protein/Carbs/Fat macro breakdown, diet presets (`High Protein`, `Keto`, `Balanced`), and multi-meal food logger.
- **Vitals Logger**: Heart rate BPM, sleep recovery percentage, body weight tracker, and energy/mood sliders.

### 6. Strength Gym & C25K Running Intervals
- **Active Gym HUD**: Exercise card with set-by-set progression (`Set 1/4`), rep/weight readouts, resting timer countdown, and remote "Start on Watch" trigger (`/lifeos/workout/start_gym`).
- **Live Set Adjuster**: Weight steppers ($\pm 2.5\text{kg}$) and rep steppers ($\pm 1$) with automatic progression targets.
- **Program Builder**: Preloaded strength routines (*Push Day*, *Pull Day*, *Leg Day*, *Core*) + Couch to 5K running intervals.
- **PR Maxes**: Personal record tracker (Bench Press, Squat, Deadlift, Overhead Press) with historical logs.

### 7. Audio Studio, DSP Equalizer & LRCLIB Lyrics
- **Media Player Canvas**: Wave visualizer disk, scrub slider, 4-state repeat, 3-state shuffle, and favorite heart toggle.
- **6-Subtab Library**: Active Queue, Scanned Device Tracks (`LocalMediaScanner`), Custom Playlists, Albums, Artists, and Storage Folders.
- **Hardware 5-Band Equalizer**: Canvas curve frequency graph (60Hz, 230Hz, 910Hz, 3.6kHz, 14kHz) with individual $\pm 12\text{dB}$ sliders and preset filters (`Bass Heavy`, `Vocal Clarity`, `Treble Sparkle`, etc.).
- **Synchronized LRCLIB Lyrics**: Integrated `LrcLyricsParser` with automatic online lyric search from `lrclib.net`, clickable line seeking, and local cache.
- **Output Routing**: Live device audio switching (`Speaker`, `Bluetooth Wireless A2DP/BLE`, and `Watch Remote Streaming`).

### 8. File Explorer, Storage Analytics & P2P Beaming
- **Storage Analytics**: Real-time `StatFs` internal capacity gauge with categorized breakdown (`Audio`, `Video`, `Images`, `Docs`, `APKs`, `Downloads`).
- **Directory Browser**: Breadcrumb chip traversal, folder child counts, multi-category filters, and sort options.
- **Sequential P2P Queue**: One-by-one transmission pipeline over `DataLayer`, `Wi-Fi Direct`, or `Bluetooth` with 32KB chunk streaming and `FileTransferAckRegistry` delivery confirmation.
- **Integrity Inspection**: Full file metadata modal with on-demand background MD5 checksum verification.

### 9. Settings & Remote Watch Studio
- **Remote Preferences**: Configure watch display format (24-hr clock), haptic vibration engine, water intake targets, focus durations, and alarm lead times directly from the phone.
- **Omni-Sync & Backup**: 1-tap state broadcast to watch, JSON database export (`ACTION_SEND`), and file-based JSON backup restoration.
- **Remote Factory Reset**: Wipe watch preferences to factory defaults via `/lifeos/reset_wear_preferences` without affecting phone database.

---

## 📡 Omni-Sync & Multi-Transport Engine

| Transport Layer | Channel / Mechanism | Purpose & Scope |
| :--- | :--- | :--- |
| **Bluetooth (BLE/RFCOMM)** | Wearable Data Layer (`PutDataMapRequest.setUrgent()`) | Instant sub-second state sync, habit toggles, quick capture, media controls. |
| **Wi-Fi LAN / P2P Sockets** | Direct Watch-to-Phone Sockets & `ChannelClient` | High-throughput file streaming (PDFs, APKs, MP3s) and zero-cellular sync. |
| **Mobile Data & Cloud** | Firebase Firestore Realtime Document Listeners | Remote synchronization when phone and watch are separated or on different networks. |
| **Local File System** | Storage Access Framework (SAF) JSON Bundles | Full air-gapped export, offline archives, and disaster recovery. |

---

## 📸 Visual Showcase & Screenshots

### Mobile Companion Application

| Dashboard & Studio | Unified Calendar & Matrix | Habit Formation | Deep Work Synthesizer | Fasting & Diet |
| :---: | :---: | :---: | :---: | :---: |
| *(Attach `mobile_dashboard.png`)* | *(Attach `mobile_calendar.png`)* | *(Attach `mobile_habits.png`)* | *(Attach `mobile_focus.png`)* | *(Attach `mobile_fasting.png`)* |

| Workout & Gym HUD | Audio Studio & EQ | File Explorer & P2P | Settings & Remote Studio | Cloud & Export |
| :---: | :---: | :---: | :---: | :---: |
| *(Attach `mobile_workout.png`)* | *(Attach `mobile_audio.png`)* | *(Attach `mobile_files.png`)* | *(Attach `mobile_settings.png`)* | *(Attach `mobile_backup.png`)* |

### Wear OS Dial Application

| Glanceable Dashboard | Calendar Day View | Habits Ring | Fasting Dial | Gym Exercise HUD |
| :---: | :---: | :---: | :---: | :---: |
| *(Attach `wear_dashboard.png`)* | *(Attach `wear_calendar.png`)* | *(Attach `wear_habits.png`)* | *(Attach `wear_fasting.png`)* | *(Attach `wear_workout.png`)* |

| Audio Player Ring | DSP Equalizer Curve | Synced Lyrics Overlay | P2P File Receiver | Settings Deck |
| :---: | :---: | :---: | :---: | :---: |
| *(Attach `wear_audio.png`)* | *(Attach `wear_eq.png`)* | *(Attach `wear_lyrics.png`)* | *(Attach `wear_files.png`)* | *(Attach `wear_settings.png`)* |

---

## 🛠 Tech Stack & Key Dependencies

```kotlin
// Build Tooling & Compilers
Kotlin("2.1.0")
AndroidGradlePlugin("8.13.1")
KSP("2.1.0-1.0.29")

// UI & Design System
JetpackCompose("2024.10.01 / Material 3")
WearCompose("1.4.0 / androidx.wear.compose.material3")
WearComposeFoundation("1.4.0")

// Persistence & Concurrency
AndroidXRoom("2.7.0-alpha13 - KMP SQLite")
KotlinxCoroutines("1.9.0")
KotlinxSerialization("1.7.3")

// Media & Hardware
AndroidXMedia3("1.5.1 - ExoPlayer, Session, UI")
AudioTrack("Procedural PCM 16-bit Synthesizer")
HardwareEqualizer("android.media.audiofx.Equalizer")

// Connectivity & Wearable
PlayServicesWearable("18.2.0 - DataClient, MessageClient, ChannelClient")
FirebaseFirestore("25.1.1")
FirebaseAuth("23.1.0")
```

---

## 🔒 Permissions & Privacy

LifeOS follows an offline-first, principle-of-least-privilege model:

### Mobile Companion App
- `POST_NOTIFICATIONS`: Alarms, routine reminders, and background file transfer progress.
- `SCHEDULE_EXACT_ALARM`: Sub-second accuracy for wake-up alarms and fasting boundaries.
- `ACTIVITY_RECOGNITION` & `BODY_SENSORS`: Background step counting and health metric aggregation.
- `READ_MEDIA_AUDIO` / `READ_EXTERNAL_STORAGE`: Scanning local audio for the Audio Studio.
- `READ_CALENDAR`: Synchronizing device Google Calendar events to the daily agenda.
- `FOREGROUND_SERVICE_MEDIA_PLAYBACK`: Continuous background music and ambient white noise synthesis.

### Wear OS Watch App
- `BODY_SENSORS` & `BODY_SENSORS_BACKGROUND`: Continuous optical PPG heart rate and recovery metrics.
- `WAKE_LOCK` & `VIBRATE`: Tactile wrist haptics and screen wake during active workouts and fasting alerts.
- `BLUETOOTH_CONNECT` & `BLUETOOTH_SCAN`: Pairing audio headsets and phone companion linking.
- `ACCESS_WIFI_STATE` & `CHANGE_WIFI_MULTICAST_STATE`: Local high-speed Watch-to-Watch and Watch-to-Phone P2P sockets.
- `BIND_COMPLICATION_DATA_SOURCE` & `BIND_TILE_PROVIDER`: Rendering glanceable tiles and watch face complications.

---

## 🚀 Getting Started & Building

### Prerequisites
- **Android Studio Ladybug (2024.2.1+)** or IntelliJ IDEA Ultimate with KMP plugins.
- **Java Development Kit (JDK)**: OpenJDK 17 (`JAVA_HOME` pointed to JDK 17).
- Connected Android Phone (API 26+) and Wear OS Watch (Wear OS 3.0+ / API 30+).

### Gradle Commands
```bash
# 1. Clean and build all modules
./gradlew clean assembleDebug

# 2. Build Mobile Companion APK
./gradlew :mobile:assembleDebug

# 3. Build Wear OS Watch APK
./gradlew :wear:assembleDebug

# 4. Install onto connected targets
adb -s <PHONE_DEVICE_ID> install -r mobile/build/outputs/apk/debug/mobile-debug.apk
adb -s <WATCH_DEVICE_ID> install -r wear/build/outputs/apk/debug/wear-debug.apk
```

---

*LifeOS is engineered for focus, physical discipline, and digital resilience.*
