# DebtTracker — Google Play submission packet

Everything needed to submit DebtTracker to Google Play, gathered in one
place. The parts that need *you* (an account, judgment calls, actually
clicking "Submit") are marked accordingly — I can't do those.

## 0. What only you can do (before anything else)

1. Create a [Google Play Console](https://play.google.com/console/) developer
   account — one-time $25 fee, government ID verification, accept the
   Developer Distribution Agreement. Takes Google anywhere from a few hours
   to a couple of days to approve.
2. In Play Console: **Create app** → name it, pick default language, pick
   "App" (not game), pick "Free". This locks in the package name
   (`org.bigblackowl.debttracker.androidApp`, from the `.aab` you upload) —
   it can never change after your first upload, so double-check it here.

## 1. Store listing text

**App name** (30 char max): `Debt Tracker`

**Short description** (80 char max, 72 used):
```
Track who owes you and who you owe — offline-first, optional cloud sync.
```

**Full description** (4000 char max):
```
Debt Tracker keeps track of money owed in both directions — who owes you,
and who you owe — without netting the two against each other.

KEY FEATURES
• Two independent lists: people who owe you, and people you owe
• Log every loan and repayment as its own transaction, with running balances
• Multi-currency: UAH, USD, PLN, EUR, per contact
• Works fully offline — no account required
• Optional free account to sync across your devices in real time
• Add contacts by scanning a QR code — no typing names or numbers
• App lock via fingerprint/face unlock or PIN
• Stats screen: totals, top debtors/creditors, 6-month trend
• Export transaction history to CSV or PDF
• Home-screen widget showing your two running totals
• Available in Ukrainian, English, and Polish
• Search, sort, filter, swipe-to-delete
• No ads. No trackers. No analytics SDKs.

Your data stays on your device unless you choose to create an account for
sync — see the privacy policy for exactly what that involves.
```

**Короткий опис** (uk-UA, 80 символів макс, 74 використано):
```
Стежте, хто винен вам і кому винні ви — офлайн, синхронізація за бажанням.
```

**Повний опис** (uk-UA, 4000 символів макс, 1056 використано):
```
DebtTracker веде облік грошей в обидва боки — хто винен вам, і кому винні ви, — не згортаючи одне в інше.

ОСНОВНІ МОЖЛИВОСТІ
• Два незалежні списки: люди, які винні вам, і люди, яким винні ви
• Кожна позика та повернення — окрема транзакція з поточним балансом
• Мультивалютність: UAH, USD, PLN, EUR — окремо для кожного контакту
• Повністю офлайн — акаунт не обов'язковий
• Безкоштовний акаунт за бажанням для синхронізації між пристроями в реальному часі
• Додавання контактів через сканування QR-коду — без введення імені чи номера
• Блокування застосунку відбитком пальця, обличчям або PIN-кодом
• Екран статистики: загальні суми, топ боржників/кредиторів, тренд за 6 місяців
• Експорт історії транзакцій у CSV або PDF
• Віджет на головному екрані з двома поточними сумами
• Доступно українською, англійською та польською мовами
• Пошук, сортування, фільтри, свайп для видалення
• Без реклами. Без трекерів. Без аналітичних SDK.

Ваші дані залишаються на пристрої, якщо ви не створите акаунт для синхронізації — детальніше в політиці конфіденційності.
```

**Krótki opis** (pl-PL, 80 znaków maks., 79 użyto):
```
Śledź, kto jest winien Tobie i komu winien Ty — offline, synchronizacja online.
```

**Pełny opis** (pl-PL, 4000 znaków maks., 1097 użyto):
```
Debt Tracker śledzi pieniądze pożyczone w obie strony — kto jest winien Tobie i komu Ty jesteś winien — bez wzajemnego rozliczania.

GŁÓWNE FUNKCJE
• Dwie niezależne listy: osoby winne Tobie i osoby, którym Ty jesteś winien
• Każda pożyczka i spłata to osobna transakcja z bieżącym saldem
• Wiele walut: UAH, USD, PLN, EUR — osobno dla każdego kontaktu
• Działa całkowicie offline — konto nie jest wymagane
• Opcjonalne bezpłatne konto do synchronizacji między urządzeniami w czasie rzeczywistym
• Dodawanie kontaktów przez skanowanie kodu QR — bez wpisywania nazwiska czy numeru
• Blokada aplikacji odciskiem palca, twarzą lub kodem PIN
• Ekran statystyk: sumy, najwięksi dłużnicy/wierzyciele, trend z 6 miesięcy
• Eksport historii transakcji do CSV lub PDF
• Widżet na ekranie głównym z dwiema bieżącymi sumami
• Dostępne w językach: ukraińskim, angielskim i polskim
• Wyszukiwanie, sortowanie, filtry, przesunięcie do usunięcia
• Bez reklam. Bez trackerów. Bez SDK analitycznych.

Twoje dane pozostają na urządzeniu, chyba że założysz konto do synchronizacji — szczegóły w polityce prywatności.
```

**App category:** Finance

**Contact email:** bobadronov@gmail.com *(using the account email as a
placeholder — swap it for whatever public support address you want; once
published this is visible to every user)*

**Website:** https://bobadronov.github.io/debt-tracker/

**Privacy policy URL:** https://bobadronov.github.io/debt-tracker/privacy-policy.html
*(now wired into `release.yml`'s web job — goes live on the next tagged
release; until then this URL 404s)*

## 2. Graphic assets

| Asset | Spec | Status |
|---|---|---|
| App icon | 512×512 PNG, 32-bit with alpha, <1MB | Done — [`webApp/src/commonMain/resources/android-chrome-512x512.png`](webApp/src/commonMain/resources/android-chrome-512x512.png) |
| Feature graphic | 1024×500 PNG/JPG (no alpha) | Done — [`store-assets/feature-graphic.png`](store-assets/feature-graphic.png), 371 KB |
| Phone screenshots | 2–8 images, PNG/JPEG, 16:9 or 9:16, side 320–3840px | Done — [`store-assets/phone/`](store-assets/phone), 1080×1920, 5 images |
| 7" tablet screenshots | up to 8 images, same format, side 320–3840px | Done — [`store-assets/tablet-7in/`](store-assets/tablet-7in), 1440×2560, 5 images |
| 10" tablet screenshots | up to 9 images, same format, side 1080–7680px | Done — [`store-assets/tablet-10in/`](store-assets/tablet-10in), 2160×3840, 5 images |

Both the feature graphic and the screenshots are generated, not hand-drawn or
device-captured — this machine has no Android SDK/emulator to capture real
screens from. `store-assets/screens.html` recreates 5 screens (debtor list,
creditor list, stats, a debtor's transaction history, settings) using the
app's actual strings (`core/i18n/Strings.kt`), colors
(`sharedUI/.../theme/Color.kt` dark scheme), and layout structure read
straight from each screen's Compose source, with placeholder demo data. The
same HTML is rendered at all three device sizes — `render-screens.mjs` swaps
the outer canvas size and toggles which `<section>` is visible, so the
"device" column stays a fixed pixel width and centers itself on wider
canvases, mirroring the real app's actual responsive behavior
(`Modifier.width(Dimens.contentMaxWidth)`, centered).

To tweak wording/data and re-render everything (feature graphic + all 15
screenshots):
```
cd store-assets
npm install playwright --no-save   # one-time, downloads a local Chromium
npx playwright install chromium    # one-time, ~115 MB browser binary
node render.mjs                    # feature-graphic.png
node render-screens.mjs            # phone/, tablet-7in/, tablet-10in/
```

If you'd rather have real device captures instead of recreations, that's
still an option — run the debug APK (`./gradlew :androidApp:installDebug`)
on a physical device or emulator and capture there instead.

## 3. Content rating questionnaire (IARC, inside Play Console)

Answer based on what the app actually does:

- Violence / sexual content / profanity / drugs / gambling: **None** — the
  app has no such content.
- User-generated content shared with other users / strangers: **No** — a
  user's debtor/creditor entries are private to their own account (RLS-
  enforced); nothing is posted publicly or shared between users.
- User-to-user communication (chat, messaging): **No**.
- Shares user's location: **No**.
- Allows purchase of digital goods / real-money gambling: **No** — the app
  *records* debts, it doesn't move money or process payments itself.

Expected result: **Everyone** / **PEGI 3**.

## 4. Data safety form (Play Console → App content → Data safety)

Best-effort mapping from the schema — **verify each category's current
wording in Play Console yourself before submitting**; Google's exact option
labels shift over time and I can't see the live form to confirm they still
match.

| Data type | Collected? | Shared with 3rd parties? | Purpose |
|---|---|---|---|
| Email address | Yes (Account+Sync only) | No | Account creation, authentication |
| Name (yours + contacts you add) | Yes (Account+Sync only) | No | App functionality |
| Phone number (contacts you add) | Yes (Account+Sync only) | No | App functionality |
| Photos (your avatar, optional) | Yes (Account+Sync only) | No | App functionality |
| Financial info (transaction amounts/dates/notes) | Yes (Account+Sync only) | No | App functionality |
| Precise/approximate location | No | — | — |
| Analytics/advertising IDs | No | — | — |

Notes for the form:
- **Is all user data encrypted in transit?** Yes (HTTPS/TLS to Supabase).
- **Can users request data deletion?** Yes — describe the in-app
  Settings → "Delete all data" flow, plus the contact email for full account
  deletion.
- **Account deletion link** (separate field in the Data safety form, not the
  privacy policy URL field): use
  `https://bobadronov.github.io/debt-tracker/privacy-policy.html#data-deletion`.
  That section states the app/developer name, spells out both deletion paths
  (in-app data-only vs. email for full account) as numbered steps, and lists
  what's deleted vs. retained with timing — the three things Play requires
  this link to cover.
- **Is data shared with third parties?** Supabase hosts the backend as your
  data processor (not a separate company using the data for its own
  purposes), which Play's model generally treats as *not* "sharing" — the
  same distinction GDPR draws between a processor and a third party. Confirm
  this reading against Play's current help text before answering.
- Local-only mode (no sign-in) collects and transmits nothing — worth
  stating in the form's free-text notes if there's room, since the table
  above only covers Account+Sync mode.

## 5. Release setup in Play Console

**First release ever (must be done by hand — the Play Developer API can't
create an app or do its first release):**

1. Start with **Internal testing** to try it privately first — recommended
   for a first submission.
2. Upload the `.aab` from the `debt-tracker-aab` artifact on your latest
   GitHub Release (built by `release.yml`'s `android-aab` job).
3. Play Console will offer **Play App Signing** — accept it (Google then
   re-signs your app for distribution using your uploaded `.aab` as the
   *upload* key; your existing keystore stays the *upload* key, not the
   final signing key, which is the standard/recommended setup).
4. Fill in the release notes, save, and **Submit for review**. First review
   typically takes anywhere from a few hours to a few days.

**Every release after that — automated via GitHub Actions:**

Once the app exists in Play Console, `release.yml` has a `play-store` job
that uploads the `.aab` for you on every tagged release (or via
`workflow_dispatch`, where you can pick the target track). It's skipped
until you set it up, one time:

1. In [Google Cloud Console](https://console.cloud.google.com/), create (or
   pick) a project, then create a **service account** for Play publishing.
2. Enable the **Google Play Android Developer API** for that project.
3. Generate a JSON key for the service account and download it.
4. In Play Console → **Users and permissions**, invite the service
   account's email, and grant it release permissions for this app (at
   minimum: view app info, manage releases to the tracks you'll use).
5. Copy the full contents of the JSON key file into a new repo secret named
   `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON` (GitHub → Settings → Secrets and
   variables → Actions).

After that, every `vX.Y.Z` tag push builds the `.aab` and uploads it to the
`production` track by default (change via the `play_track` input when running
the workflow manually, or use `publishing.bat`, which always dispatches with
`play_track=production`). Play Console still applies its staged-rollout /
review rules before the release actually goes live.

## 6. Recap: what's already done vs. what's left

**Done (this repo):**
- Signed `.aab` build wired into CI (`android-aab` job in `release.yml`)
- Automated Play Store upload (`play-store` job in `release.yml`), gated on
  the `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON` secret being configured
- Privacy policy page, deployed to GitHub Pages alongside the web app
- This packet: listing text, content rating guidance, data safety mapping

**Left for you:**
- Play Console account + app creation, and the manual first upload (§5)
- Service account + `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON` secret, to turn on
  automated uploads for every release after the first (§5)
- All graphic assets (icon, feature graphic, phone/7"/10" screenshots) are
  done, generated from the app's real design tokens — see §2. Swap in real
  device captures instead if you'd prefer that over the recreations.
- Walking through the content rating and data safety questionnaires in the
  live Play Console UI (I've drafted the answers, but only you can click
  through the actual forms)
- Reviewing/replacing the placeholder contact email
- Hitting Submit
