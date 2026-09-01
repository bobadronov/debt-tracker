# DebtTracker — rebuild prompt

This file is a **self-contained prompt**: if the repository were ever lost and
only this one file survived, pasting its contents to an AI coding agent (or
using it as a human spec) should be enough to recreate the app faithfully —
same features, same architecture, same conventions. It supersedes the old
local-only, gitignored `AGENTS.MD` / `debt-tracker-kmp-spec.md` files that
this project used to keep outside version control; both are gone now, so
everything they held that still matters has been folded in here and this file
*is* tracked in git.

Pair this with `supabase/full_backup.sql` (the full DB schema, restorable in
one shot on a fresh Supabase project) to recover both the app and its backend.

---

## 1. Prompt to give an AI agent

> Build a Kotlin Multiplatform app called **DebtTracker** for tracking
> personal debts in both directions: who owes *me* money, and who *I* owe
> money to. It must be local-first by default (fully usable offline, no
> account required) with an optional cloud-sync account mode backed by
> Supabase.
>
> Use **Compose Multiplatform** for a single shared UI codebase targeting
> Android, iOS, Desktop (JVM), and Web (JS/Wasm). Platform modules
> (`androidApp`, `iosApp`, `desktopApp`, `webApp`) must be thin entry points
> only — `Application`/`main()`/`MainViewController` — with **no** business
> logic; everything else lives in one shared module, `sharedUI`.
>
> Follow the feature list, architecture, tech stack, database schema, build
> setup, and conventions in the sections below exactly. Where this document
> doesn't specify an implementation detail, prefer the simplest idiomatic
> Kotlin Multiplatform / Compose Multiplatform approach and stay consistent
> with the patterns already established elsewhere in the spec (MVI screens,
> expect/actual for platform code, offline-first + last-write-wins sync).

---

## 2. Features

- **Two mirrored directions** — "Мені винні" (people who owe me) and "Я
  винен" (people I owe), each with its own list, detail screen, and running
  total. The same person can appear in both lists independently; balances
  are never netted against each other.
- **Offline-first, optional sync** — every write lands in a local Room
  database first (`syncStatus = PENDING`) and is pushed to Supabase in the
  background. Works fully offline in **Local-only** mode; sign in to switch
  to **Account+Sync** and get realtime updates across devices, with
  last-write-wins conflict resolution on `updatedAt`.
- **Multi-currency** — each debtor/creditor is tagged with a currency (UAH /
  USD / PLN / EUR) fixed at the person level; a whole debt is in one
  currency, no FX conversion between transactions.
