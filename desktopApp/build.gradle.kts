import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.util.Properties

plugins {
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.jvm)
}

// Version is defined once in /version.properties and shared by every target (спек: single source of truth).
val versionProps = Properties().apply {
    rootProject.file("version.properties").inputStream().use { load(it) }
}
val appVersionName: String = versionProps.getProperty("VERSION_NAME")

dependencies {
    implementation(project(":sharedUI"))
    implementation(libs.koin.core) // main.kt звертається до KoinApplication.koin.get<SyncCoordinator>()
}

compose.desktop {
    application {
        mainClass = "MainKt"

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
            }

            macOS {
                iconFile.set(project.file("src/main/resources/appIcons/MacosIcon.icns"))
                bundleID = "org.bigblackowl.debttracker.desktopApp"
            }

        }
    }
}
