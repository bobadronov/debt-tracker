import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    compilerOptions { jvmTarget = JvmTarget.JVM_21 }
}

// kotlin("jvm") also applies the Java plugin; keep its compileJava target in step
// with Kotlin's (the Gradle daemon runs JDK 25, so compileJava would otherwise
// default to 25 and trip JVM-target validation). jlink still bundles the daemon JDK.
java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

// Version is defined once in /version.properties and shared by every target (спек: single source of truth).
val versionProps = Properties().apply {
    rootProject.file("version.properties").inputStream().use { load(it) }
}
val appVersionName: String = versionProps.getProperty("VERSION_NAME")

dependencies {
    implementation(project(":sharedUI"))
    implementation(libs.koin.core) // main.kt звертається до KoinApplication.koin.get<SyncCoordinator>()
    implementation(libs.compose.native.tray) // HDPI-correct system tray with icon-capable menu items (main.kt)
    implementation(libs.material.icons.extended) // ImageVector icons for the tray menu (main.kt's TrayMenu)

    // Nucleus Tao windowing backend + Material 3 native window decorations (main.kt's MaterialDecoratedWindow).
    implementation(libs.nucleus.application)
    implementation(libs.nucleus.window.tao)
    implementation(libs.nucleus.window.material3)
}

compose.desktop {
    application {
        mainClass = "MainKt"

        // Room's BundledSQLiteDriver loads its native lib via System.loadLibrary
        // (DatabaseBuilder.jvm.kt). Under the JDK 25 toolchain this project's
        // Gradle daemon uses (gradle/gradle-daemon-jvm.properties) that triggers a
        // "restricted method ... has been called" warning, and a future JDK will
        // block it outright. Grant native access up front — applies to both
        // `:desktopApp:run` and the packaged app's bundled (jlink'd) runtime.
        jvmArgs += listOf("--enable-native-access=ALL-UNNAMED")

        buildTypes {
            release {
                // ProGuard 7.8.0 can't resolve java.lang.Object under the JDK 25 toolchain this
                // project's Gradle daemon uses (fails with "can't find superclass ... Object" on
                // every class) — disabled until upstream JDK 25 support lands. Release packaging
                // still runs jlink to trim the bundled runtime, just without bytecode minification.
                proguard {
                    isEnabled.set(false)
                }
            }
        }

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Debt Tracker"
            packageVersion = appVersionName

            linux {
                iconFile.set(project.file("src/main/resources/appIcons/LinuxIcon.png"))
                modules("jdk.security.auth") // FileKit's Linux (XDG portal/D-Bus) file dialogs need this jlink module
            }

            windows {
                iconFile.set(project.file("src/main/resources/appIcons/WindowsIcon.ico"))
                shortcut = true
                dirChooser= false
                // Pinned so MSI upgrades replace the previous installation instead of
                // stacking side by side. Keep in sync with the same UUID in
                // .github/workflows/release.yml's jpackage --win-upgrade-uuid.
                upgradeUuid = "7b9d5c61-9805-50a2-a4da-3033c222b695"
            }

            macOS {
                iconFile.set(project.file("src/main/resources/appIcons/MacosIcon.icns"))
                bundleID = "org.bigblackowl.debttracker.desktopApp"
            }

        }
    }
}
