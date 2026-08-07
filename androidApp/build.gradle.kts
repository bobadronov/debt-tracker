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

    // Release signing comes from env vars (CI secrets) — never hardcoded/committed.
    // Missing/blank env vars → release build stays unsigned, it just doesn't fail
    // the build (matches Android Gradle Plugin's default behavior for `assembleRelease`
    // with no signingConfig).
    val releaseKeystorePath = System.getenv("ANDROID_KEYSTORE_PATH")
    val releaseKeystorePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
    val releaseKeyAlias = System.getenv("ANDROID_KEY_ALIAS")
    val releaseKeyPassword = System.getenv("ANDROID_KEY_PASSWORD")
    val hasReleaseSigning = !releaseKeystorePath.isNullOrBlank() &&
        !releaseKeystorePassword.isNullOrBlank() &&
        !releaseKeyAlias.isNullOrBlank() &&
        !releaseKeyPassword.isNullOrBlank()

    if (hasReleaseSigning) {
        signingConfigs {
            create("release") {
                storeFile = file(releaseKeystorePath!!)
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

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

dependencies {
    implementation(project(":sharedUI"))
    implementation(libs.androidx.activityCompose)
    implementation(libs.koin.android)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.bignum) // DebtSummaryWidget рахує BigDecimal-суми напряму
}
