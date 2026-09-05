@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import java.util.Properties

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.room)
    alias(libs.plugins.ksp)
    alias(libs.plugins.buildConfig)
}

kotlin {
    android {
        namespace = "org.bigblackowl.debttracker"
        compileSdk = 37
        minSdk = 26 // Biometric API stability (spec §2)
        androidResources.enable = true
        compilerOptions { jvmTarget = JvmTarget.JVM_21 }
    }

    jvm {
        compilerOptions { jvmTarget = JvmTarget.JVM_21 }
    }

    js { browser() }
    wasmJs { browser() }

    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        // Custom intermediate source set (roomMain) triggers KGP's "explicit dependsOn" opt-out
        // of the default hierarchy template project-wide, so the FULL hierarchy below is wired
        // by hand (kotlin.mpp.applyDefaultHierarchyTemplate=false in gradle.properties).
        // Room publishes no js/wasmJs artifacts (spec §1 — Web has no local DB), so
        // Entity/DAO/Database live in roomMain (android/jvm/iosArm64/iosSimulatorArm64), not commonMain.
        val commonMain = getByName("commonMain")
        val commonTest = getByName("commonTest")

        val roomMain = create("roomMain") { dependsOn(commonMain) }
        // PdfKmp + pdfkmp-viewer publish no js/wasmJs artifacts (viewer is Android/iOS/Desktop-only,
        // core adds wasm-js but not plain js) — same android/jvm/iosMain membership as roomMain above,
        // so the PDF-building DSL code lives here once instead of copy-pasted per platform.
        val pdfMain = create("pdfMain") { dependsOn(commonMain) }

        // QRKit (qr-kit) publishes Android/JVM/iOS artifacts plus a wasmJs one whose QrScanner
        // (camera) actual is an empty no-op stub — scanning never fires there, so the feature is
        // kept out of webMain entirely rather than half-supporting it. Same android/jvm/ios
        // membership as roomMain/pdfMain above.
        val qrMain = create("qrMain") { dependsOn(commonMain) }

        val androidMain = getByName("androidMain") {
            dependsOn(roomMain)
            dependsOn(pdfMain)
            dependsOn(qrMain)
        }
        val jvmMain = getByName("jvmMain") {
            dependsOn(roomMain)
            dependsOn(pdfMain)
            dependsOn(qrMain)
        }

        val iosMain = create("iosMain") {
            dependsOn(roomMain)
            dependsOn(pdfMain)
            dependsOn(qrMain)
        }

        getByName("iosArm64Main") { dependsOn(iosMain) }
        getByName("iosSimulatorArm64Main") { dependsOn(iosMain) }

        val webMain = create("webMain") { dependsOn(commonMain) }
        getByName("jsMain") { dependsOn(webMain) }
        getByName("wasmJsMain") { dependsOn(webMain) }

        pdfMain.dependencies {
            implementation(libs.pdfkmp) // PDF export (spec §6, item 8) — vector DSL
            implementation(libs.pdfkmp.viewer) // in-app viewer screen (search/share/download)
        }

        qrMain.dependencies {
            implementation(libs.qr.kit) // contact-card QR generate/scan
        }

        roomMain.dependencies {
            implementation(libs.room.runtime)
            implementation(libs.androidx.sqlite.bundled)
        }

        commonMain.dependencies {
            api(libs.compose.runtime)
            api(libs.compose.ui)
            api(libs.compose.foundation)
            api(libs.compose.resources)
            api(libs.compose.ui.tooling.preview)
            api(libs.compose.material3)
            implementation(libs.material.icons.extended)
            implementation(libs.napier)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.serialization)
            implementation(libs.ktor.serialization.json)
            implementation(libs.ktor.client.logging)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime)
            implementation(libs.androidx.lifecycle.viewmodel.navigation3)
            implementation(libs.compose.nav3)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.coil)
            implementation(libs.coil.network.ktor)
            implementation(libs.multiplatformSettings)
            implementation(libs.kotlinx.datetime)
            implementation(libs.bignum)
            api(libs.filekit.dialogs.compose) // cross-platform file pick/save (avatar picker, CSV/PDF export); api so desktopApp's main() can call FileKit.init()
            implementation(libs.qrose) // contact-card QR display — pure Kotlin, unlike qr-kit publishes a real js/wasmJs target, so this needs no expect/actual
            // room-runtime is NOT here: Room has no js/wasmJs target (spec §1 — Web has no local DB).

            implementation(project.dependencies.platform(libs.supabase.bom))
            implementation(libs.supabase.auth)
            implementation(libs.supabase.postgrest)
            implementation(libs.supabase.realtime)
            implementation(libs.supabase.storage)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.compose.ui.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.multiplatformSettings.test) // MapSettings for AppSettings in ViewModel tests
        }

        androidMain.dependencies {
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.koin.android)
            implementation(libs.androidx.biometric)
            implementation(libs.androidx.fragment)
            implementation(libs.play.app.update.ktx)
            // Restore Credentials API (zero-tap sign-in on a new device) — see AndroidRestoreCredentialClient.
            implementation(libs.androidx.credentials)
            implementation(libs.androidx.credentials.play.services.auth)
            // Native "Sign in with Google" — GetGoogleIdOption for Credential Manager (see GoogleSignInLauncher.android.kt).
            implementation(libs.google.identity.googleid)
        }

        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.zxing.core) // decodes a QR code from a locally-picked image file (Desktop has no camera scanner)
            implementation(libs.nucleus.notification) // native OS notifications; falls back to a Compose toast (LocalNotifier.jvm.kt)
            implementation(libs.java.keyring) // OS credential store for the Supabase session-encryption key (see DesktopSessionKeyCipher)
        }

        webMain.dependencies {
            implementation(libs.nav3.browser)
            implementation(libs.ktor.client.js) // Realtime needs a WebSocket engine (Phase 10, not yet used)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }

    }

    targets
        .withType<KotlinNativeTarget>()
        .matching { it.konanTarget.family.isAppleFamily }
        .configureEach {
            binaries {
                framework {
                    baseName = "SharedUI"
                    isStatic = true
                }
            }
        }
}

