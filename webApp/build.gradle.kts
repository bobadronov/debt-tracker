@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
}

kotlin {
    // WASM only — no js (non-WASM) fallback. WASM has been supported by every major browser
    // since 2017; shipping both targets was doubling webApp's deploy size (~18MB extra) and CI
    // build time/memory for a fallback path essentially nobody hits.
    wasmJs {
        browser()
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":sharedUI"))
            implementation(libs.koin.core) // main.kt calls KoinApplication (initKoin())
        }
    }
}
