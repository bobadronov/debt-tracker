# DebtTracker

A Kotlin Multiplatform app for tracking personal debts in both directions:
who owes **you** money, and who **you** owe money to. Local-first by default,
with optional cloud sync via Supabase.

Built with **Compose Multiplatform** — one shared UI codebase targets
Android, iOS, Desktop (JVM), and Web (JS/Wasm).

**Live web app:** https://bobadronov.github.io/debt-tracker/
(double-click [`web-page.html`](web-page.html) for a local shortcut that
opens it in your default browser).

## Features

- **Two mirrored directions** — "Мені винні" (people who owe me) and "Я
  винен" (people I owe), each with its own list, detail screen, and running
  total. The same person can appear in both lists independently; balances
  are never netted against each other.
- **Offline-first, optional sync** — every write lands in a local Room
  database first (`syncStatus = PENDING`) and is pushed to Supabase in the
  background. Works fully offline in **Local-only** mode; sign in to switch
  to **Account+Sync** and get realtime updates across devices, with
  last-write-wins conflict resolution on `updatedAt`.
- **App lock** — biometric prompt (BiometricPrompt / LocalAuthentication)
  with PIN fallback, toggled per device in Settings.
- **Transaction history per contact** — record lending/borrowing and
  partial/full repayments, each transaction signed (`+`/`-`) to derive its
  type and color from the amount alone.
- **Stats screen** — total owed in each direction, top debtors/creditors,
  and a 6-month trend chart.
- **Export** — CSV/PDF export of transaction history, filtered by direction
  and date range, generated natively on each platform.
- **Android home-screen widget** (Glance) showing the two running totals.
- **Search, sort, filter, swipe-to-delete**, sound feedback on key actions,
  and Desktop keyboard shortcuts (`Ctrl+N` new debtor, `Ctrl+Shift+N` new
  creditor, `Ctrl+F` search, `Esc` back).

## Tech stack

| | |
|---|---|
| Language | Kotlin 2.4 (Kotlin Multiplatform) |
| UI | Compose Multiplatform 1.12 (Material 3) |
| DI | Koin 4 (`koin-compose`, `koin-compose-viewmodel`) |
| Local storage | Room (KMP) 2.8 |
| Backend | Supabase (`supabase-kt` 3.7) — Auth, Postgrest, Realtime |
| Networking | Ktor client 3.5 |
| Navigation | Navigation 3 (`androidx.navigation3`) |
| Async | Kotlin Coroutines & Flow |
| Precision math | `bignum` (`BigDecimal`, for money) |
| Settings | `multiplatform-settings` |

## Architecture

The whole app — UI, domain logic, and data layer — lives in a single shared
module, **`sharedUI`**. Platform modules (`androidApp`, `iosApp`,
`desktopApp`, `webApp`) are thin entry points only (`Application`/`main()`/
`MainViewController`) with no business logic of their own.

```
sharedUI/src
├── commonMain/kotlin/org/bigblackowl/debttracker
│   ├── domain/            # pure Kotlin: models, repository interfaces, use cases
│   │   ├── model/         # Debtor/Creditor + their transactions, enums
│   │   ├── repository/    # DebtorRepository / CreditorRepository / AuthRepository
│   │   └── usecase/       # one class per operation (ObserveDebtorsUseCase, ...)
│   ├── core/
│   │   ├── di/            # Koin modules (appModule + expect platformDataModule)
│   │   ├── settings/      # AppSettings (Compose-reactive user prefs)
│   │   ├── security/      # expect BiometricAuthenticator
│   │   ├── export/        # expect FileExporter (CSV/PDF)
│   │   ├── sound/         # expect SoundPlayer
│   │   └── shortcuts/     # Desktop hotkey → search-focus bridge
│   ├── ui/
│   │   ├── screens/{debtors,creditors,auth,settings,export,stats}/
│   │   │   # each screen is MVI: <Name>Contract.kt (State/Intent/Effect),
│   │   │   # <Name>ViewModel.kt, <Name>Screen.kt (Composable)
│   │   └── components/    # shared composables (AmountBottomSheet, PlaceholderScreen, ...)
│   ├── navigation/        # sealed Screen routes + NavDisplay graph
│   ├── theme/             # Material 3 color schemes, debt/repay accent colors
│   └── preview/           # @Preview support: fake in-memory repos + isolated Koin context
├── roomMain/               # Entity/DAO/Database + repository impls + sync (Android/iOS/JVM only)
├── androidMain/ iosMain/ jvmMain/ webMain/
│                           # actual implementations of the expect declarations above
└── webMain                 # shared JS + Wasm source set (no Room — online-only on Web)
```

