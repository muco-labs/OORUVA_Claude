# OORUVA

Android app for discovering street vendors around you — chai stalls, samosa carts,
juice corners, mobile repair, salons. Kotlin + Jetpack Compose, Material 3, mock data.

## Screens (10)

| Screen | Route | What it does |
|---|---|---|
| Auth | `auth` | Phone + OTP (mock: any 10-digit phone, any 6-digit OTP) |
| Home | `home` | Vendor feed with search, ratings, distance |
| Business Detail | `business_detail/{vendorId}` | Info cards, check-in, community posts |
| Community | `community` | Post feed with likes, comments, share |
| Group Finder | `group_finder` | Headcount + budget → per-person cost and matching stalls |
| Map | `map` | Nearby vendor list (Google Maps key not wired — see below) |
| Rewards | `rewards` | Points dashboard, redemption, activity |
| Vendor Portal | `vendor_portal` | Business management preview + stats |
| Admin Dashboard | `admin_dashboard` | System metrics, pending actions, activity log |
| Profile | `profile` | User stats and settings list |

Bottom navigation: Home · Groups · Map · Community · Rewards · Profile.
Vendor Portal and Admin Dashboard are in the top-bar overflow menu.

## Toolchain

Pinned to what this machine can actually run — the only JDK present is the
JetBrains Runtime **25** bundled with Android Studio, which rules out the older
Gradle/AGP versions in the original spec.

| Component | Version |
|---|---|
| Gradle | 9.7.0 |
| Android Gradle Plugin | 9.3.1 |
| Kotlin | 2.4.10 |
| Compose BOM | 2026.08.00 |
| compileSdk / targetSdk | 36 |
| minSdk | 26 |
| Java / Kotlin JVM target | 17 |

`local.properties` points at `C:/Users/ELCOT/AppData/Local/Android/Sdk`. AGP will
download the API 36 platform on first build (only `android-37.0` is installed locally).

## Build

```bash
./gradlew assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

Install:

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

`adb` lives at `C:/Users/ELCOT/AppData/Local/Android/Sdk/platform-tools/adb.exe`.

### Two machine constraints worth knowing

**Memory.** This box has 7.4 GB RAM and the build needs roughly 2–3 GB free.
`gradle.properties` is already tuned down (`-Xmx900m` for the Gradle daemon,
`-Xmx768m` for the Kotlin daemon, parallel off). If the daemon dies with
*"insufficient memory for the Java Runtime Environment"*, close Chrome / VS Code
and run it again — that error is the OS refusing the allocation, not a code problem.

**Disk.** `C:` has ~2.9 GB free; a full Gradle cache plus Android build outputs
will not comfortably fit. The Gradle home has been placed on `D:` instead. Either
pass it per build:

```bash
./gradlew -g D:/gradle-home assembleDebug
```

or set `GRADLE_USER_HOME=D:\gradle-home` as a user environment variable so
Android Studio picks it up too. The Gradle 9.7.0 distribution zip is already
seeded there, so the first build skips the 150 MB download.

## Dependencies trimmed from the original spec

The original dependency list included libraries that nothing in the app calls, and
several of them would have broken the build or the first launch:

| Removed | Why |
|---|---|
| Firebase Messaging + `FCMService` | No `google-services.json`; the manifest referenced a service class that did not exist |
| Supabase (auth / realtime / storage) | Unused — every screen runs on mock data; pulls a large Ktor tree |
| Room | Declared with `annotationProcessor`, which does nothing for Kotlin (needs KSP), and no DAOs or entities exist |
| Retrofit / OkHttp / Gson | Unused — no network layer yet |
| Google Maps + maps-compose | `MapScreen` is a placeholder that already says *"Add your Google Maps API key to enable"* |
| Accompanist Permissions | Replaced by the standard `ActivityResultContracts.RequestMultiplePermissions` in `MainActivity` |

Kept: Compose (BOM-aligned), Navigation Compose, Material 3, extended icons, Coil,
`kotlinx-serialization` (the data models are `@Serializable` and ready for a backend).

Add any of these back when the feature behind it is real — the ProGuard rules already
keep the Maps and Play Services `-dontwarn` entries.

## Fixes applied to the supplied source

- `androidx.compose.material.icons.materialIcon` used as a parameter type → `ImageVector`
- `androidx.compose.foundation.isSystemInDarkMode` → `isSystemInDarkTheme`
- Deprecated `window.statusBarColor` block dropped from the theme; status bar colour
  moved to `themes.xml` with a `values-night` variant
- `Divider` → `HorizontalDivider`; `Icons.Default.ArrowBack` / `ExitToApp` →
  their `AutoMirrored` equivalents
- `HomeScreen`'s hand-rolled `Modifier.border` extension (which called the real one
  as a top-level function) removed in favour of `androidx.compose.foundation.border`
- Missing imports restored across the split screen files (`Color`, `Vendor`, `Scaffold`)
- `NavGraph.kt` rewritten for all 10 routes with a 6-tab bottom bar and an overflow
  menu; `MainActivity` requests location permission on startup
- Manifest: `FCMService` entry removed, storage permissions bounded with `maxSdkVersion`
- Added the resources the manifest referenced but that did not exist:
  `backup_rules.xml`, `data_extraction_rules.xml`, adaptive launcher icons

## Layout

```
app/src/main/kotlin/com/ooruva/app/
├── MainActivity.kt
├── data/models/          User, Vendor, Photo, CommunityPost,
│                         CommunityComment, CheckIn, Favorite
├── ui/navigation/        NavGraph.kt
├── ui/screens/           10 screens
└── ui/theme/             Color.kt, Theme.kt, Typography.kt
```
