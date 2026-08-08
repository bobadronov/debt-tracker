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
• App lock via fingerprint/face unlock or PIN
• Stats screen: totals, top debtors/creditors, 6-month trend
• Export transaction history to CSV or PDF
• Home-screen widget showing your two running totals
• Search, sort, filter, swipe-to-delete
• No ads. No trackers. No analytics SDKs.

Your data stays on your device unless you choose to create an account for
sync — see the privacy policy for exactly what that involves.
```

**App category:** Finance

**Contact email:** bobadronov@gmail.com *(using the account email as a
placeholder — swap it for whatever public support address you want; once
published this is visible to every user)*

**Website:** https://bobadronov.github.io/debt-tracker/

**Privacy policy URL:** https://bobadronov.github.io/debt-tracker/privacy-policy.html
*(now wired into `release.yml`'s web job — goes live on the next tagged
release; until then this URL 404s)*

## 2. Graphic assets — you'll need to produce these

I can't generate real device screenshots or original artwork. Play requires:

| Asset | Spec |
|---|---|
| App icon | 512×512 PNG, 32-bit with alpha, <1MB |
| Feature graphic | 1024×500 PNG/JPG (no alpha) |
| Phone screenshots | 2–8 images, JPG/PNG, 16:9 or 9:16, each side 320–3840px |

Easiest path for screenshots: run the debug APK on a device or emulator
(`./gradlew :androidApp:installDebug`) and capture the debtor list, a
detail screen, stats, and settings. Android Studio's emulator has a
built-in screenshot button that saves at a Play-ready resolution.

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
- **Is data shared with third parties?** Supabase hosts the backend as your
  data processor (not a separate company using the data for its own
  purposes), which Play's model generally treats as *not* "sharing" — the
  same distinction GDPR draws between a processor and a third party. Confirm
  this reading against Play's current help text before answering.
- Local-only mode (no sign-in) collects and transmits nothing — worth
  stating in the form's free-text notes if there's room, since the table
  above only covers Account+Sync mode.

## 5. Release setup in Play Console

1. **Production** (or start with **Internal testing** to try it privately
   first — recommended for a first submission).
2. Upload the `.aab` from the `debt-tracker-aab` artifact on your latest
   GitHub Release (built by `release.yml`'s `android-aab` job).
3. Play Console will offer **Play App Signing** — accept it (Google then
   re-signs your app for distribution using your uploaded `.aab` as the
   *upload* key; your existing keystore stays the *upload* key, not the
   final signing key, which is the standard/recommended setup).
4. Fill in the release notes, save, and **Submit for review**. First review
   typically takes anywhere from a few hours to a few days.

## 6. Recap: what's already done vs. what's left

**Done (this repo):**
- Signed `.aab` build wired into CI (`android-aab` job in `release.yml`)
- Privacy policy page, deployed to GitHub Pages alongside the web app
- This packet: listing text, content rating guidance, data safety mapping

**Left for you:**
- Play Console account + app creation
- App icon, feature graphic, and real device screenshots
- Walking through the content rating and data safety questionnaires in the
  live Play Console UI (I've drafted the answers, but only you can click
  through the actual forms)
- Reviewing/replacing the placeholder contact email
- Hitting Submit
