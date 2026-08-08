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
        minSdk = 26 // Biometric API stability (спек §2 / spec §2)
        androidResources.enable = true
        compilerOptions { jvmTarget = JvmTarget.JVM_17 }
    }

    jvm {
        compilerOptions { jvmTarget = JvmTarget.JVM_17 }
    }

    js { browser() }
    wasmJs { browser() }

    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        // Custom intermediate source set (roomMain) triggers KGP's "explicit dependsOn" opt-out
        // of the default hierarchy template project-wide, so the FULL hierarchy below is wired
        // by hand (kotlin.mpp.applyDefaultHierarchyTemplate=false in gradle.properties).
        // Room не публікує артефакти для js/wasmJs (спек §1 — Web без локальної БД), тому
        // Entity/DAO/Database живуть у roomMain (android/jvm/iosArm64/iosSimulatorArm64), не в commonMain.
        val commonMain by getting
        val commonTest by getting

        val roomMain by creating { dependsOn(commonMain) }

        val androidMain by getting { dependsOn(roomMain) }
        val jvmMain by getting { dependsOn(roomMain) }

        val iosMain by creating { dependsOn(roomMain) }
        val iosArm64Main by getting { dependsOn(iosMain) }
        val iosSimulatorArm64Main by getting { dependsOn(iosMain) }

        val webMain by creating { dependsOn(commonMain) }
        val jsMain by getting { dependsOn(webMain) }
        val wasmJsMain by getting { dependsOn(webMain) }

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
            // room-runtime НЕ тут: Room не підтримує js/wasmJs (спек §1 — Web без локальної БД).
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
        }

        androidMain.dependencies {
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.koin.android)
            implementation(libs.androidx.biometric)
            implementation(libs.androidx.fragment)
            implementation(libs.play.app.update.ktx)
        }

        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.pdfbox) // Desktop PDF export (спек §6, п.8) — растеризація сторінки, без embedding шрифтів
        }

        webMain.dependencies {
            implementation(libs.nav3.browser)
            implementation(libs.ktor.client.js) // Realtime потребує WebSocket-engine (Фаза 10, поки не використовується)
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

dependencies {
    androidRuntimeClasspath(libs.compose.ui.tooling)
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

// Version is defined once in /version.properties and shared by every target (спек: single source of truth) —
// androidApp/desktopApp read it for their own versionName/packageVersion, and it's exposed here as
// BuildConfig.APP_VERSION so every Compose target (Android, Desktop, Web, iOS) shows the same value
// on the Settings → About screen.
val versionProps = Properties().apply {
    rootProject.file("version.properties").inputStream().use { load(it) }
}

buildConfig {
    packageName("org.bigblackowl.debttracker")
    // anon key навмисно публічний (RLS захищає дані, не сам ключ) — стандартна практика Supabase.
    // anon key is intentionally public (RLS protects the data, not the key itself) — standard Supabase practice.
    buildConfigField("SUPABASE_URL", secret("SUPABASE_URL"))
    buildConfigField("SUPABASE_ANON_KEY", secret("SUPABASE_ANON_KEY"))
    buildConfigField("APP_VERSION", versionProps.getProperty("VERSION_NAME"))
    buildConfigField("APP_VERSION_CODE", versionProps.getProperty("VERSION_CODE").toInt())
    buildConfigField("APP_AUTHOR", "BigBlackOwl")
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    with(libs.room.compiler) {
        add("kspAndroid", this)
        add("kspJvm", this)
        add("kspIosArm64", this)
        add("kspIosSimulatorArm64", this)
    }
}
