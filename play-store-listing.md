# DebtTracker — Google Play submission packet

Everything needed to submit DebtTracker to Google Play, gathered in one
place. The parts that need *you* (an account, judgment calls, actually
clicking "Submit") are marked accordingly — I can't do those.

## 0. What only you can do (before anything else)

1. Create a [Google Play Console](https://play.google.com/console/) developer
   account — one-time $25 fee, government ID verification, accept the
   Developer Distribution Agreement. Takes Google anywhere from a few hours
   to a couple of days to approve.
2. In Play Console: **Create app** → name it, pick default language
   (`en-US`), pick "App" (not game), pick "Free". This locks in the package
   name (`org.bigblackowl.debttracker.androidApp`, from the `.aab` you
   upload) — it can never change after your first upload, so double-check it
   here.

## 1. Store listing text

**App name** (30 char max): `Debt Tracker`

**App category:** Finance

**Contact email:** bobadronov@gmail.com *(using the account email as a
placeholder — swap it for whatever public support address you want; once
published this is visible to every user)*

**Website:** https://bobadronov.github.io/debt-tracker/

**Privacy policy URL:** https://bobadronov.github.io/debt-tracker/privacy-policy.html
*(deployed by `release.yml`'s web job from [`legal/privacy-policy.html`](legal/privacy-policy.html) on every tagged release)*

---

The app ships 10 UI languages (`sharedUI/.../core/i18n/translate/`). Below is
the store listing in each, keyed by its Play Console locale. Add each locale
under **Store presence → Main store listing → Manage translations**; `en-US`
is the default. Every short description is ≤ 80 chars and every full
description ≤ 4000 chars — paste as-is.

### en-US (default)

**Short description:**
```
Track who owes you and who you owe — offline-first, optional cloud sync.
```

**Full description:**
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
• Available in 10 languages
• Search, sort, filter, swipe-to-delete
• No ads. No trackers. No analytics SDKs.

Your data stays on your device unless you choose to create an account for
sync — see the privacy policy for exactly what that involves.
```

### uk-UA

**Short description:**
```
Стежте, хто винен вам і кому винні ви — офлайн, синхронізація за бажанням.
```

**Full description:**
```
DebtTracker веде облік грошей в обидва боки — хто винен вам, і кому винні ви — не згортаючи одне в інше.

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
• Доступно 10 мовами
• Пошук, сортування, фільтри, свайп для видалення
• Без реклами. Без трекерів. Без аналітичних SDK.

Ваші дані залишаються на пристрої, якщо ви не створите акаунт для синхронізації — детальніше в політиці конфіденційності.
```

### de-DE

**Short description:**
```
Wer schuldet dir Geld, wem schuldest du — offline, Cloud-Sync optional.
```

**Full description:**
```
Debt Tracker verfolgt geliehenes Geld in beide Richtungen — wer dir etwas schuldet und wem du etwas schuldest — ohne beides gegeneinander aufzurechnen.

FUNKTIONEN
• Zwei unabhängige Listen: Leute, die dir schulden, und Leute, denen du schuldest
• Jede Leihgabe und Rückzahlung als eigene Transaktion mit laufendem Saldo
• Mehrere Währungen: UAH, USD, PLN, EUR — pro Kontakt
• Funktioniert komplett offline — kein Konto nötig
• Optionales kostenloses Konto für Echtzeit-Sync über alle deine Geräte
• Kontakte per QR-Code hinzufügen — kein Abtippen von Namen oder Nummern
• App-Sperre per Fingerabdruck, Gesichtsentsperrung oder PIN
• Statistik: Summen, größte Schuldner/Gläubiger, 6-Monats-Trend
• Transaktionsverlauf als CSV oder PDF exportieren
• Homescreen-Widget mit deinen beiden laufenden Summen
• In 10 Sprachen verfügbar
• Suchen, sortieren, filtern, zum Löschen wischen
• Keine Werbung. Kein Tracking. Keine Analytics-SDKs.

Deine Daten bleiben auf deinem Gerät, es sei denn, du legst ein Konto für die Synchronisierung an — Details in der Datenschutzerklärung.
```

### es-ES

**Short description:**
```
Controla quién te debe y a quién debes — sin conexión, sync opcional.
```

**Full description:**
```
Debt Tracker lleva la cuenta del dinero prestado en ambos sentidos — quién te debe y a quién debes — sin compensar lo uno con lo otro.

FUNCIONES
• Dos listas independientes: quienes te deben y a quienes debes
• Cada préstamo y devolución como transacción propia, con saldo actualizado
• Varias monedas: UAH, USD, PLN, EUR, por contacto
• Funciona totalmente sin conexión — no hace falta cuenta
• Cuenta gratuita opcional para sincronizar entre tus dispositivos en tiempo real
• Añade contactos escaneando un código QR — sin teclear nombres ni números
• Bloqueo de la app con huella, cara o PIN
• Pantalla de estadísticas: totales, mayores deudores/acreedores, tendencia de 6 meses
• Exporta el historial de transacciones a CSV o PDF
• Widget en la pantalla de inicio con tus dos totales
• Disponible en 10 idiomas
• Buscar, ordenar, filtrar, deslizar para borrar
• Sin anuncios. Sin rastreadores. Sin SDK de analíticas.

Tus datos se quedan en tu dispositivo salvo que crees una cuenta para sincronizar — consulta la política de privacidad para los detalles.
```

### fr-FR

**Short description:**
```
Qui vous doit, à qui vous devez — hors ligne, synchro cloud en option.
```

**Full description:**
```
Debt Tracker suit l'argent prêté dans les deux sens — qui vous doit et à qui vous devez — sans compenser l'un par l'autre.

FONCTIONNALITÉS
• Deux listes indépendantes : ceux qui vous doivent et ceux à qui vous devez
• Chaque prêt et remboursement comme transaction distincte, avec solde courant
• Plusieurs devises : UAH, USD, PLN, EUR, par contact
• Fonctionne entièrement hors ligne — aucun compte requis
• Compte gratuit facultatif pour synchroniser vos appareils en temps réel
• Ajoutez des contacts en scannant un QR code — sans saisir noms ni numéros
• Verrouillage par empreinte, reconnaissance faciale ou code PIN
• Écran de statistiques : totaux, principaux débiteurs/créanciers, tendance sur 6 mois
• Exportez l'historique des transactions en CSV ou PDF
• Widget d'écran d'accueil affichant vos deux totaux
• Disponible en 10 langues
• Recherche, tri, filtres, balayer pour supprimer
• Pas de publicité. Pas de traceurs. Pas de SDK d'analytics.

Vos données restent sur votre appareil sauf si vous créez un compte pour la synchronisation — voir la politique de confidentialité pour le détail.
```

### it-IT

**Short description:**
```
Chi ti deve e a chi devi — offline, sincronizzazione cloud opzionale.
```

**Full description:**
```
Debt Tracker tiene traccia del denaro prestato in entrambe le direzioni — chi deve a te e a chi devi tu — senza compensare l'uno con l'altro.

FUNZIONI
• Due elenchi indipendenti: chi deve a te e a chi devi tu
• Ogni prestito e restituzione come transazione a sé, con saldo aggiornato
• Più valute: UAH, USD, PLN, EUR, per contatto
• Funziona completamente offline — nessun account richiesto
• Account gratuito opzionale per sincronizzare i tuoi dispositivi in tempo reale
• Aggiungi contatti scansionando un codice QR — senza digitare nomi o numeri
• Blocco dell'app con impronta, volto o PIN
• Schermata statistiche: totali, principali debitori/creditori, andamento a 6 mesi
• Esporta lo storico delle transazioni in CSV o PDF
• Widget nella schermata Home con i tuoi due totali
• Disponibile in 10 lingue
• Cerca, ordina, filtra, scorri per eliminare
• Niente pubblicità. Niente tracker. Niente SDK di analytics.

I tuoi dati restano sul dispositivo a meno che tu non crei un account per la sincronizzazione — vedi l'informativa sulla privacy per i dettagli.
```

### nl-NL

**Short description:**
```
Wie is jou geld schuldig en wie ben jij — offline, sync optioneel.
```

**Full description:**
```
Debt Tracker houdt geleend geld in beide richtingen bij — wie is jou iets schuldig en wie ben jij iets schuldig — zonder het tegen elkaar weg te strepen.

FUNCTIES
• Twee onafhankelijke lijsten: wie is jou schuldig en wie ben jij schuldig
• Elke lening en terugbetaling als eigen transactie, met lopend saldo
• Meerdere valuta's: UAH, USD, PLN, EUR, per contact
• Werkt volledig offline — geen account nodig
• Optioneel gratis account om je apparaten in realtime te synchroniseren
• Voeg contacten toe door een QR-code te scannen — geen namen of nummers typen
• App-vergrendeling met vingerafdruk, gezicht of pincode
• Statistiekenscherm: totalen, grootste debiteuren/crediteuren, trend over 6 maanden
• Exporteer transactiegeschiedenis naar CSV of PDF
• Widget op het startscherm met je twee lopende totalen
• Beschikbaar in 10 talen
• Zoeken, sorteren, filteren, vegen om te verwijderen
• Geen advertenties. Geen trackers. Geen analytics-SDK's.

Je gegevens blijven op je apparaat tenzij je een account aanmaakt om te synchroniseren — zie het privacybeleid voor de details.
```

### pl-PL

**Short description:**
```
Śledź, kto jest winien Tobie i komu winien Ty — offline, synchronizacja online.
```

**Full description:**
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
• Dostępne w 10 językach
• Wyszukiwanie, sortowanie, filtry, przesunięcie do usunięcia
• Bez reklam. Bez trackerów. Bez SDK analitycznych.

Twoje dane pozostają na urządzeniu, chyba że założysz konto do synchronizacji — szczegóły w polityce prywatności.
```

### pt-PT

**Short description:**
```
Controla quem te deve e a quem deves — offline, sincronização opcional.
```

**Full description:**
```
O Debt Tracker acompanha o dinheiro emprestado nos dois sentidos — quem te deve e a quem deves — sem compensar um com o outro.

FUNCIONALIDADES
• Duas listas independentes: quem te deve e a quem deves
• Cada empréstimo e devolução como transação própria, com saldo atualizado
• Várias moedas: UAH, USD, PLN, EUR, por contacto
• Funciona totalmente offline — não é preciso conta
• Conta gratuita opcional para sincronizar os teus dispositivos em tempo real
• Adiciona contactos ao ler um código QR — sem escrever nomes nem números
• Bloqueio da app por impressão digital, rosto ou PIN
• Ecrã de estatísticas: totais, maiores devedores/credores, tendência de 6 meses
• Exporta o histórico de transações para CSV ou PDF
• Widget no ecrã principal com os teus dois totais
• Disponível em 10 idiomas
• Pesquisar, ordenar, filtrar, deslizar para eliminar
• Sem anúncios. Sem rastreadores. Sem SDK de analítica.

Os teus dados ficam no dispositivo a não ser que cries uma conta para sincronizar — consulta a política de privacidade para os detalhes.
```

### cs-CZ

**Short description:**
```
Kdo dluží tobě a komu dlužíš ty — offline, volitelná synchronizace.
```

**Full description:**
```
Debt Tracker sleduje půjčené peníze v obou směrech — kdo dluží tobě a komu dlužíš ty — aniž by je vzájemně započítával.

FUNKCE
• Dva nezávislé seznamy: kdo dluží tobě a komu dlužíš ty
• Každá půjčka a splátka jako vlastní transakce s průběžným zůstatkem
• Více měn: UAH, USD, PLN, EUR, pro každý kontakt
• Funguje zcela offline — účet není potřeba
• Volitelný účet zdarma pro synchronizaci zařízení v reálném čase
• Přidávej kontakty naskenováním QR kódu — bez psaní jmen a čísel
• Zámek aplikace otiskem prstu, obličejem nebo PINem
• Obrazovka statistik: součty, největší dlužníci/věřitelé, trend za 6 měsíců
• Export historie transakcí do CSV nebo PDF
• Widget na domovské obrazovce se dvěma průběžnými součty
• K dispozici v 10 jazycích
• Hledání, řazení, filtry, přejetí pro smazání
• Žádné reklamy. Žádné trackery. Žádné analytické SDK.

Tvá data zůstávají v zařízení, dokud si nevytvoříš účet pro synchronizaci — podrobnosti v zásadách ochrany soukromí.
```

## 2. Graphic assets

| Asset | Spec | Status |
|---|---|---|
| App icon | 512×512 PNG, 32-bit with alpha, <1 MB | Ready — [`webApp/src/commonMain/resources/android-chrome-512x512.png`](webApp/src/commonMain/resources/android-chrome-512x512.png) |
| Feature graphic | 1024×500 PNG/JPG (no alpha) | **Needed** — not in the repo |
| Phone screenshots | 2–8 images, PNG/JPEG, 16:9 or 9:16, side 320–3840 px | **Needed** — 2 min. |
| 7" tablet screenshots | up to 8 images, same format, side 320–3840 px | Optional |
| 10" tablet screenshots | up to 8 images, same format, side 1080–7680 px | Optional |

Only the app icon is present. The feature graphic and screenshots need to be
produced — this machine has no Android SDK/emulator to capture real screens
from. Two ways to get them:

- **Real captures:** run the app on a device/emulator
  (`./gradlew :androidApp:installDebug`) and screenshot the debtor list,
  creditor list, stats, a contact's transaction history, and settings.
- **Mockups:** render frames from the shared design tokens
  (`core/i18n/Strings.kt` for copy, `sharedUI/.../theme/Color.kt` for the
  palette) — Play accepts non-photographic promo screenshots as long as they
  represent the app.

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
   re-signs your app for distribution; your uploaded keystore stays the
   *upload* key, not the final signing key, which is the
   standard/recommended setup).
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

### Countries / regions — releasing everywhere

Country availability is **Play Console only** — the Play Developer API and
the `r0adkll/upload-google-play` action used in `release.yml` don't manage
it, so there's nothing to change in this repo. The app's 10 UI languages
don't restrict availability: a listing shows in every selected country
regardless of which languages the app translates. "All countries" just means
ticking all ~170.
[Google's guide.](https://support.google.com/googleplay/android-developer/answer/6223646)

**Step 1 — clear the global declarations first** (production won't publish
worldwide until every row here is green): **Policy and programs → App
content**.

| Section | Answer for DebtTracker |
|---|---|
| Privacy policy | `https://bobadronov.github.io/debt-tracker/privacy-policy.html` |
| Ads | **No**, this app does not contain ads |
| App access | All functionality available without special access *(the Android build needs no login; the Web AuthGate doesn't apply to the APK/AAB Play reviews)* |
| Content ratings | Complete the IARC questionnaire — see §3 |
| Target audience and content | Not designed for children (13+ / 18+) |
| Data safety | Complete the form — see §4 |
| Financial features | **"My app doesn't provide any financial features"** — it *records* debts, it doesn't lend, transfer, or process money. A wrong answer here gets the release rejected. |
| Government apps | **No** |
| Health / other prompts | Answer as they appear; none apply |

**Step 2 — store listing must be complete** (nothing publishes at all
without it): app icon (ready), **feature graphic 1024×500**, and **at least
2 phone screenshots** — see §2.

**Step 3 — select the countries:**
`Test and release → Production → open the latest release → Countries /
regions tab → Add countries / regions → tick "Select all" at the top of the
list → Add → Save`. Each track (Internal / Closed / Open / Production) keeps
its own country list, so set it on the track you're publishing.

**Step 4 — apply it:** an availability change only goes out **with a
release**. Create or update a Production release, roll it out, and Google
review follows (a few hours to a few days). Before the first publish, the
selection just takes effect at launch.

**Countries that may need extra compliance** — for a *free* app with no
payments most don't apply, but Play may still gate a few and show a warning
if so:

- **Brazil** — developer business/merchant details verification.
- **South Korea, Vietnam, India, Israel, Japan** — region-specific
  compliance docs (usually games/fintech; unlikely here).

If a country demands something you can't provide, untick just that country
and ship the rest. No per-country tax/pricing setup is needed (free app, no
in-app purchases).

## 6. Recap: what's already done vs. what's left

**Done (this repo):**
- Signed `.aab` build wired into CI (`android-aab` job in `release.yml`)
- Automated Play Store upload (`play-store` job in `release.yml`), gated on
  the `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON` secret being configured
- Privacy policy page, deployed to GitHub Pages alongside the web app
- This packet: listing text in all 10 languages, content rating guidance,
  data safety mapping, full country-rollout steps (§5)
- App icon (512×512)

**Left for you:**
- Play Console account + app creation, and the manual first upload (§5)
- Service account + `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON` secret, to turn on
  automated uploads for every release after the first (§5)
- Feature graphic + at least 2 phone screenshots (§2)
- Clearing the App content declarations, then walking the content rating and
  data safety questionnaires in the live Play Console UI (answers drafted
  above, but only you can click through the actual forms)
- Selecting all countries/regions and rolling out a release to apply it (§5)
- Reviewing/replacing the placeholder contact email
- Hitting Submit