**Screen pattern (MVI):** each screen's `Contract` file defines an immutable
`State`, a sealed `Intent` for user actions, and a sealed `Effect` for
one-shot events (navigation, snackbars). The `ViewModel` reduces intents into
state and exposes it as a `StateFlow`; the `Screen` composable is a thin
render of that state plus `onIntent` dispatch. `Debtor`/`Creditor` screens
are structurally identical, mirrored one-to-one.

**Platform code:** `commonMain` never contains platform-specific code — where
a real per-platform implementation is unavoidable (biometrics, file export,
sound, the local database), `commonMain` declares an `expect`, and each
platform source set provides the `actual`.

## Previewing screens

Every screen composable in `ui/screens` has a matching `@Preview` function.
Because screens resolve their `ViewModel`/repositories via Koin
(`koinViewModel()`, `koinInject()`), previews render inside
`DebtTrackerPreview { ... }` (`core/preview/PreviewHost.kt`), which spins up
an isolated `KoinApplicationPreview` wired to in-memory fake repositories
(`preview/PreviewFakes.kt`) instead of Room/Supabase — no network or database
access happens when rendering a preview.

## Getting started

1. Create a Supabase project and run `supabase/full_backup.sql` once (SQL
   Editor or `psql -f`) — a single, ready-to-run snapshot of the full schema
   (`profiles`, `debtors`, `debt_transactions`, `creditors`,
   `creditor_transactions`, triggers, RLS policies). `supabase/migrations/`
   holds the same schema as incremental, versioned steps instead, if you'd
   rather apply it via `supabase db push`.
2. Copy `secrets.properties.example` to `secrets.properties` and fill in your
   project's URL and anon key — read by `sharedUI/build.gradle.kts`'s
   `buildConfig { ... }` block. The anon key is safe to keep client-side
   since RLS enforces per-user data isolation.
3. Build/run one of the targets below.

## Build & run

### Android
Open the project in Android Studio and run the `androidApp` configuration, or:
```
./gradlew :androidApp:assembleDebug
```
APK output: `androidApp/build/outputs/apk/debug/androidApp-debug.apk`

### Desktop
```
./gradlew :desktopApp:run
```
Hot reload:
```
./gradlew :desktopApp:hotRun --auto
```

### iOS
Open `iosApp/iosApp.xcodeproj` in Xcode and run, or use the
[Kotlin Multiplatform Mobile plugin](https://plugins.jetbrains.com/plugin/14936-kotlin-multiplatform-mobile)
for Android Studio. `iosApp` is a minimal Swift shell — all UI is the shared
Compose Multiplatform code.

### Web
JS dev server:
```
./gradlew :webApp:jsBrowserDevelopmentRun
```
Wasm dev server:
```
./gradlew :webApp:wasmJsBrowserDevelopmentRun
```
Production distribution:
```
./gradlew :webApp:composeCompatibilityBrowserDistribution
```
Deploy `webApp/build/dist/composeWebCompatibility/productionExecutable` to
any static web server. The `release.yml` workflow deploys it automatically to
GitHub Pages at https://bobadronov.github.io/debt-tracker/ on every tagged
release.