- **Contact autofill by email** — when adding a debtor/creditor, looking up
  their email against other registered users' profiles offers their display
  name + avatar for autofill (via a `SECURITY DEFINER` RPC, since RLS
  otherwise blocks reading another user's profile — see schema §7 below).
  The resulting `avatarUrl` is shown everywhere that debtor/creditor appears:
  list rows, detail screen, stats top-lists, and the name-suggestion
  autocomplete dropdown — there is no manual per-contact avatar upload, only
  this lookup-based autofill (contrast with the user's own profile photo,
  which *is* user-uploaded — see §7's `avatars` Storage bucket).
- **QR contact exchange** — share your own contact card (name/phone/email,
  autofilled from the account when signed in) as a QR code, and scan someone
  else's to prefill a new debtor/creditor form. See §6a below for the full
  design (this needs its own section since scan capability differs sharply
  by platform).
- **App lock** — biometric prompt (BiometricPrompt / LocalAuthentication)
  with PIN fallback, toggled per device in Settings. Devices with no
  biometric hardware or no enrolled biometric fall back to **PIN-only**
  protection during onboarding and in Settings, rather than only offering a
  "Skip" — biometric absence must never silently leave the app unlocked.
- **Transaction history per contact** — record lending/borrowing and
  partial/full repayments, each transaction signed (`+`/`-`) to derive its
  type and color from the amount alone.
- **Stats screen** — total owed in each direction, top debtors/creditors,
  and a 6-month trend chart.
- **Export** — CSV/PDF export of transaction history, filtered by direction
  and date range. CSV is UTF-8 with a leading BOM (so Excel, which otherwise
  guesses the system codepage for a BOM-less file, renders Cyrillic content
  correctly). Android/iOS/Desktop generate the PDF as a real vector document
  (PdfKmp) and save both files via FileKit's cross-platform save dialog; Web
  has no PdfKmp target and no bundled-Unicode-font JS PDF library available,
  so it renders the report as HTML into a hidden iframe and calls the
  browser's own `print()`, letting the user "Save as PDF" through the OS/
  browser dialog with correct Cyrillic via the browser's native text
  rendering.
- **Android home-screen widget** (Glance) — a **fixed-size** 2×2 tile
  (`android:resizeMode="none"`, `SizeMode.Single`), two stacked icon-badge KPIs
  (colored circular badge + trend icon, matching the Stats screen's accent
  colors) showing the two running totals ("Мені винні" / "Я винен"). Not
  user-resizable — the layout is tuned for one size only. Ships a static XML
  widget-preview so Android's widget picker shows a realistic thumbnail before
  placement.
- **In-app update check (Android)** — uses Google Play's in-app update API
  (`app-update-ktx`), not GitHub; Settings shows live status (`Checking`,
  `UpToDate`, `CheckFailed`, `Downloading`, `DownloadFailed`) next to the
  manual "check for update" button. Desktop instead polls GitHub Releases
  (`AppUpdateChecker`) since there's no Play Store there; iOS/Web have
  neither.
- **Delete all data** — a double-confirmation "Delete all data" action that
  hard-deletes everything server-side via a `SECURITY DEFINER` RPC scoped to
  `auth.uid()` (settings/profile row itself is left untouched).
- **Runtime language switcher** — System / Українська / English / Polski,
  each with a flag icon in the picker, switchable without restarting the app
  (see §6 "Localization" — this is *not* done via `compose.resources`).
- **Search, sort, filter, swipe-to-delete**, sound + haptic feedback on key
  actions (both independently toggleable in Settings; the haptic toggle only
  shows on Android/iOS, since Desktop/Web have no vibration motor behind
  `LocalHapticFeedback`), and Desktop keyboard shortcuts (`Ctrl+N` new
  debtor, `Ctrl+Shift+N` new creditor, `Ctrl+F` search, `Esc` back).

## 3. Tech stack

Exact versions matter less than picking current stable releases of the same
libraries — pin whatever is current when rebuilding — but as of this
snapshot:

| | |
|---|---|
| Language | Kotlin 2.4.x (Kotlin Multiplatform) |
| UI | Compose Multiplatform 1.12.x (Material 3) |
| DI | Koin 4.x (`koin-compose`, `koin-compose-viewmodel`) |
| Local storage | Room (KMP) 2.8.x |
| Backend | Supabase (`supabase-kt` 3.7.x) — Auth, Postgrest, Realtime |
| Networking | Ktor client 3.5.x |
| Navigation | Navigation 3 (`androidx.navigation3`) |
| Async | Kotlin Coroutines & Flow |
| Precision math | `bignum` (`BigDecimal`, for money — never `Float`/`Double`) |
| Settings | `multiplatform-settings` |
| Images | Coil 3 (`coil-compose`, `coil-network-ktor3`) |
| Android widget | AndroidX Glance |
| PDF export | PdfKmp (`io.github.conamobiledev:pdfkmp` + `pdfkmp-viewer`) — vector DSL, Android/Desktop/iOS only; Web uses browser print instead (no PdfKmp target) |
| File export/picking | FileKit (`io.github.vinceglb:filekit-dialogs-compose`) — cross-platform save dialog + image picker, replaces per-platform expect/actual for both |
| QR generate | `qrose` (`io.github.alexzhirkevich:qrose`) — pure Kotlin, only QR library here that publishes a working js/wasmJs target |
| QR scan (camera) | QRKit (`network.chaintech:qr-kit`) — Android/iOS only; excluded on Desktop to avoid its heavy native transitive deps (javacv/opencv/ffmpeg, ~900MB) |
| QR decode (picked image) | ZXing (`com.google.zxing:core`) on Desktop; the browser's native `BarcodeDetector` API on Web (Chrome/Edge only — falls back to "unsupported" elsewhere); no camera scanner on either platform |
| Android in-app update | `com.google.android.play:app-update-ktx` |

Android: `compileSdk = 37`, `minSdk = 26` (biometric API stability), `targetSdk = 37`.
JVM target for Android/JVM source sets: **17**.

## 4. Architecture

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
│   │   ├── export/        # expect FileExporter (CSV/PDF), FileKit-backed on Android/iOS/Desktop
│   │   ├── sound/         # expect SoundPlayer
│   │   ├── qr/            # QR contact-exchange: generate/scan/decode, deep links (see §6a)
│   │   ├── media/         # FileKit-backed cross-platform image picker (no expect/actual needed)
│   │   ├── update/        # expect InAppUpdateLauncher (Play in-app update on Android, GitHub polling on Desktop)
│   │   ├── i18n/          # Strings.kt catalog + AppLocale.kt + translate/ (see §6)
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
├── pdfMain/                # PdfKmp vector-DSL PDF report builder (Android/iOS/JVM — not Web)
├── androidMain/ iosMain/ jvmMain/ webMain/
│                           # actual implementations of the expect declarations above
└── webMain                 # shared JS + Wasm source set (no Room — online-only on Web)
```

Note: `roomMain` and `pdfMain` are **custom intermediate source sets**, which
opts KGP out of its default hierarchy template project-wide — wire the full
`commonMain`/`roomMain`/`pdfMain`/platform hierarchy by hand via explicit
`dependsOn(...)` in `sharedUI/build.gradle.kts`, and set
`kotlin.mpp.applyDefaultHierarchyTemplate=false` in `gradle.properties`.

**Screen pattern (MVI):** each screen's `Contract` file defines an immutable
`State`, a sealed `Intent` for user actions, and a sealed `Effect` for
one-shot events (navigation, snackbars). The `ViewModel` reduces intents into
state and exposes it as a `StateFlow`; the `Screen` composable is a thin
render of that state plus `onIntent` dispatch. `Debtor`/`Creditor` screens
are structurally identical, mirrored one-to-one — implement one, then mirror
it for the other rather than diverging the two.

**Platform code:** `commonMain` never contains platform-specific code — where
a real per-platform implementation is unavoidable (biometrics, file export,
sound, the local database), `commonMain` declares an `expect`, and each
platform source set provides the `actual`.

**Previewing screens:** every screen composable in `ui/screens` has a
matching `@Preview` function. Since screens resolve their
`ViewModel`/repositories via Koin (`koinViewModel()`, `koinInject()`),
previews render inside a `DebtTrackerPreview { ... }` wrapper
(`core/preview/PreviewHost.kt`) that spins up an isolated
`KoinApplicationPreview` wired to in-memory fake repositories
(`preview/PreviewFakes.kt`) instead of Room/Supabase — no network or database
access should happen when rendering a preview.

## 5. Data layer & sync

- Every entity has `id` (client-generated UUID, offline-first), `isDeleted`
  (soft delete, so sync can propagate deletions), `createdAt`/`updatedAt`
  (source of truth for last-write-wins), and `syncStatus` (`PENDING` until
  pushed).
- `debtors`/`creditors` denormalize a `status` (`ACTIVE`/`CLOSED`) column,
  recalculated server-side by a trigger whenever their transactions change
  (`balance = -SUM(amount)`, `CLOSED` if `balance <= 0`) — don't compute this
  client-side from scratch, mirror what the trigger does.
- Amounts are **signed**: for debtors, `LEND` is negative and `REPAY` is
  positive; for creditors, `BORROW` is negative and `RETURN` is positive.
  The transaction's type/color is derived purely from the sign of `amount` —
  don't store the type redundantly as authoritative, treat it as denormalized
  for query convenience only.
- `SyncCoordinator` (in `roomMain`) reconciles local Room state with Supabase
  Postgrest + Realtime: push pending local writes, pull remote changes,
  resolve conflicts by comparing `updatedAt` (last write wins).
- Web has no Room — it's online-only, Account+Sync required, talking to
  Supabase directly (see `webMain`).

## 6. Localization

The app has a runtime language switcher (System / Українська / English /
Polski, each row with a flag icon) in Settings that intentionally does
**not** use `compose.resources` (`Res.string.*`). Reason: at the pinned
Compose Multiplatform version, the `components-resources` library has no
public API to override the resolved locale independent of the OS locale —
`stringResource()` always follows the live system locale with no override
hook (`ComposeEnvironment`, `LocalComposeEnvironment`, `ResourceEnvironment`,
`LanguageQualifier` are all `internal`). If rebuilding against a newer
Compose Multiplatform release, re-check whether that library now exposes a
public locale override before assuming this workaround is still necessary.

Instead, build a hand-rolled catalog in `core/i18n/`:
- `Strings.kt` — a `data class Strings` with every user-facing string as a
  field (interpolated ones as `(Int) -> String` etc.). No language data here
  — just the field declarations.
- `core/i18n/translate/{UkStrings,EnStrings,PlStrings}.kt` — one file per
  language, each a top-level `val` instance of `Strings`. Adding a new
  language means adding one new file here rather than growing `Strings.kt`
  itself.
- `AppLocale.kt` — `LocalStrings` (a `CompositionLocal<Strings>`), a
  `supportedLanguages: Map<String, Strings>` (`"uk"`/`"en"`/`"pl"` →
  instance), `resolveStrings(localeSetting: String): Strings` (a plain,
  non-composable function — needed in ViewModels and the Android Glance
  widget, not just Composables — that looks up `localeSetting` in the map,
  then falls back to the OS locale via `Locale.current.language`, then to
  `UkStrings`), and a `ProvideAppStrings` composable wrapper.
- `AppSettings.locale` (`"system" | "uk" | "en" | "pl"`, default `"system"`)
  drives it; the app's root theme composable wraps its content in
  `ProvideAppStrings(settings.locale)`.
- ViewModels that need a localized fallback string outside Compose take
  `AppSettings` as a constructor param and call `resolveStrings(...)`
  directly, since they can't use a `CompositionLocal`.
- The language picker (`LanguageScreen.kt`) pairs each language with a flag
  emoji (`LanguageOption(value, label, flag)`); "System" has no flag and
  keeps a generic language icon instead.

When adding new user-facing text, add a field to `Strings` and fill it in on
every file under `core/i18n/translate/` rather than reaching for
`Res.string`. When adding a new language, add both a new
`core/i18n/translate/XxStrings.kt` file and an entry in `AppLocale.kt`'s
`supportedLanguages` map and the language picker's option list.

## 6a. QR contact exchange

A "Share/Scan" hub screen (`ui/screens/qr/QrHubScreen.kt`, reachable from the
Home top bar) with two modes:

- **Share (default)** — renders your own contact card (name/phone/email —
  autofilled from the account when signed in, or entered/saved locally when
  signed out) as a QR image (`core/qr/ContactQrImage.kt`,
  `rememberContactQrPainter`), encoding a deep link:
  `debttracker://contact?name=...&phone=...&email=...` (query params
  UTF-8 percent-encoded via Ktor's `Parameters`/`formUrlEncode` —
  `domain/model/ContactQrPayload.kt`). Generation uses **qrose**, a pure-
  Kotlin QR library with no expect/actual split — the same code path renders
  the code on every platform including Web/wasmJs. The QR module color
  flips to stay visible on dark backgrounds; the app logo is centered on the
  code.
- **Scan** — a single button on the Share screen adapts by platform
  (`core/qr/QrScanSupport.kt`'s `QR_SCAN_CAPABLE_PLATFORMS = {ANDROID, IOS}`):
  - **Android/iOS** ("Scan"): switches to live-camera scanning via QRKit's
    `QrScanner` (`ContactQrScanner.android.kt`/`.ios.kt`), which owns its own
    camera-permission flow.
  - **Desktop/Web** ("Select image", no camera scanner available): opens the
    OS file picker directly (FileKit's `rememberImagePicker`) and decodes the
    picked image via `decodeQrFromImage` — Desktop uses ZXing
    (hand-rolled `BufferedImageLuminanceSource`); Web uses the browser's
    native `BarcodeDetector` API (Chrome/Edge only — returns
    `QrDecodeResult.Unsupported` elsewhere, no bundled decode library on
    Web). QRKit's own image-picker path is deliberately not used anywhere —
    it crashes against this project's Material3 version.
- A successful scan/decode shows a dialog asking "add as debtor or
  creditor?", then navigates to the matching Add form prefilled from the
  decoded contact.
- The custom `debttracker://contact?...` scheme is also registered as an
  Android intent-filter and iOS `CFBundleURLTypes`, so third-party scanners
  (e.g. Google Lens, the stock camera app) can open the app directly with
  the contact prefilled, without going through the in-app scanner at all.
  `core/qr/ContactDeepLinks.kt` bridges an externally-opened link
  (`AppActivity.onCreate`/`onNewIntent` on Android, `onOpenURL` on iOS — no
  equivalent on Desktop/Web, which have no OS-level URL-scheme handoff into
  a running app in the same way) into the nav graph via a
  `StateFlow<String?>`.
- This feature carries **no schema changes** — it's pure client-side
  deep-link/payload plumbing; the QR payload itself never includes an
  avatar, only name/phone/email.

## 7. Database schema (Supabase / Postgres)

Full, ready-to-run schema: **`supabase/full_backup.sql`** (versioned,
incremental history: `supabase/migrations/0001..0006`). Summary:

- `profiles` — 1:1 with `auth.users`; app settings (theme, locale, biometric/
  sound/haptic toggles) + `display_name`/`avatar_url`/`email` for the
  contact-autofill lookup. Auto-created via an `on_auth_user_created` trigger
  on `auth.users`.
- `debtors` / `debt_transactions` — people who owe the user money, and their
  signed transactions.
- `creditors` / `creditor_transactions` — mirror of the above, people the
  user owes.
- Every table has **RLS enabled** with an owner-only `for all using/with
  check (user_id = auth.uid())` policy (or `id = auth.uid()` for `profiles`)
  — strict per-user isolation, no cross-user reads except through the
  sanctioned `find_profile_by_email` RPC below.
- Trigger-only functions (`touch_updated_at`, `handle_new_user`,
  `recalc_debtor_status`, `recalc_creditor_status`) have `EXECUTE` revoked
  from `public`/`anon`/`authenticated` — triggers fire regardless of grants,
  so this just stops them being callable as public RPC endpoints via
  PostgREST.
- `find_profile_by_email(p_email text)` — `SECURITY DEFINER`, returns only
  `display_name`/`avatar_url` for a single email match, deliberately bypasses
  owner-only RLS for the autofill feature. Granted to `authenticated` only.
- `delete_all_user_data(uid uuid)` — `SECURITY DEFINER`, hard-deletes a
  user's rows across all tables after checking `auth.uid() = uid`. Granted to
  `authenticated` only; this one *should* show up in the Supabase security
  advisor as callable by authenticated users — that's intentional.
- `debtors`/`creditors`/`debt_transactions`/`creditor_transactions` are added
  to the `supabase_realtime` publication for client-side realtime
  subscriptions.
- Storage bucket `avatars` (public read) — the account photo picked in
  Settings (`AccountAvatar` → `AuthRepository.updateAvatar`) uploads to
  `avatars/{auth.uid()}/avatar.{ext}` and stores the resulting public URL in
  `profiles.avatar_url`, reloaded into `avatarUrl` (`StateFlow`) whenever
  `client.auth.sessionStatus` becomes `Authenticated` — including right after
  sign-in, so the photo shows up without extra plumbing. `storage.objects`
  RLS restricts insert/update/delete to the caller's own folder (first path
  segment must equal `auth.uid()`).

Setting up a fresh backend: create a Supabase project, run
`supabase/full_backup.sql` once (SQL Editor or `psql -f`), then wire the new
project's URL/anon key per §8.

## 8. Configuration & secrets

- `sharedUI/build.gradle.kts` has a `buildConfig { ... }` block exposing
  `SUPABASE_URL`, `SUPABASE_ANON_KEY`, `APP_VERSION`, `APP_VERSION_CODE`,
  `APP_AUTHOR` to all targets via generated `BuildConfig`.
  `SUPABASE_URL`/`SUPABASE_ANON_KEY` are read from a `secret(...)` helper
  backed by Gradle properties/env vars — **not hardcoded**.
- Local dev: copy `secrets.properties.example` → `secrets.properties` (git-
  ignored) and fill in the real project URL + anon key. The anon key is
  intentionally safe to ship client-side — RLS enforces per-user isolation,
  not secrecy of the key.
- `version.properties` (`VERSION_NAME`, `VERSION_CODE`) is the single source
  of truth for the app version across Android/Desktop/Web/iOS, read by each
  platform module's `build.gradle.kts`.
- Android release signing comes entirely from environment variables
  (`ANDROID_KEYSTORE_PATH`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`,
  `ANDROID_KEY_PASSWORD`) — never hardcoded/committed. If they're unset,
  `assembleRelease` silently produces an unsigned APK instead of failing.

## 9. Build & run

```
# Android
./gradlew :androidApp:assembleDebug

# Desktop
./gradlew :desktopApp:run
./gradlew :desktopApp:hotRun --auto   # hot reload

# iOS — open iosApp/iosApp.xcodeproj in Xcode and run
#        (iosApp is a minimal Swift shell; all UI is shared Compose code)

# Web
./gradlew :webApp:jsBrowserDevelopmentRun
./gradlew :webApp:wasmJsBrowserDevelopmentRun
./gradlew :webApp:composeCompatibilityBrowserDistribution   # production build
```

**Build verification order** (cheapest/fastest failures first): JVM →
Android → JS/Wasm → iOS. Kotlin/Native cross-compiles the iOS targets even
without Xcode/a Mac — only final linking/running on a real device or
simulator needs one, plain compilation doesn't. Don't skip iOS compile
verification by assuming it's unreachable:

```
./gradlew :sharedUI:compileKotlinJvm :sharedUI:compileAndroidMain \
  :sharedUI:compileKotlinIosArm64 :sharedUI:compileKotlinIosSimulatorArm64 \
  :sharedUI:compileKotlinJs :sharedUI:compileKotlinWasmJs \
  :androidApp:compileDebugKotlin :desktopApp:compileKotlin :webApp:compileKotlinJs
```

## 10. CI/CD

`.github/workflows/release.yml` — triggered by pushing a `v*` tag (or manual
`workflow_dispatch`). Jobs: `android-apk` (ubuntu), `windows-msi` (Windows —
Compose Desktop downloads WiX automatically), `linux-deb` (ubuntu, needs
`fakeroot dpkg-dev`), `web` (Windows — `wasm-opt`/Binaryen crashes on the
Ubuntu runner for this project's wasmJs production optimization, Windows
avoids it), `deploy-pages` (publishes the web build to GitHub Pages), then
`github-release` (attaches APK/.msi/.deb to a GitHub Release with generated
notes — the `.aab` is Play-Store-only and isn't attached). `SUPABASE_URL`/
`SUPABASE_ANON_KEY` and the four `ANDROID_*` signing secrets are wired in via
repo/Actions secrets — see §8.

A local convenience script, `publishing.bat` (Windows-only), interactively
prompts for the new `VERSION_NAME` and `VERSION_CODE` (blank = auto-bump),
then bumps `version.properties`, commits + pushes ALL pending changes to
main, and dispatches the Release workflow via `gh workflow run` with
`play_track=production` — not required to rebuild the project, just a
personal shortcut.

## 11. Conventions

- Money is always `BigDecimal` (`bignum`), never `Float`/`Double`.
- Dependency versions live in one version catalog
  (`gradle/libs.versions.toml`) — don't hardcode versions in individual
  `build.gradle.kts` files.
- `Debtor`/`Creditor` screens, models, repositories, and use cases are
  structurally mirrored — when changing one side, mirror the change on the
  other rather than letting them drift.
- Bilingual (Ukrainian + English) comments in SQL migrations and doc-comments
  that explain non-obvious *why* — not required for every comment, but is the
  established style in `supabase/migrations/`.
