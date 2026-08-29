# Restore Credentials — remaining setup

Zero-tap sign-in on a new device (2026 Play "secure device migration" standard, enforced
**April 2027**). The client is fully implemented and compiled but **disabled** behind
`BuildConfig.RESTORE_CREDENTIALS_ENABLED` (`sharedUI/build.gradle.kts`) until the steps below
are done. Flip that flag to `true` in the same commit that finishes step 2.

## How it works

`Restore Credentials` is a passkey/FIDO2 feature — it needs a WebAuthn **relying party**. We use
**Supabase Auth passkeys** (beta, GA pending) as the RP:

| When | Client | Server |
|---|---|---|
| After password sign-in | `AndroidRestoreCredentialClient.createRestoreKey()` → Credential Manager mints a Block-Store-backed restore key | `client.auth.passkeys.startRegistration()` / `verifyRegistration()` stores the public key |
| First launch on a new device (before the auth gate) | `getRestoreAssertion()` returns an assertion for the restored key, or `null` | `startAuthentication()` / `verifyAuthentication()` verifies it and returns a `UserSession` |
| Sign-out | `clearRestoreKey()` (`TYPE_CLEAR_RESTORE_CREDENTIAL`) | `client.auth.passkeys.delete()` *(not yet wired — see TODO)* |

Code: `core/security/RestoreCredentialClient.kt` (+ `AndroidRestoreCredentialClient`),
`data/remote/RestoreCredentialCoordinator.kt`, `domain/repository/RestoreCredentialGateway.kt`.
Wired into `AuthViewModel` (register), `SplashViewModel` (restore, 4 s cap), `ForceSignOutUseCase`
(clear). No-op on iOS/Desktop/Web via `UnsupportedRestoreCredentialClient`.

## Step 1 — enable Supabase Auth passkeys

Supabase dashboard → project `nywvasgnbgnixfjzadbu` → **Authentication → Passkeys** (beta):

- **Relying Party ID**: a bare domain you control, e.g. `debttracker.app` (no scheme, no path).
- **Relying Party Display Name**: `Debt Tracker`.
- **Relying Party Origins**: `https://debttracker.app` (and any web-app origin).

There is no domain configured for the app today (`url.txt` points at an unrelated flag API).
You must register/own a domain first.

## Step 2 — Android app-links / Digital Asset Links

The platform will only hand a restore credential to an app whose package is verified against the
RP ID domain.

1. Add to `androidApp/src/main/AndroidManifest.xml` inside the launcher `<activity>` (or a
   dedicated one), replacing the domain:

   ```xml
   <intent-filter android:autoVerify="true">
       <action android:name="android.intent.action.VIEW" />
       <category android:name="android.intent.category.DEFAULT" />
       <category android:name="android.intent.category.BROWSABLE" />
       <data android:scheme="https" android:host="debttracker.app" />
   </intent-filter>
   ```

2. Host `https://debttracker.app/.well-known/assetlinks.json` (served as
   `application/json`, HTTP 200, no redirect):

   ```json
   [{
     "relation": ["delegate_permission/common.handle_all_urls",
                  "delegate_permission/common.get_login_creds"],
     "target": {
       "namespace": "android_app",
       "package_name": "org.bigblackowl.debttracker.androidApp",
       "sha256_cert_fingerprints": ["<PLAY_APP_SIGNING_SHA256>"]
     }
   }]
   ```

   `<PLAY_APP_SIGNING_SHA256>`: Play Console → **Test and release → Setup → App signing** →
   *App signing key certificate* SHA-256. Add the *upload key* SHA-256 as a second entry too so
   internal-testing builds verify. Local `get`:
   `keytool -list -v -keystore <release.keystore> -alias <alias>`.

3. Verify: `adb shell pm verify-app-links --re-verify org.bigblackowl.debttracker.androidApp`
   then `adb shell pm get-app-links org.bigblackowl.debttracker.androidApp` → domain `verified`.

## Step 3 — flip the flag & ship

`buildConfigField("RESTORE_CREDENTIALS_ENABLED", true)` in `sharedUI/build.gradle.kts`.
Requirements at runtime: Android 9+, Google Play services ≥ 24220000.

## TODO when enabling

- Wire `client.auth.passkeys.delete(passkeyId)` into `RestoreCredentialCoordinator.clear()` (need
  to persist the passkey id from `verifyRegistration`) so server-side keys don't accumulate.
- Decide whether to also register a restore key on `SplashViewModel` restore success for users who
  signed in before the feature shipped (currently only set on an explicit sign-in).
- Manual test matrix: fresh install + cloud restore, D2D transfer, sign-out clears key,
  `E2eeUnavailableException` fallback (device with no screen lock).
