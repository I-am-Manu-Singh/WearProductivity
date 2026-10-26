# LifeOS Companion

LifeOS Companion is a sophisticated local-first personal operating system built with Kotlin Multiplatform (KMP), Jetpack Compose, and Wear OS. It serves as a unified command center for productivity, health, and finance, designed specifically for a seamless Phone + Watch ecosystem.

## 🏗 Architecture & Modules

The project follows a modular architecture to share business logic while providing native UI for different form factors:

- **`:shared` (KMP)**:
    - **Models**: Unified `LifeState` and `SyncStamp` for conflict-free synchronization.
    - **Repository**: `LifeRepository` handles all business logic, state transitions, and sync orchestration.
    - **Logic**: `LifeSyncEngine` for merging states, `LifeReminderPlanner` for scheduling, and `LifeExporter` for data portability.
- **`:mobile` (Android)**:
    - Material 3 dashboard with comprehensive views for planning, tracking, fitness, and finance.
    - Platform services for system alarms, notifications, and Wear OS Data Layer bridging.
- **`:wear` (Wear OS)**:
    - Round-optimized Compose UI.
    - Low-friction entry points for quick capture, workout tracking, and glanceable dashboard metrics.

## 🚀 Key Features

### 📅 Command Center
- **Today Dashboard**: Unified view of tasks, habits, fasting progress, and upcoming events.
- **Quick Capture**: Rapid entry for tasks, notes, and expenses from both devices.

### 🛠 Productivity & Planning
- **Task Management**: Project-based todos with priority and due labels.
- **Notes**: Pinned notes with linking capabilities.
- **Reviews**: Guided daily and weekly reflection prompts.

### ⚡️ Wellness & Health
- **Intermittent Fasting**: Active timer with progress tracking and haptic alerts.
- **C25K Running**: Multi-week guided run plan with interval cues.
- **Gym Log**: Exercise library with set performance and PR tracking.
- **Bio-tracking**: Water intake, mood, energy, and weight logging.

### 💰 Finance
- **Expense Logger**: Quick currency-aware entry.
- **Budgeting**: Category-based limits with visual spend warnings.

### 🔄 Synchronization & Persistence
- **Local-First**: Data lives on-device for instant responsiveness.
- **Sync Packets**: Conflict-resolution based on `SyncStamp` (updatedAt + version) via Wear OS Data Layer.

## 🛠 Engineering Deep Dive

### Sync Strategy
The app uses a `SyncStamp` on every entity. When devices connect, they exchange `SyncPacket`s. The `LifeSyncEngine` performs a property-level merge, preferring the version with the latest `updatedAt` or higher `version`.

### Platform Integration
- **Wear OS**: Custom `RoundProgress` canvas components and `WearModeRail` navigation.
- **Alarms**: Integration with `AlarmManager` for exact, power-efficient reminders.
- **Data Export**: `LifeExporter` generates CSV and JSON bundles for external backups.

## 🚧 Next Engineering Milestones

1. **Robust Persistence**: Replace `SharedPreferences` with Room KMP / SQLDelight for structured queries.
2. **Data Layer Implementation**: Full `DataClient` and `MessageClient` integration for real-time background sync.
3. **Health Integration**: Connect Wear exercises to Health Services and Phone data to Health Connect.
4. **Rich Complications**: Implementation of Wear OS Tiles and watch face complications.
5. **Interactive Widgets**: Android Home Screen widgets for the Today summary.

## 🛠 Build & Development

1. **Requirements**: Android Studio Ladybug+ or IntelliJ IDEA with KMP plugins.
2. **Build**:
   ```bash
   ./gradlew :mobile:assembleDebug :wear:assembleDebug
   ```
3. **Test**:
   ```bash
   ./gradlew :shared:test
   ```

---
*LifeOS is built for those who want their tools to be as mobile and resilient as they are.*
