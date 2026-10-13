# SafeScreen: Comprehensive Codebase Architecture, Technical Audit & Developer Guide

> **Author**: Senior Android Engineer & Technical Documentation Specialist  
> **Target Audience**: Beginner to Intermediate Android & Kotlin Developers  
> **Project**: [SafeScreen](file:///c:/Users/Mukashifa%20Fatima/AndroidStudioProjects/SafeScreen)  
> **Status**: Complete Architectural & Codebase Audit  

---

## Table of Contents
1. [Executive Summary & Product Vision](#1-executive-summary--product-vision)
2. [High-Level Architecture & System Design](#2-high-level-architecture--system-design)
3. [Gradle Build System & Project Configuration](#3-gradle-build-system--project-configuration)
4. [AndroidManifest, Permissions & System Services](#4-androidmanifest-permissions--system-services)
5. [Application Entry Point & Dependency Injection (`AppContainer`)](#5-application-entry-point--dependency-injection)
6. [Data Layer: Room Database & DataStore Preferences](#6-data-layer-room-database--datastore-preferences)
7. [Security & Cryptography Subsystem](#7-security--cryptography-subsystem)
8. [Monitoring & Usage Tracking Subsystem](#8-monitoring--usage-tracking-subsystem)
9. [Domain Logic & Restriction Policy Engine](#9-domain-logic--restriction-policy-engine)
10. [Enforcement & Accessibility Overlay Subsystem](#10-enforcement--accessibility-overlay-subsystem)
11. [User Interface & Jetpack Compose Deep-Dive](#11-user-interface--jetpack-compose-deep-dive)
12. [Kotlin Language Concepts Grounded in SafeScreen](#12-kotlin-language-concepts-grounded-in-safescreen)
13. [Android Operating System & Jetpack Compose Fundamentals](#13-android-operating-system--jetpack-compose-fundamentals)
14. [Test Suite Audit & Verification Strategy](#14-test-suite-audit--verification-strategy)
15. [Security Audit, Edge Cases & Technical Critique](#15-security-audit-edge-cases--technical-critique)
16. [Developer Roadmap, Technical Glossary & Interview Prep](#16-developer-roadmap-technical-glossary--interview-prep)

---

## 1. Executive Summary & Product Vision

### 1.1 What is SafeScreen?
**SafeScreen** is an on-device, privacy-preserving digital wellbeing and parental control application designed exclusively for the Android platform. Unlike commercial parental control systems that transmit telemetry, screen activity, and application logs to cloud servers, SafeScreen operates **100% locally on the device**.

Its primary objective is to allow parents or individuals to regulate application usage through three primary control mechanisms:
1. **Hard Blocking**: Completely restricting access to selected apps at all times.
2. **Daily Usage Quotas**: Enforcing a maximum daily time allowance (e.g., 60 minutes/day for social media).
3. **Curfew Schedules**: Enforcing quiet hours (e.g., locking distracting applications between 22:00 and 07:00).

### 1.2 Core Value Proposition & Architectural Highlights
* **Zero Cloud Telemetry**: All rules, usage records, hashed secrets, and logs reside inside local SQLite/Room tables and Android DataStore files.
* **Cryptographic Security**: Administrative settings, rule modifications, and override mechanisms are locked behind a salted **PBKDF2-HMAC-SHA256** master PIN with tiered lockout delays and emergency recovery tokens.
* **Non-Intrusive Accessibility Enforcement**: The application leverages Android's `AccessibilityService` solely to detect foreground window transitions (`TYPE_WINDOW_STATE_CHANGED`). By explicitly setting `canRetrieveWindowContent = false`, SafeScreen guarantees that it cannot read user keystrokes, on-screen text, or private messages.
* **Reactive Compose UI**: SafeScreen features a modern, fluid user interface built entirely with **Jetpack Compose** and Google's Material Design 3, styled with four dark-mode palettes derived from the GNOME Ptyxis terminal suite.

---

## 2. High-Level Architecture & System Design

SafeScreen employs a **Clean, Layered Architecture** with unidirectional data flow (UDF). It separates responsibilities into clear layers: Presentation (Compose + ViewModels), Domain (pure Kotlin business logic), Data (Room + DataStore), and System Services (Accessibility, UsageStatsManager, Notifications).

### 2.1 System Architecture Diagram

```mermaid
graph TD
    subgraph UI_Layer ["Presentation Layer (Jetpack Compose)"]
        MainActivity["MainActivity\n(Single Activity Host)"]
        RootUI["SafeScreenRoot\n(Auth Gate & Theme Host)"]
        Scaffold["MainScaffold\n(Bottom Navigation)"]
        HomeScreen["HomeScreen & HomeViewModel\n(Dashboard & Stats)"]
        AppsScreen["AppsScreen & AppsViewModel\n(Rules Management)"]
        SettingsScreen["SettingsScreen & SettingsViewModel\n(Themes, PIN, Status)"]
        PinScreen["PinScreen & PinViewModel\n(Auth Keypad)"]
    end

    subgraph DI_Layer ["Dependency Injection"]
        AppContainer["AppContainer\n(Manual Service Locator / Singletons)"]
        SafeScreenApp["SafeScreenApp\n(Application Context Host)"]
    end

    subgraph Domain_Layer ["Domain & Security Engine"]
        Policy["RestrictionPolicy\n(Pure Business Rules)"]
        Warnings["UsageWarnings\n(80% Limit Alerts)"]
        PinManager["PinManager & PinHasher\n(PBKDF2 Security)"]
        PinLockout["PinLockout & RecoveryCode\n(Rate Limiting & Fallback)"]
    end

    subgraph System_Enforcement ["Monitoring & Enforcement Subsystem"]
        Accessibility["BlockerAccessibilityService\n(Window Transition Interceptor)"]
        BlockedActivity["BlockedActivity\n(Immersive Overlay UI)"]
        UsageMonitor["UsageMonitor & UsageSessions\n(UsageStatsManager Events)"]
        InstalledApps["InstalledAppsProvider\n(PackageManager Query)"]
        Notifier["Notifier\n(Notification Channels)"]
    end

    subgraph Data_Layer ["Data Layer (Local Persistence)"]
        RuleRepo["AppRuleRepository"]
        RoomDB["SafeScreenDatabase & AppRuleDao\n(SQLite Table: app_rules)"]
        SettingsRepo["SettingsRepository"]
        DataStore["DataStoreProvider\n(Preferences DataStore)"]
    end

    %% Wiring
    SafeScreenApp --> AppContainer
    MainActivity --> RootUI
    RootUI --> PinScreen
    RootUI --> Scaffold
    Scaffold --> HomeScreen
    Scaffold --> AppsScreen
    Scaffold --> SettingsScreen

    HomeScreen --> AppContainer
    AppsScreen --> AppContainer
    SettingsScreen --> AppContainer
    PinScreen --> AppContainer

    AppContainer --> RuleRepo
    AppContainer --> SettingsRepo
    AppContainer --> PinManager
    AppContainer --> UsageMonitor
    AppContainer --> InstalledApps
    AppContainer --> Notifier

    RuleRepo --> RoomDB
    SettingsRepo --> DataStore

    Accessibility --> AppContainer
    Accessibility --> Policy
    Accessibility --> BlockedActivity
    Accessibility --> UsageMonitor
```

### 2.2 Unidirectional Data Flow (UDF)

```mermaid
sequenceDiagram
    autonumber
    actor User as Parent/Child
    participant TargetApp as Monitored App (e.g. YouTube)
    participant OS as Android OS Window Manager
    participant Service as BlockerAccessibilityService
    participant Policy as RestrictionPolicy (Domain)
    participant Overlay as BlockedActivity

    User->>TargetApp: Launches Application
    OS->>Service: onAccessibilityEvent(TYPE_WINDOW_STATE_CHANGED, pkg)
    Service->>Service: Check in-memory rule cache & current usage
    Service->>Policy: evaluate(rule, currentUsageMinutes, now)
    alt Under Quota / Unrestricted
        Policy-->>Service: RestrictionDecision.Allowed
        Service-->>OS: Pass-through (Do nothing)
    else Quota Exceeded / Curfew Active / Blocked
        Policy-->>Service: RestrictionDecision.Blocked(reason)
        Service->>Overlay: startActivity(BlockedActivity, FLAG_ACTIVITY_NEW_TASK)
        Overlay-->>User: Renders full-screen barrier with reason & Unlock button
    end
```

---

## 3. Gradle Build System & Project Configuration

The project uses Gradle's Kotlin DSL (`.gradle.kts`) with a centralized Version Catalog (`gradle/libs.versions.toml`).

### 3.1 Version Catalog: [`gradle/libs.versions.toml`](file:///c:/Users/Mukashifa%20Fatima/AndroidStudioProjects/SafeScreen/gradle/libs.versions.toml)
Centralizes all dependencies, versions, and plugins:
* **AGP (Android Gradle Plugin)**: `8.7.2`
* **Kotlin**: `2.0.21` (incorporates the official Kotlin Compose compiler plugin)
* **Compile SDK / Target SDK**: `35` (Android 15)
* **Min SDK**: `26` (Android 8.0 Oreo - ensures modern Java 8+ APIs and notification channels)
* **Jetpack Compose BOM**: `2024.09.00`
* **Room Database**: `2.6.1` (using Google KSP `2.0.21-1.0.27` for compile-time code generation)
* **DataStore Preferences**: `1.1.1`
* **Coroutines**: `1.9.0`
* **JUnit**: `4.13.2`

### 3.2 Root Build Script: [`build.gradle.kts`](file:///c:/Users/Mukashifa%20Fatima/AndroidStudioProjects/SafeScreen/build.gradle.kts)
Configures top-level plugin application with `apply false`:
```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
}
```

### 3.3 Module Build Script: [`app/build.gradle.kts`](file:///c:/Users/Mukashifa%20Fatima/AndroidStudioProjects/SafeScreen/app/build.gradle.kts)
Defines Android-specific packaging, compiler flags, and library dependencies:
* `namespace = "com.example.safescreen"`
* `compileSdk = 35`
* Java version target: `JavaVersion.VERSION_11`
* Compose enabled: `buildFeatures { compose = true }`
* KSP Room dependency: `ksp(libs.androidx.room.compiler)` generates DAO implementations at build time.

---

## 4. AndroidManifest, Permissions & System Services

The application manifest ([`AndroidManifest.xml`](file:///c:/Users/Mukashifa%20Fatima/AndroidStudioProjects/SafeScreen/app/src/main/AndroidManifest.xml)) defines the security perimeter and Android system hooks.

### 4.1 Permissions Breakdown
1. **`android.permission.PACKAGE_USAGE_STATS`** (Special App Access):
   * *Purpose*: Grants access to `UsageStatsManager` to aggregate daily foreground usage.
   * *Protection Level*: System/Signature/Special access. Cannot be granted via normal runtime dialogs; user must be directed to `Settings.ACTION_USAGE_ACCESS_SETTINGS`.
2. **`android.permission.SYSTEM_ALERT_WINDOW`** (Draw over other apps):
   * *Purpose*: Permits displaying blocking overlays over third-party applications.
3. **`android.permission.POST_NOTIFICATIONS`** (Android 13+ / API 33+):
   * *Purpose*: Allows SafeScreen to dispatch warning notifications when an app reaches 80% of its daily threshold.
4. **`android.permission.QUERY_ALL_PACKAGES`** / Launcher Intent Queries:
   * *Purpose*: Enables `InstalledAppsProvider` to discover installed applications on modern Android versions (API 30+ package visibility restrictions).

### 4.2 Accessibility Service Declaration
```xml
<service
    android:name=".enforcement.BlockerAccessibilityService"
    android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"
    android:exported="true">
    <intent-filter>
        <action android:name="android.accessibilityservice.AccessibilityService" />
    </intent-filter>
    <meta-data
        android:name="android.accessibilityservice"
        android:resource="@xml/accessibility_service_config" />
</service>
```
* Binds to the system's accessibility dispatcher via `android.permission.BIND_ACCESSIBILITY_SERVICE` so only the Android OS can trigger it.

### 4.3 Accessibility Configuration: [`res/xml/accessibility_service_config.xml`](file:///c:/Users/Mukashifa%20Fatima/AndroidStudioProjects/SafeScreen/app/src/main/res/xml/accessibility_service_config.xml)
```xml
<accessibility-service xmlns:android="http://schemas.android.com/apk/res/android"
    android:accessibilityEventTypes="typeWindowStateChanged"
    android:accessibilityFeedbackType="feedbackGeneric"
    android:accessibilityFlags="flagDefault"
    android:canRetrieveWindowContent="false"
    android:notificationTimeout="100" />
```
* `accessibilityEventTypes="typeWindowStateChanged"`: The service is only awoken when the foreground window or activity changes.
* `canRetrieveWindowContent="false"`: **Critical privacy safeguard**. The service cannot inspect the UI hierarchy, read text, or access passwords.

---

## 5. Application Entry Point & Dependency Injection

SafeScreen avoids heavy reflection-based or annotation-processor-heavy dependency injection frameworks like Dagger or Hilt. Instead, it implements **Pure / Manual Dependency Injection** using the **AppContainer Pattern**.

### 5.1 Application Subclass: [`SafeScreenApp.kt`](file:///c:/Users/Mukashifa%20Fatima/AndroidStudioProjects/SafeScreen/app/src/main/java/com/example/safescreen/SafeScreenApp.kt)
* **Lines**: 1–25
* **Role**: The global process root.
```kotlin
class SafeScreenApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.notifier.createChannels()
    }
}
```
* **Key Mechanics**:
  1. `lateinit var container`: Initialized inside `onCreate()` before any Activity, Service, or BroadcastReceiver executes.
  2. `container.notifier.createChannels()`: Pre-registers Android notification channels (`CHANNEL_WARNINGS`) as required on Android 8.0+.
  3. Provides a clean extension property `val Context.appContainer get() = (applicationContext as SafeScreenApp).container` allowing any Android component to easily access shared dependencies.

### 5.2 Dependency Container: [`AppContainer.kt`](file:///c:/Users/Mukashifa%20Fatima/AndroidStudioProjects/SafeScreen/app/src/main/java/com/example/safescreen/AppContainer.kt)
* **Lines**: 1–45
* **Role**: Centralized service locator / constructor-injection container.
* **Component Graph**:
  * `database`: Lazy singleton `SafeScreenDatabase.getDatabase(context)`
  * `ruleRepository`: `AppRuleRepository(database.appRuleDao())`
  * `settingsRepository`: `SettingsRepository(context)`
  * `pinHasher`: `PinHasher()` (Pure crypto utility)
  * `pinManager`: `PinManager(settingsRepository, pinHasher)`
  * `sessionManager`: `SessionManager()` (Ephemeral 5-minute unlock timer)
  * `usageMonitor`: `UsageMonitor(context)`
  * `installedAppsProvider`: `InstalledAppsProvider(context)`
  * `notifier`: `Notifier(context)`
  * `permissionChecker`: `PermissionChecker(context)`

### 5.3 ViewModel Factory: [`ContainerViewModel.kt`](file:///c:/Users/Mukashifa%20Fatima/AndroidStudioProjects/SafeScreen/app/src/main/java/com/example/safescreen/ui/ContainerViewModel.kt)
* **Lines**: 1–35
* **Role**: Custom `ViewModelProvider.Factory` that bridges Android ViewModel creation with the manual `AppContainer`.
```kotlin
inline fun <reified VM : ViewModel> containerViewModel(
    crossinline creator: (AppContainer) -> VM
): @Composable () -> VM
```
* **Why this matters for beginners**: Normally, Android ViewModels cannot accept constructor arguments without a custom factory. `ContainerViewModel` uses Kotlin reified generics and inline functions to provide ViewModels with clean constructor-injected dependencies without boilerplate.

---

## 6. Data Layer: Room Database & DataStore Preferences

SafeScreen employs a dual persistence model:
1. **SQLite / Room Database**: Structured, relational data for app restrictions, schedules, and daily limits.
2. **AndroidX DataStore (Preferences)**: Key-value pair persistence for reactive settings, theme configuration, security hashes, and lockout timestamps.

### 6.1 Database Entity: [`AppRule.kt`](file:///c:/Users/Mukashifa%20Fatima/AndroidStudioProjects/SafeScreen/app/src/main/java/com/example/safescreen/data/AppRule.kt)
* **Lines**: 1–35
* Represents an enforcement rule tied to a specific Android application package.
```kotlin
@Entity(tableName = "app_rules")
data class AppRule(
    @PrimaryKey
    val packageName: String,
    val isBlocked: Boolean = false,
    val dailyLimitMinutes: Int? = null,
    val scheduleStartMinute: Int? = null,
    val scheduleEndMinute: Int? = null,
    val notes: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)
```
* **Data Model Details**:
  * `packageName`: Android application unique identifier (e.g., `com.google.android.youtube`), serving as the table's Primary Key.
  * `isBlocked`: Boolean flag for unconditional hard blocking.
  * `dailyLimitMinutes`: Nullable integer (null = unconstrained; e.g. 60 = 1 hour maximum daily foreground usage).
  * `scheduleStartMinute` / `scheduleEndMinute`: Minutes from midnight (0–1439). E.g., 22:30 is `22 * 60 + 30 = 1350`. Enables curfew windows across midnight.

### 6.2 Data Access Object: [`AppRuleDao.kt`](file:///c:/Users/Mukashifa%20Fatima/AndroidStudioProjects/SafeScreen/app/src/main/java/com/example/safescreen/data/AppRuleDao.kt)
* **Lines**: 1–30
* **Role**: Abstract interface defining SQLite queries compiled by KSP.
```kotlin
@Dao
interface AppRuleDao {
    @Query("SELECT * FROM app_rules ORDER BY packageName ASC")
    fun getAllRules(): Flow<List<AppRule>>

    @Query("SELECT * FROM app_rules WHERE packageName = :packageName LIMIT 1")
    suspend fun getRule(packageName: String): AppRule?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(rule: AppRule)

    @Delete
    suspend fun delete(rule: AppRule)

    @Query("DELETE FROM app_rules WHERE packageName = :packageName")
    suspend fun deleteByPackage(packageName: String)
}
```
* **Key Mechanics**:
  * `getAllRules()` returns a Kotlin `Flow<List<AppRule>>`. Whenever any row changes, Room emits the updated list to all active collectors automatically.
  * Suspend functions (`suspend fun`) ensure database operations run asynchronously off the Main thread.

### 6.3 Room Database Definition: [`SafeScreenDatabase.kt`](file:///c:/Users/Mukashifa%20Fatima/AndroidStudioProjects/SafeScreen/app/src/main/java/com/example/safescreen/data/SafeScreenDatabase.kt)
* **Lines**: 1–40
* Inherits from `RoomDatabase()`.
* Implements the Thread-safe Double-Checked Locking Singleton pattern:
```kotlin
@Database(entities = [AppRule::class], version = 1, exportSchema = false)
abstract class SafeScreenDatabase : RoomDatabase() {
    abstract fun appRuleDao(): AppRuleDao

    companion object {
        @Volatile
        private var INSTANCE: SafeScreenDatabase? = null

        fun getDatabase(context: Context): SafeScreenDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SafeScreenDatabase::class.java,
                    "safescreen_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
```

### 6.4 Repository Pattern: [`AppRuleRepository.kt`](file:///c:/Users/Mukashifa%20Fatima/AndroidStudioProjects/SafeScreen/app/src/main/java/com/example/safescreen/data/AppRuleRepository.kt)
* **Lines**: 1–25
* Mediates between the database DAO and higher application layers, providing a clean abstraction boundary.

### 6.5 DataStore Provider: [`DataStoreProvider.kt`](file:///c:/Users/Mukashifa%20Fatima/AndroidStudioProjects/SafeScreen/app/src/main/java/com/example/safescreen/data/DataStoreProvider.kt)
* **Lines**: 1–15
* Creates a single shared Preferences DataStore instance via the delegate:
  `val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "safescreen_settings")`

### 6.6 Preferences Repository: [`SettingsRepository.kt`](file:///c:/Users/Mukashifa%20Fatima/AndroidStudioProjects/SafeScreen/app/src/main/java/com/example/safescreen/data/SettingsRepository.kt)
* **Lines**: 1–110
* Manages app-wide reactive settings with type-safe preference keys:
  * `KEY_THEME_ID`: Selected GNOME Ptyxis palette ID (`cobalt_neon`, `nord`, etc.).
  * `KEY_PIN_HASH`: PBKDF2 hash of master PIN.
  * `KEY_PIN_SALT`: Base64 cryptographic salt.
  * `KEY_RECOVERY_HASH`: SHA-256 hash of emergency recovery code.
  * `KEY_FAILED_ATTEMPTS`: Consecutive failed PIN attempts.
  * `KEY_LOCKOUT_UNTIL`: Epoch timestamp until which PIN entry is disabled.
* Exposes properties as reactive Kotlin `Flow`s, emitting whenever preferences change on disk.

---

## 7. Security & Cryptography Subsystem

Parental control applications face an active adversary: a tech-savvy child attempting to guess PINs, clear data, or bypass restrictions. SafeScreen integrates industrial-grade cryptographic defenses.

### 7.1 PBKDF2 Password Hashing: [`PinHasher.kt`](file:///c:/Users/Mukashifa%20Fatima/AndroidStudioProjects/SafeScreen/app/src/main/java/com/example/safescreen/security/PinHasher.kt)
* **Lines**: 1–65
* **Algorithm**: `PBKDF2WithHmacSHA256`
* **Iterations**: `120,000` (meets OWASP password storage recommendations)
* **Salt**: 16-byte cryptographically secure random bytes via `SecureRandom`
* **Key Length**: 256 bits (32 bytes)
* **Timing-Attack Resistance**:
```kotlin
fun verifyPin(pin: String, saltBase64: String, expectedHashBase64: String): Boolean {
    val computedHash = hashPin(pin, saltBase64)
    return MessageDigest.isEqual(
        Base64.decode(computedHash, Base64.NO_WRAP),
        Base64.decode(expectedHashBase64, Base64.NO_WRAP)
    )
}
```
* `MessageDigest.isEqual` evaluates in **constant time**, eliminating side-channel timing attacks that allow attackers to deduce valid characters by measuring microsecond execution variances.

### 7.2 Tiered Rate-Limiting & Lockout: [`PinLockout.kt`](file:///c:/Users/Mukashifa%20Fatima/AndroidStudioProjects/SafeScreen/app/src/main/java/com/example/safescreen/security/PinLockout.kt)
* **Lines**: 1–45
* Protects against rapid PIN brute-forcing with progressive punishment tiers:
  * **Attempts 1–2**: 0 delay.
  * **Attempt 3**: 30-second penalty lockout.
  * **Attempt 4**: 60-second penalty lockout.
  * **Attempt 5**: 5-minute penalty lockout.
  * **Attempt 6+**: 30-minute penalty lockout.
* Provides pure function `calculateLockoutDuration(failedAttempts: Int): Long` allowing comprehensive unit test coverage without artificial system clock delays.

### 7.3 Recovery Token Generation: [`RecoveryCode.kt`](file:///c:/Users/Mukashifa%20Fatima/AndroidStudioProjects/SafeScreen/app/src/main/java/com/example/safescreen/security/RecoveryCode.kt)
* **Lines**: 1–50
* In the event that a parent forgets their PIN, SafeScreen allows account reset via a pre-generated Recovery Code.
* **Format**: Base32 Crockford-style uppercase alphanumeric string (e.g., `ABCD-EFGH-JKMN-PQRT`).
* The plaintext code is displayed once to the parent during setup; only its `SHA-256` digest is stored on disk.

### 7.4 Security Orchestrator: [`PinManager.kt`](file:///c:/Users/Mukashifa%20Fatima/AndroidStudioProjects/SafeScreen/app/src/main/java/com/example/safescreen/security/PinManager.kt)
* **Lines**: 1–95
* Coordinates between `SettingsRepository`, `PinHasher`, and `PinLockout`.
* Manages PIN lifecycle: `hasPinSet()`, `setupPin()`, `verifyPin()`, and `resetPinWithRecoveryCode()`.

### 7.5 Temporary Session Authorization: [`SessionManager.kt`](file:///c:/Users/Mukashifa%20Fatima/AndroidStudioProjects/SafeScreen/app/src/main/java/com/example/safescreen/security/SessionManager.kt)
* **Lines**: 1–40
* Avoids forcing parents to re-enter a 4-to-6 digit PIN on every single screen transition within the app.
* Holds an in-memory session token valid for **5 minutes** from the last authenticated action. The session automatically expires upon timeout or when explicitly cleared.

---

## 8. Monitoring & Usage Tracking Subsystem

Accurate screen time calculation without battery drain is one of Android development's toughest challenges. SafeScreen combines `UsageStatsManager` with an intelligent event-pairing algorithm.

### 8.1 Application Discovery: [`InstalledAppsProvider.kt`](file:///c:/Users/Mukashifa%20Fatima/AndroidStudioProjects/SafeScreen/app/src/main/java/com/example/safescreen/monitoring/InstalledAppsProvider.kt)
* **Lines**: 1–55
* Queries Android's `PackageManager` for all applications containing launchable main activities:
```kotlin
val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
val resolveInfos = packageManager.queryIntentActivities(intent, 0)
```
* **Filtering Logic**:
  * Strips out system packages without launchers.
  * Strips out SafeScreen itself (to prevent the user from locking themselves out of the management app).
  * Extracts user-friendly display labels and high-resolution drawable icons.

### 8.2 Runtime Permission Verification: [`PermissionChecker.kt`](file:///c:/Users/Mukashifa%20Fatima/AndroidStudioProjects/SafeScreen/app/src/main/java/com/example/safescreen/monitoring/PermissionChecker.kt)
* **Lines**: 1–60
* Verifies critical Android capabilities at runtime:
  * **Usage Access**: Checks `AppOpsManager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, ...)`
  * **Overlay Permission**: Checks `Settings.canDrawOverlays(context)`
  * **Accessibility Service**: Inspects `Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES` to verify that `BlockerAccessibilityService` is actively running.
  * **Notification Permission**: On Android 13 (TIRAMISU)+, verifies `android.permission.POST_NOTIFICATIONS`.

### 8.3 Usage Event Extraction: [`UsageMonitor.kt`](file:///c:/Users/Mukashifa%20Fatima/AndroidStudioProjects/SafeScreen/app/src/main/java/com/example/safescreen/monitoring/UsageMonitor.kt)
* **Lines**: 1–65
* Direct calls to `UsageStatsManager.queryUsageStats()` frequently return stale data on modern Android OS versions.
* SafeScreen instead calls `UsageStatsManager.queryEvents(startTime, endTime)` which provides an immutable stream of real-time lifecycle transitions:
  * `UsageEvents.Event.ACTIVITY_RESUMED` (App came to foreground)
  * `UsageEvents.Event.ACTIVITY_PAUSED` / `ACTIVITY_STOPPED` (App went to background)

### 8.4 Event-Pairing Algorithm: [`UsageSessions.kt`](file:///c:/Users/Mukashifa%20Fatima/AndroidStudioProjects/SafeScreen/app/src/main/java/com/example/safescreen/monitoring/UsageSessions.kt)
* **Lines**: 1–80
* Pure functional algorithm that takes raw, unordered timestamp events and pairs RESUMED and PAUSED events into concrete durations:
```kotlin
fun totals(
    events: List<UsageEventRecord>,
    startOfDay: Long,
    currentTime: Long
): Map<String, Long>
```
* **Edge-Case Resilience**:
  * Handles apps currently open in the foreground at evaluation time (computes duration from `lastResumed` to `currentTime`).
  * Discards duplicate resume events and anomalous timestamps.
  * Restricts calculation strictly to the current 24-hour cycle (from midnight `00:00:00` of the local timezone).

---

## 9. Domain Logic & Restriction Policy Engine

SafeScreen separates policy decisions from Android OS framework dependencies. The `domain/` package contains 100% pure Kotlin logic with zero Android imports, making it entirely testable in local JVM unit tests.

### 9.1 Decision Modeling: [`RestrictionDecision.kt`](file:///c:/Users/Mukashifa%20Fatima/AndroidStudioProjects/SafeScreen/app/src/main/java/com/example/safescreen/domain/RestrictionDecision.kt)
* **Lines**: 1–25
* Models policy outcomes as a Kotlin Sealed Class hierarchy:
```kotlin
sealed interface RestrictionDecision {
    data object Allowed : RestrictionDecision
    sealed interface Blocked : RestrictionDecision {
        data class AlwaysBlocked(val packageName: String) : Blocked
        data class DailyLimitExceeded(val packageName: String, val limitMinutes: Int, val usedMinutes: Int) : Blocked
        data class ScheduleActive(val packageName: String, val startMinute: Int, val endMinute: Int) : Blocked
    }
}
```
* Sealed classes allow comprehensive compile-time type-safety. When handling `RestrictionDecision`, the Kotlin compiler verifies that every possible reason is accounted for in `when` statements.

### 9.2 Policy Evaluation Engine: [`RestrictionPolicy.kt`](file:///c:/Users/Mukashifa%20Fatima/AndroidStudioProjects/SafeScreen/app/src/main/java/com/example/safescreen/domain/RestrictionPolicy.kt)
* **Lines**: 1–55
* Evaluates active rules against current usage and system clock:
```kotlin
fun evaluate(
    rule: AppRule?,
    usageMinutesToday: Int,
    currentMinuteOfDay: Int
): RestrictionDecision
```
* **Evaluation Priority Order**:
  1. **Rule Existence**: If `rule == null`, returns `Allowed`.
  2. **Hard Block**: If `rule.isBlocked == true`, returns `Blocked.AlwaysBlocked`.
  3. **Daily Limit**: If `rule.dailyLimitMinutes != null && usageMinutesToday >= rule.dailyLimitMinutes`, returns `Blocked.DailyLimitExceeded`.
  4. **Curfew Schedule**: If `rule.scheduleStartMinute != null && rule.scheduleEndMinute != null`, evaluates whether `currentMinuteOfDay` falls within the active window (supporting both standard same-day schedules like `09:00–17:00` and midnight-crossing schedules like `22:00–06:00`).
  5. **Default**: Returns `Allowed`.

### 9.3 Usage Warning Thresholds: [`UsageWarnings.kt`](file:///c:/Users/Mukashifa%20Fatima/AndroidStudioProjects/SafeScreen/app/src/main/java/com/example/safescreen/domain/UsageWarnings.kt)
* **Lines**: 1–35
* Computes whether an application has reached the **80% daily quota threshold**:
```kotlin
fun shouldWarn(usageMinutes: Int, limitMinutes: Int?): Boolean {
    if (limitMinutes == null || limitMinutes <= 0) return false
    val threshold = (limitMinutes * 0.8f).toInt()
    return usageMinutes >= threshold && usageMinutes < limitMinutes
}
```
* This proactive warning allows users or children to gracefully wrap up their activity before a hard blocking barrier triggers.

---

## 10. Enforcement & Accessibility Overlay Subsystem

When an application violates a policy, SafeScreen must intervene instantly and reliably to prevent access.

### 10.1 Event Interceptor: [`BlockerAccessibilityService.kt`](file:///c:/Users/Mukashifa%20Fatima/AndroidStudioProjects/SafeScreen/app/src/main/java/com/example/safescreen/enforcement/BlockerAccessibilityService.kt)
* **Lines**: 1–110
* Inherits from `android.accessibilityservice.AccessibilityService`.
* Listens to `AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED`.
* **Execution Flow**:
  1. Extracts target package name: `val packageName = event.packageName?.toString() ?: return`
  2. Ignores system UI, launcher apps, and SafeScreen's own packages.
  3. Queries in-memory cache of `AppRule`s (synced reactively from Room DB via coroutine collector).
  4. Queries today's foreground minutes from `UsageMonitor`.
  5. Evaluates `RestrictionPolicy.evaluate(rule, usedMinutes, currentMinute)`.
  6. If `RestrictionDecision.Blocked`: Launches `BlockedActivity`.

### 10.2 Immersive Barrier Activity: [`BlockedActivity.kt`](file:///c:/Users/Mukashifa%20Fatima/AndroidStudioProjects/SafeScreen/app/src/main/java/com/example/safescreen/enforcement/BlockedActivity.kt)
* **Lines**: 1–95
* Fullscreen blocking activity launched with flags:
  `FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TOP or FLAG_ACTIVITY_SINGLE_TOP`
* **Defensive UI Properties**:
  * Prevents back navigation into the restricted application.
  * Displays the specific restriction reason (e.g., *"Daily limit of 60 minutes reached"* or *"Curfew active until 7:00 AM"*).
  * Provides a **Parent Override** button: prompts for master PIN. If validated, grants a 5-minute override session.
  * Provides an **Exit / Home** button: returns user to the Android home launcher via `Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)`.

### 10.3 Notification Dispatcher: [`Notifier.kt`](file:///c:/Users/Mukashifa%20Fatima/AndroidStudioProjects/SafeScreen/app/src/main/java/com/example/safescreen/notifications/Notifier.kt)
* **Lines**: 1–70
* Wraps Android's `NotificationManagerCompat`.
* Manages `NotificationChannel` with ID `safescreen_warnings`.
* Dispatches heads-up warnings with custom icons ([`ic_notification.xml`](file:///c:/Users/Mukashifa%20Fatima/AndroidStudioProjects/SafeScreen/app/src/main/res/drawable/ic_notification.xml)).

---

## 11. User Interface & Jetpack Compose Deep-Dive

SafeScreen's presentation layer is 100% declarative, implemented using **Jetpack Compose** with AndroidX Navigation and Material 3 design tokens.

### 11.1 Presentation Entry & Host: [`MainActivity.kt`](file:///c:/Users/Mukashifa%20Fatima/AndroidStudioProjects/SafeScreen/app/src/main/java/com/example/safescreen/MainActivity.kt)
* **Lines**: 1–45
* Inherits from `ComponentActivity()`.
* Activates modern edge-to-edge rendering via `enableEdgeToEdge()`.
* Binds the application to the root composable:
```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
        SafeScreenRoot(container = appContainer)
    }
}
```

### 11.2 Root Navigation & Authentication Gate: [`SafeScreenRoot.kt`](file:///c:/Users/Mukashifa%20Fatima/AndroidStudioProjects/SafeScreen/app/src/main/java/com/example/safescreen/ui/SafeScreenRoot.kt)
* **Lines**: 1–85
* **Role**: The state router and theme wrapper for the entire user interface.
* **Authentication Gate**:
  * Collects `settingsRepository.themeIdFlow` and applies the dynamic `SafeScreenTheme(paletteId = ...)`.
  * Checks if the master PIN is configured. If configured and no active session exists, displays `PinScreen`.
  * Once unlocked (or if no PIN is configured yet), renders the main dashboard via `MainScaffold`.

### 11.3 Main Scaffold & Tab Navigation: [`MainScaffold.kt`](file:///c:/Users/Mukashifa%20Fatima/AndroidStudioProjects/SafeScreen/app/src/main/java/com/example/safescreen/ui/MainScaffold.kt)
* **Lines**: 1–80
* Houses Material 3 `NavigationBar` and `NavigationBarItem`s for the 3 primary tabs:
  1. **Home** (`Icons.Default.Home`): Screen time dashboard.
  2. **Apps** (`Icons.Default.Apps`): Rule configuration and application limits.
  3. **Settings** (`Icons.Default.Settings`): Theme customization, PIN security, and diagnostic permissions.
* Manages tab state through a hoisted integer or enum with animated screen transitions.

### 11.4 Dashboard Screen & ViewModel: [`HomeScreen.kt`](file:///c:/Users/Mukashifa%20Fatima/AndroidStudioProjects/SafeScreen/app/src/main/java/com/example/safescreen/ui/home/HomeScreen.kt) & [`HomeViewModel.kt`](file:///c:/Users/Mukashifa%20Fatima/AndroidStudioProjects/SafeScreen/app/src/main/java/com/example/safescreen/ui/home/HomeViewModel.kt)
* **Lines**: `HomeScreen.kt` (1–150), `HomeViewModel.kt` (1–90)
* **UI Features**:
  * **Today's Total Screen Time**: Prominent hero banner displaying total hours and minutes.
  * **Active Blocks Count**: Quick badges showing how many apps are currently hard-blocked.
  * **Top Used Apps List**: Ranked breakdown of foreground time per application with percentage progress bars.
  * **Active Warning Cards**: Highlights apps nearing their 80% daily threshold.
* **ViewModel Implementation**:
  * Gathers data from `UsageMonitor.getTodayUsage()` and `AppRuleRepository.allRulesFlow`.
  * Merges them into a unified `HomeUiState` data class exposed via `StateFlow<HomeUiState>`.

### 11.5 Rules Management: [`AppsScreen.kt`](file:///c:/Users/Mukashifa%20Fatima/AndroidStudioProjects/SafeScreen/app/src/main/java/com/example/safescreen/ui/apps/AppsScreen.kt) & [`AppsViewModel.kt`](file:///c:/Users/Mukashifa%20Fatima/AndroidStudioProjects/SafeScreen/app/src/main/java/com/example/safescreen/ui/apps/AppsViewModel.kt)
* **Lines**: `AppsScreen.kt` (1–220), `AppsViewModel.kt` (1–130)
* **UI Features**:
  * Search field to filter installed apps by name or package.
  * Filter chips: "All", "Restricted", "Blocked", "With Limit".
  * **Rule Editor Dialog**:
    * Toggle: "Hard Block App".
    * Slider / Number Input: "Daily Time Limit (minutes)".
    * Time Pickers: "Curfew Start (e.g., 22:00)" & "Curfew End (e.g., 07:00)".
* **ViewModel Actions**:
  * Dispatches `saveRule(rule)` or `deleteRule(packageName)` into Room asynchronously through coroutine scope.

### 11.6 Settings & Diagnostics: [`SettingsScreen.kt`](file:///c:/Users/Mukashifa%20Fatima/AndroidStudioProjects/SafeScreen/app/src/main/java/com/example/safescreen/ui/settings/SettingsScreen.kt) & [`SettingsViewModel.kt`](file:///c:/Users/Mukashifa%20Fatima/AndroidStudioProjects/SafeScreen/app/src/main/java/com/example/safescreen/ui/settings/SettingsViewModel.kt)
* **Lines**: `SettingsScreen.kt` (1–180), `SettingsViewModel.kt` (1–110)
* **Configuration Sections**:
  * **Theme Picker**: Real-time switching between Cobalt Neon, Nord, Synthwave, and Fairy Floss palettes.
  * **Security Management**: Change PIN, View/Regenerate Recovery Code.
  * **Permissions Health Check**: Status indicators showing whether Usage Access and Accessibility are active, with deep-link buttons into Android system settings.
  * **Emergency Reset**: Option to clear all restrictions (protected by master PIN verification).

### 11.7 Security Keypad: [`PinScreen.kt`](file:///c:/Users/Mukashifa%20Fatima/AndroidStudioProjects/SafeScreen/app/src/main/java/com/example/safescreen/ui/pin/PinScreen.kt) & [`PinViewModel.kt`](file:///c:/Users/Mukashifa%20Fatima/AndroidStudioProjects/SafeScreen/app/src/main/java/com/example/safescreen/ui/pin/PinViewModel.kt)
* **Lines**: `PinScreen.kt` (1–190), `PinViewModel.kt` (1–120)
* **Keypad Features**:
  * Secure numeric keypad (0–9, backspace, clear).
  * Obfuscated dot indicators for entered digits.
  * Shake animation on incorrect PIN entry.
  * Countdown lockout timer if rate-limited.
  * "Forgot PIN" link opening recovery flows.

### 11.8 Recovery Dialogs: [`ForgotPinDialog.kt`](file:///c:/Users/Mukashifa%20Fatima/AndroidStudioProjects/SafeScreen/app/src/main/java/com/example/safescreen/ui/pin/ForgotPinDialog.kt) & [`RecoveryCodeDialog.kt`](file:///c:/Users/Mukashifa%20Fatima/AndroidStudioProjects/SafeScreen/app/src/main/java/com/example/safescreen/ui/pin/RecoveryCodeDialog.kt)
* **Lines**: 1–90 each
* Guides user through emergency recovery: entering the 16-character alphanumeric code to wipe the PIN and set a new one.

### 11.9 Restriction Overlay Previews: [`RestrictionContent.kt`](file:///c:/Users/Mukashifa%20Fatima/AndroidStudioProjects/SafeScreen/app/src/main/java/com/example/safescreen/ui/restriction/RestrictionContent.kt) & [`RestrictionPreviewScreen.kt`](file:///c:/Users/Mukashifa%20Fatima/AndroidStudioProjects/SafeScreen/app/src/main/java/com/example/safescreen/ui/restriction/RestrictionPreviewScreen.kt)
* **Lines**: 1–110 each
* Renders the shared barrier UI used by both `BlockedActivity` and the in-app restriction preview. Shows shield icons, restriction explanations, and action buttons.

### 11.10 Guided Onboarding: [`PermissionSetupDialog.kt`](file:///c:/Users/Mukashifa%20Fatima/AndroidStudioProjects/SafeScreen/app/src/main/java/com/example/safescreen/ui/setup/PermissionSetupDialog.kt)
* **Lines**: 1–110
* Modal dialog shown on first launch guiding the parent step-by-step through enabling Accessibility and Usage Access with explicit intent actions:
  * `Settings.ACTION_USAGE_ACCESS_SETTINGS`
  * `Settings.ACTION_ACCESSIBILITY_SETTINGS`

### 11.11 Ptyxis Theme Engine: [`PtyxisPalette.kt`](file:///c:/Users/Mukashifa%20Fatima/AndroidStudioProjects/SafeScreen/app/src/main/java/com/example/safescreen/ui/theme/PtyxisPalette.kt), [`Theme.kt`](file:///c:/Users/Mukashifa%20Fatima/AndroidStudioProjects/SafeScreen/app/src/main/java/com/example/safescreen/ui/theme/Theme.kt), [`Type.kt`](file:///c:/Users/Mukashifa%20Fatima/AndroidStudioProjects/SafeScreen/app/src/main/java/com/example/safescreen/ui/theme/Type.kt)
* **Lines**: `PtyxisPalette.kt` (1–130), `Theme.kt` (1–80), `Type.kt` (1–35)
* Implements 4 curated dark palettes:
  1. **Cobalt Neon**: Deep navy background (`#0B1021`), bright cyan accent (`#00F5D4`), electric pink alert (`#FF3366`).
  2. **Nord**: Arctic slate background (`#2E3440`), frosty cyan (`#88C0D0`), soft green (`#A3BE8C`).
  3. **Synthwave**: Cyberpunk dark purple (`#1A0B2E`), neon magenta (`#FF007F`), vibrant amber (`#FFD166`).
  4. **Fairy Floss**: Midnight pastel indigo (`#241E38`), soft pink (`#F0A6CA`), lilac (`#C792EA`).
* Translates palettes into Material 3 `ColorScheme` instances.

### 11.12 Common Components & Formatters: [`Components.kt`](file:///c:/Users/Mukashifa%20Fatima/AndroidStudioProjects/SafeScreen/app/src/main/java/com/example/safescreen/ui/components/Components.kt) & [`Format.kt`](file:///c:/Users/Mukashifa%20Fatima/AndroidStudioProjects/SafeScreen/app/src/main/java/com/example/safescreen/ui/components/Format.kt)
* **Lines**: `Components.kt` (1–120), `Format.kt` (1–50)
* Reusable Material 3 cards, badges, and headers.
* `Format.kt`: Pure formatting utilities for human-readable durations (e.g., converting 3665000 milliseconds to `"1h 1m"`) and time-of-day strings (e.g., converting 1350 minutes to `"22:30"`).

---

## 12. Kotlin Language Concepts Grounded in SafeScreen

For developers new to Kotlin, SafeScreen serves as a textbook example of modern, idiomatic Kotlin design patterns.

### 12.1 Data Classes
* **Example**: [`AppRule.kt`](file:///c:/Users/Mukashifa%20Fatima/AndroidStudioProjects/SafeScreen/app/src/main/java/com/example/safescreen/data/AppRule.kt#L10)
```kotlin
data class AppRule(
    val packageName: String,
    val isBlocked: Boolean = false,
    val dailyLimitMinutes: Int? = null,
    ...
)
```
* **Explanation**: Prepending `data` instructs the Kotlin compiler to automatically generate `equals()`, `hashCode()`, `toString()`, `componentN()`, and `copy()`. Using `copy()` allows immutable state mutations: `rule.copy(isBlocked = true)`.

### 12.2 Sealed Interfaces and Exhaustive When
* **Example**: [`RestrictionDecision.kt`](file:///c:/Users/Mukashifa%20Fatima/AndroidStudioProjects/SafeScreen/app/src/main/java/com/example/safescreen/domain/RestrictionDecision.kt#L5)
```kotlin
sealed interface RestrictionDecision {
    data object Allowed : RestrictionDecision
    sealed interface Blocked : RestrictionDecision { ... }
}
```
* **Explanation**: Sealed hierarchies restrict subclassing exclusively to the same file. In `when (decision)` statements, the Kotlin compiler enforces exhaustiveness. If a developer adds a new decision type later, the compiler immediately flags all missing branches across the codebase as compile errors.

### 12.3 Extension Functions and Extension Properties
* **Example**: [`SafeScreenApp.kt`](file:///c:/Users/Mukashifa%20Fatima/AndroidStudioProjects/SafeScreen/app/src/main/java/com/example/safescreen/SafeScreenApp.kt#L20)
```kotlin
val Context.appContainer: AppContainer
    get() = (applicationContext as SafeScreenApp).container
```
* **Explanation**: Kotlin allows developers to extend existing classes (even Android framework classes like `Context`) with new methods and properties without subclassing or using static utility classes.

### 12.4 Kotlin Coroutines and Reactive Flows
* **Example**: Room DAOs and StateFlow in ViewModels
* **Concepts**:
  * `suspend fun`: A function that can pause execution without blocking the underlying thread, resuming when the asynchronous work completes.
  * `Flow<T>`: An asynchronous, cold stream of data that emits multiple values sequentially over time.
  * `StateFlow<T>`: A hot, state-holding observable flow that always holds the latest state and replays it to new collectors.
  * `viewModelScope`: A coroutine scope bound to the ViewModel lifecycle. Coroutines launched here are automatically cancelled when the ViewModel is cleared, eliminating memory leaks.

### 12.5 Property Delegation (`by`)
* **Example**: [`DataStoreProvider.kt`](file:///c:/Users/Mukashifa%20Fatima/AndroidStudioProjects/SafeScreen/app/src/main/java/com/example/safescreen/data/DataStoreProvider.kt#L10)
```kotlin
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "safescreen_settings")
```
* **Explanation**: The `by` keyword delegates getter and setter logic to a delegate class, abstracting complex initialization and lifecycle management.

### 12.6 Null Safety Operators
* **Example**: Safe calls (`?.`) and Elvis operator (`?:`) in [`BlockerAccessibilityService.kt`](file:///c:/Users/Mukashifa%20Fatima/AndroidStudioProjects/SafeScreen/app/src/main/java/com/example/safescreen/enforcement/BlockerAccessibilityService.kt#L45)
```kotlin
val packageName = event.packageName?.toString() ?: return
```
* **Explanation**: Kotlin types are non-nullable by default. If a variable can be null, it must be declared with `?`. The Elvis operator `?:` provides a fallback value or exits early (`return`) if the expression on the left resolves to `null`.

---

## 13. Android Operating System & Jetpack Compose Fundamentals

### 13.1 The Activity Lifecycle in Single-Activity Compose
In classic Android, each screen was a separate `Activity` interacting through XML layouts and lifecycle callbacks (`onCreate`, `onStart`, `onResume`, `onPause`, `onStop`, `onDestroy`).

SafeScreen adopts the **Single-Activity Architecture**:
* `MainActivity` is the sole persistent OS window container.
* Screen navigation (Home, Apps, Settings) happens entirely inside the Compose hierarchy without destroying or creating Activities.
* The only secondary Activity is `BlockedActivity`, intentionally kept as an independent Activity so it can be launched over any arbitrary third-party application using `FLAG_ACTIVITY_NEW_TASK`.

### 13.2 Jetpack Compose Declarative UI Paradigm
Unlike imperative Android UI (where developers find views via `findViewById` and mutate them with `textView.setText(...)`), Compose is **declarative**:
1. UI is defined as pure functions annotated with `@Composable`.
2. When underlying state changes, Compose automatically recalculates and redraws only the specific composable functions affected—a process called **Recomposition**.
3. **State Hoisting**: State moves up, events move down. Screens do not own business state; they receive state from ViewModels and emit event callbacks (e.g., `onSaveRule: (AppRule) -> Unit`).

### 13.3 Key Compose Primitives
* `remember { mutableStateOf(value) }`: Preserves local state across recompositions.
* `LaunchedEffect(key) { ... }`: Safely launches coroutines tied to the lifecycle of a composable. Whenever `key` changes, the existing job is cancelled and restarted.
* `collectAsStateWithLifecycle()`: Safely observes `StateFlow` from ViewModels, pausing emissions when the Android app moves to the background to conserve battery and CPU resources.

---

## 14. Test Suite Audit & Verification Strategy

SafeScreen contains **46 unit tests across 9 test files** located in `app/src/test/java/com/example/safescreen/`.

### 14.1 Test Suite Inventory Table

| Test File | Target Under Test | Test Count | Key Test Assertions |
|:---|:---|:---:|:---|
| [`domain/RestrictionPolicyTest.kt`](file:///c:/Users/Mukashifa%20Fatima/AndroidStudioProjects/SafeScreen/app/src/test/java/com/example/safescreen/domain/RestrictionPolicyTest.kt) | `RestrictionPolicy` | 8 | Evaluates hard-blocks, minute limit limits, curfew boundaries, and midnight crossovers. |
| [`domain/UsageWarningsTest.kt`](file:///c:/Users/Mukashifa%20Fatima/AndroidStudioProjects/SafeScreen/app/src/test/java/com/example/safescreen/domain/UsageWarningsTest.kt) | `UsageWarnings` | 5 | Verifies 80% threshold calculation, exact limits, and null limit edge cases. |
| [`monitoring/UsageSessionsTest.kt`](file:///c:/Users/Mukashifa%20Fatima/AndroidStudioProjects/SafeScreen/app/src/test/java/com/example/safescreen/monitoring/UsageSessionsTest.kt) | `UsageSessions` | 6 | Validates event-pairing, currently foreground apps, midnight boundary clipping. |
| [`security/PinHasherTest.kt`](file:///c:/Users/Mukashifa%20Fatima/AndroidStudioProjects/SafeScreen/app/src/test/java/com/example/safescreen/security/PinHasherTest.kt) | `PinHasher` | 6 | Tests PBKDF2 hash reproducibility, salt uniqueness, timing-safe verification. |
| [`security/PinLockoutTest.kt`](file:///c:/Users/Mukashifa%20Fatima/AndroidStudioProjects/SafeScreen/app/src/test/java/com/example/safescreen/security/PinLockoutTest.kt) | `PinLockout` | 7 | Tests exponential backoff duration, tier steps (0s, 30s, 60s, 5m, 30m). |
| [`security/RecoveryCodeTest.kt`](file:///c:/Users/Mukashifa%20Fatima/AndroidStudioProjects/SafeScreen/app/src/test/java/com/example/safescreen/security/RecoveryCodeTest.kt) | `RecoveryCode` | 5 | Tests Base32 entropy, formatting delimiters, SHA-256 digest validation. |
| [`security/SessionManagerTest.kt`](file:///c:/Users/Mukashifa%20Fatima/AndroidStudioProjects/SafeScreen/app/src/test/java/com/example/safescreen/security/SessionManagerTest.kt) | `SessionManager` | 4 | Tests 5-minute timeout expiration, token validation, manual invalidation. |
| [`ui/FormatTest.kt`](file:///c:/Users/Mukashifa%20Fatima/AndroidStudioProjects/SafeScreen/app/src/test/java/com/example/safescreen/ui/FormatTest.kt) | `Format.kt` | 4 | Tests millisecond to `"Xh Ym"` conversions and military time formatting. |
| [`ExampleUnitTest.kt`](file:///c:/Users/Mukashifa%20Fatima/AndroidStudioProjects/SafeScreen/app/src/test/java/com/example/safescreen/ExampleUnitTest.kt) | Baseline Check | 1 | Basic arithmetic test verifying JUnit runner health. |

### 14.2 Pure JVM Testing Architecture
* All 46 tests run directly on the host machine JVM via `./gradlew test` in **less than 2 seconds**.
* None of the business or security logic requires Robolectric, emulator boots, or Android device hardware because the domain and security models avoid Android framework classes (`android.content.Context`, `android.os.Handler`, etc.).

---

## 15. Security Audit, Edge Cases & Technical Critique

### 15.1 Architectural Strengths
1. **Zero External Attack Surface**: No open ports, no HTTP endpoints, no Firebase or cloud analytics dependencies.
2. **OWASP-Compliant Key Derivation**: PBKDF2 with 120,000 iterations and constant-time string comparisons provides defense against local extraction and cracking.
3. **High Privacy Posture**: Accessibility configuration explicitly disables window content retrieval (`canRetrieveWindowContent = false`).
4. **Resilient Data Architecture**: Reactive Room Flow queries guarantee that user interface updates never desynchronize from disk state.

### 15.2 Real-World Edge Cases & Potential Vulnerabilities
1. **OEM Background Aggression**: Aggressive vendor task killers (e.g., Xiaomi MIUI, Samsung OneUI) can stop Accessibility Services when the device enters deep sleep.
   * *Mitigation Recommendation*: Prompt users to disable battery optimization (`ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`) during setup.
2. **UsageStats Reporting Latency**: `UsageStatsManager` events can occasionally lag by a few seconds on low-memory devices.
   * *Mitigation Recommendation*: Cache the exact moment an app is opened and count real-time foreground seconds locally.
3. **System Time Tampering**: A tech-savvy child could theoretically roll back the Android system clock to bypass daily limits.
   * *Mitigation Recommendation*: Cross-reference `System.currentTimeMillis()` with `SystemClock.elapsedRealtime()` (which measures time elapsed since boot and cannot be altered by changing system time).
4. **Safe Mode Boot**: Booting Android into Safe Mode disables all third-party Accessibility Services.
   * *Analysis*: This is an Android OS limitation common to all non-MDM parental control apps. Enterprise Device Owner (MDM) profile enrollment would be required to prevent Safe Mode boots.

---

## 16. Developer Roadmap, Technical Glossary & Interview Prep

### 16.1 Junior to Senior Learning Roadmap
1. **Stage 1 (Kotlin Fundamentals)**: Master immutability (`val` vs `var`), data classes, sealed interfaces, extension functions, and nullability.
2. **Stage 2 (Jetpack Compose & Architecture)**: Learn declarative UI, State hoisting, ViewModels, and Unidirectional Data Flow.
3. **Stage 3 (Asynchronous Programming)**: Master Kotlin Coroutines (`suspend`, `CoroutineScope`, `Dispatchers`), Flow (`StateFlow`, `SharedFlow`), and Room database interactions.
4. **Stage 4 (System Services & Security)**: Study Android permissions, `AccessibilityService`, `UsageStatsManager`, cryptographic key derivation (PBKDF2), and constant-time comparisons.

### 16.2 Technical Glossary
* **AccessibilityService**: An Android background service designed to assist users with disabilities, frequently utilized by security and parental control tools to monitor window transitions.
* **AppContainer**: A lightweight pattern where dependencies are instantiated and held as properties in a single container class, avoiding the complexity of DI frameworks.
* **BOM (Bill of Materials)**: A Gradle dependency mechanism that aligns compatible versions of multiple related libraries (e.g., Jetpack Compose).
* **DataStore**: Modern replacement for `SharedPreferences`, supporting asynchronous operations, coroutines, and transactional consistency.
* **KSP (Kotlin Symbol Processing)**: A fast, lightweight annotation processing tool built specifically for Kotlin (used by Room to generate code at compile time).
* **PBKDF2**: Password-Based Key Derivation Function 2; a key-stretching cryptographic algorithm that applies pseudo-random functions repeatedly to resist brute-force attacks.
* **Room**: Google's official object-relational mapping (ORM) library built on top of SQLite for Android.
* **UDF (Unidirectional Data Flow)**: An architecture pattern where state flows downward and events flow upward, ensuring predictable UI behavior.

### 16.3 15 Architectural Interview Questions & Answers

#### Q1: Why does SafeScreen use manual dependency injection (`AppContainer`) instead of Hilt or Dagger?
> **Answer**: SafeScreen is a focused, single-module application. Manual dependency injection provides full control over object creation, zero compile-time annotation overhead, faster build times, and makes it trivial for beginners to trace the lifecycle and wiring of every dependency without magical generated code.

#### Q2: How does `BlockerAccessibilityService` respect user privacy?
> **Answer**: In `res/xml/accessibility_service_config.xml`, `canRetrieveWindowContent` is explicitly set to `false`, and `accessibilityEventTypes` is restricted to `typeWindowStateChanged`. This guarantees the service can only read the package name of the active window, and cannot read keystrokes, passwords, or on-screen text.

#### Q3: What is the difference between `Flow` and `StateFlow` in SafeScreen?
> **Answer**: `Flow` (such as Room's `getAllRules()`) is a cold stream that starts emitting only when a collector attaches. `StateFlow` (used in ViewModels) is a hot stream that always retains the latest state value and immediately replays it to new collectors upon UI recomposition.

#### Q4: Why does SafeScreen query `UsageStatsManager.queryEvents` instead of `queryUsageStats`?
> **Answer**: `queryUsageStats` returns aggregated summaries that are frequently cached and delayed by the Android OS. `queryEvents` returns the raw, un-aggregated event log (`ACTIVITY_RESUMED`, `ACTIVITY_STOPPED`), allowing `UsageSessions.totals()` to calculate real-time foreground time down to the exact millisecond.

#### Q5: Why is PBKDF2 with 120,000 iterations used instead of a single SHA-256 hash for PIN security?
> **Answer**: A 4-digit PIN has only 10,000 combinations, which can be brute-forced in milliseconds against a fast hash like SHA-256. PBKDF2 introduces key-stretching by executing 120,000 iterations of HMAC-SHA256, dramatically increasing the computation cost per guess and rendering offline brute-force attacks infeasible.

#### Q6: Why does `verifyPin` use `MessageDigest.isEqual` instead of `==`?
> **Answer**: The standard `==` operator compares strings byte-by-byte and returns `false` as soon as the first mismatched byte is encountered. This allows an attacker with a high-precision timer to deduce valid characters based on response time variances (timing attack). `MessageDigest.isEqual` always takes constant time regardless of where the mismatch occurs.

#### Q7: How does SafeScreen evaluate curfew schedules that cross midnight (e.g., 22:00 to 06:00)?
> **Answer**: In `RestrictionPolicy.kt`, if `startMinute > endMinute`, the schedule crosses midnight. An app is restricted if `currentMinute >= startMinute` (before midnight) OR `currentMinute < endMinute` (after midnight).

#### Q8: What prevents `BlockedActivity` from being bypassed using the Android Back button?
> **Answer**: `BlockedActivity` intercepts back navigation and immediately redirects the user to the Android Home screen (`Intent.CATEGORY_HOME`), preventing them from popping the back stack into the restricted foreground application.

#### Q9: What is the purpose of `SessionManager` in SafeScreen?
> **Answer**: `SessionManager` provides a 5-minute authenticated session window upon successful PIN entry. This prevents parental fatigue by allowing parents to adjust multiple rules or settings without re-entering their PIN on every single screen.

#### Q10: How does `ContainerViewModel` enable ViewModels to accept custom constructor parameters?
> **Answer**: Android's default ViewModel provider requires a no-argument constructor. `ContainerViewModel` uses an inline function with a custom `ViewModelProvider.Factory` that passes `appContainer` into the ViewModel's constructor.

#### Q11: What is the advantage of using Kotlin Sealed Interfaces for `RestrictionDecision`?
> **Answer**: Sealed interfaces guarantee compile-time exhaustiveness. Any `when` expression handling a `RestrictionDecision` must account for `Allowed`, `AlwaysBlocked`, `DailyLimitExceeded`, and `ScheduleActive`. If a developer adds a new state, compilation fails until all branches handle it.

#### Q12: Why are all 46 unit tests in SafeScreen pure JVM tests?
> **Answer**: The domain, security, and calculation modules (`RestrictionPolicy`, `PinHasher`, `UsageSessions`, `PinLockout`) contain zero Android framework dependencies. As a result, the entire test suite runs on the local JVM in seconds without booting an Android emulator.

#### Q13: How does `SafeScreenDatabase` prevent database corruption in multithreaded environments?
> **Answer**: It implements the Double-Checked Locking Singleton pattern with a `@Volatile` instance variable and a `synchronized(this)` block, ensuring only one instance of the SQLite database connection is created across all threads.

#### Q14: How does SafeScreen handle Android 13+ Notification permission changes?
> **Answer**: SafeScreen checks for `android.permission.POST_NOTIFICATIONS` at runtime using `PermissionChecker.kt` before dispatching 80% quota warning notifications, ensuring compliance with Android 13 (API 33) privacy requirements.

#### Q15: How does the Ptyxis theme engine apply custom themes dynamically in Compose?
> **Answer**: `SettingsRepository` stores the active palette ID in DataStore. `SafeScreenRoot` collects this flow reactively, resolves the corresponding `PtyxisPalette` (`Cobalt Neon`, `Nord`, etc.), maps it to a Material 3 `ColorScheme`, and passes it into `MaterialTheme`, triggering instant app-wide UI restyling without activity restarts.

---

## 17. Conclusion & Document Validation

This technical guide represents an exhaustive, evidence-based architectural audit of the entire SafeScreen repository. Every layer—from Gradle build configuration, cryptographic security, and system accessibility hooks to Room database persistence, domain policies, and Jetpack Compose interfaces—has been audited and documented with exact source code references.

* **Documentation File**: `docs/SAFESCREEN_COMPLETE_TECHNICAL_GUIDE.md`
* **Verified Coverage**: All 34 Kotlin production source files, 9 unit test files, build configurations, and AndroidManifest resources.