// Supabase URL/key live in secrets.properties (git-ignored, see secrets.properties.example)
// so they aren't committed to VCS alongside the source.
val secretsFile = rootProject.file("secrets.properties")
val secrets = Properties().apply {
    if (secretsFile.exists()) secretsFile.inputStream().use { load(it) }
}

fun secret(key: String): String =
    (System.getenv(key) ?: secrets.getProperty(key))
        ?: error("Missing $key: define it in secrets.properties (see secrets.properties.example) or as an env var.")

// Like [secret] but tolerates a missing value (returns "") — for optional/feature-flagged config
// whose absence must not fail a build, e.g. GOOGLE_SERVER_CLIENT_ID before its CI secret is set.
fun optionalSecret(key: String): String =
    System.getenv(key) ?: secrets.getProperty(key) ?: ""

// Version is defined once in /version.properties and shared by every target (single source of truth) —
// androidApp/desktopApp read it for their own versionName/packageVersion, and it's exposed here as
// BuildConfig.APP_VERSION so every Compose target (Android, Desktop, Web, iOS) shows the same value
// on the Settings → About screen.
val versionProps = Properties().apply {
    rootProject.file("version.properties").inputStream().use { load(it) }
}

// Desktop/KMP has no real debug/release build variant (unlike Android's AGP), so this approximates
// it from which Gradle task is running: release.yml's packaging/publishing tasks vs. everything
// else (IDE run, ./gradlew build, tests). Gates verbose logging (see initKoin/Napier.base call).
// Compose Desktop's release build-type tasks all contain "Release" (packageReleaseMsi,
// packageReleaseDistributionForCurrentOS, createReleaseDistributable, ...), same as AGP's
// (assembleRelease, bundleRelease) — except release.yml's actual desktop packaging tasks, which
// invoke the default/"main" build type (packageMsi/packageDeb, no "Release" in the name).
val releaseTaskSuffixes = setOf(
    "packageMsi", "packageDeb", // desktopApp (release.yml's actual CI tasks)
    "composeCompatibilityBrowserDistribution", // webApp
)

