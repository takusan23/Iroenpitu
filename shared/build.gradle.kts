import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    android {
        namespace = "io.github.takusan23.iroenpitushared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
        androidResources {
            enable = true
        }
        withHostTest {
            isIncludeAndroidResources = true
        }
    }

    wasmJs { browser() }

    sourceSets {
        androidMain.dependencies {
            // Activity Result API
            implementation(libs.androidx.activity.compose)

            // Ktor Android Impl
            implementation("io.ktor:ktor-client-okhttp:3.2.3")

            // DataStore
            implementation("androidx.datastore:datastore-preferences:1.1.7")
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            // HTTP Client
            implementation("io.ktor:ktor-client-core:3.2.3")

            // calc Hash
            implementation(project.dependencies.platform("org.kotlincrypto.hash:bom:0.7.0"))
            implementation("org.kotlincrypto.hash:sha2")

            // calc Hmac-Hadh
            implementation(project.dependencies.platform("org.kotlincrypto.macs:bom:0.7.0"))
            implementation("org.kotlincrypto.macs:hmac-sha2")

            // kotlinx.datetime
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.7.1")

            // Coil
            implementation("io.coil-kt.coil3:coil-compose:3.3.0")
            implementation("io.coil-kt.coil3:coil-network-ktor3:3.3.0")
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}

