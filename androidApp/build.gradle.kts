import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.android.application)
}

// Version is defined once in /version.properties and shared by every target (спек: single source of truth).
val versionProps = Properties().apply {
    rootProject.file("version.properties").inputStream().use { load(it) }
}
val appVersionName: String = versionProps.getProperty("VERSION_NAME")
val appVersionCode: Int = versionProps.getProperty("VERSION_CODE").toInt()

// Release signing comes from env vars (CI secrets) — never hardcoded/committed.
// Missing/blank env vars → release build stays unsigned locally, but fails fast in CI
// (see the task-execution check below `android {}`) instead of silently shipping unsigned.
val releaseKeystorePath = System.getenv("ANDROID_KEYSTORE_PATH")
val releaseKeystorePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
val releaseKeyAlias = System.getenv("ANDROID_KEY_ALIAS")
val releaseKeyPassword = System.getenv("ANDROID_KEY_PASSWORD")
val hasReleaseSigning = !releaseKeystorePath.isNullOrBlank() &&
        !releaseKeystorePassword.isNullOrBlank() &&
        !releaseKeyAlias.isNullOrBlank() &&
        !releaseKeyPassword.isNullOrBlank()

android {
    namespace = "org.bigblackowl.debttracker.androidApp"
    compileSdk = 37

    defaultConfig {
        minSdk = 26 // Biometric API stability (спек §2 / spec §2)
        targetSdk = 37

        applicationId = "org.bigblackowl.debttracker.androidApp"
        versionCode = appVersionCode
        versionName = appVersionName
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    if (hasReleaseSigning) {
        signingConfigs {
            create("release") {
                storeFile = file(releaseKeystorePath)
                storePassword = releaseKeystorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
}

// Checked at task-execution time (not above, during `android {}` configuration) because Gradle
// configures every module in this multi-project build regardless of which task was requested —
// an eager check there would fail CI builds of desktopApp/webApp too, which never touch Android
// signing at all.
if (!hasReleaseSigning && System.getenv("CI") == "true") {
    tasks.matching { it.name == "assembleRelease" || it.name == "bundleRelease" }.configureEach {
        doFirst {
            throw GradleException(
                "Release signing is required in CI. " +
                        "Configure ANDROID_KEYSTORE_PATH, " +
                        "ANDROID_KEYSTORE_PASSWORD, " +
                        "ANDROID_KEY_ALIAS and ANDROID_KEY_PASSWORD."
            )
        }
    }
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

dependencies {
    implementation(project(":sharedUI"))
    implementation(libs.androidx.activityCompose)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.koin.android)
    implementation(libs.androidx.glance.appwidget)
    debugImplementation(libs.androidx.glance.preview)
    debugImplementation(libs.androidx.glance.appwidget.preview)
    debugImplementation(libs.compose.ui.tooling) // ComposeViewAdapter — потрібен рендереру Preview-панелі в IDE
    implementation(libs.bignum) // DebtSummaryWidget рахує BigDecimal-суми напряму
    implementation(libs.qr.kit) // AppContext.set(...) in DebtTrackerApplication (QRKit setup requirement)
}