val isDebugBuild = gradle.startParameter.taskNames.none { taskName ->
    taskName.contains("Release") || releaseTaskSuffixes.any { taskName.endsWith(it) }
}

buildConfig {
    packageName("org.bigblackowl.debttracker")
    // anon key is intentionally public (RLS protects the data, not the key itself) — standard Supabase practice.
    buildConfigField("SUPABASE_URL", secret("SUPABASE_URL"))
    buildConfigField("SUPABASE_ANON_KEY", secret("SUPABASE_ANON_KEY"))
    // Web OAuth client ID from Google Cloud Console — used as `serverClientId` for the Android
    // native Credential Manager flow (see GoogleSignInLauncher.android.kt). Public value, same
    // rationale as the anon key: it identifies the app, it doesn't authorize anything on its own.
    buildConfigField("GOOGLE_SERVER_CLIENT_ID", optionalSecret("GOOGLE_SERVER_CLIENT_ID"))
    buildConfigField("APP_VERSION", versionProps.getProperty("VERSION_NAME"))
    buildConfigField("APP_VERSION_CODE", versionProps.getProperty("VERSION_CODE").toInt())
    buildConfigField("APP_AUTHOR", "BigBlackOwl")
    buildConfigField("SOUND_ENABLED", false)
    buildConfigField("DEBUG", isDebugBuild)
    // Restore Credentials (zero-tap sign-in). OFF until its backend prerequisites are met:
    //  1. Supabase dashboard → Auth → enable the Passkeys provider (beta) with a Relying Party ID
    //     that is a real domain the app owns, plus its allowed origins.
    //  2. That domain must serve /.well-known/assetlinks.json with this app's signing-cert SHA-256
    //     (Android app-links / Digital Asset Links) so the platform trusts the RP.
    //  3. supabase-kt's @SupabaseExperimental passkey API must still be present (it is, 3.7.0).
    // Client scaffolding (RestoreCredentialCoordinator + AndroidRestoreCredentialClient) is wired
    // and compiled regardless — flip this to true once 1–2 are done.
    buildConfigField("RESTORE_CREDENTIALS_ENABLED", false)
    // "Continue with Google" on the auth screen. The whole flow (GoogleSignInLauncher + its
    // actuals, the button, the OAuth deep-link handling) compiles and links regardless of this.
    buildConfigField("GOOGLE_SIGN_IN_ENABLED", true)
}

room {
    schemaDirectory("$projectDir/schemas")
}

// qr-kit (contact-card QR generate/scan) transitively pulls cmp-image-pick-n-crop on JVM,
// which drags in javacv-platform/opencv-platform (native opencv/ffmpeg/openblas binaries for
// every OS+arch, ~900MB) for webcam-based capture, plus a hardcoded macos-arm64 compose-desktop
// copy (a duplicate skiko native lib). Per QR_SCAN_CAPABLE_PLATFORMS in QrScanSupport.kt, Desktop
// never offers camera scanning, so none of it is reachable at runtime — safe to drop from the
// packaged app. (KMP's per-sourceSet `implementation(libs.qr.kit) { exclude(...) }` doesn't type-check
// against the version catalog's Provider, so the exclude is applied here at the configuration level instead.)
configurations.matching { it.name.startsWith("jvm") }.configureEach {
    exclude(group = "org.bytedeco", module = "javacv-platform")
    exclude(group = "org.bytedeco", module = "opencv-platform")
    exclude(group = "org.jetbrains.compose.desktop", module = "desktop-jvm-macos-arm64")
}

dependencies {
    androidRuntimeClasspath(libs.compose.ui.tooling)

    // Room codegen only for roomMain's platforms (android/jvm/iosArm64/iosSimulatorArm64) — no js/wasmJs.
    for (target in listOf("kspAndroid", "kspJvm", "kspIosArm64", "kspIosSimulatorArm64")) {
        add(target, libs.room.compiler)
    }
}
