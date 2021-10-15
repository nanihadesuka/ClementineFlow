import java.util.Properties

plugins {
    id("com.android.application")
    id("kotlin-android")
    id("kotlin-kapt")
}

android {
    compileSdk = 31

    val localPropertiesFile = file("../local.properties")
    val isSignBuild = localPropertiesFile.exists()

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.0.1"
    }

    defaultConfig {
        applicationId = "my.nanihadesuka.clementineflow"
        minSdk = 26
        targetSdk = 31
        versionCode = 1
        versionName = "1.0"
        setProperty("archivesBaseName", "ClementineFlow_v$versionName")
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    if (isSignBuild) signingConfigs {
        create("release") {
            val properties = Properties().apply {
                load(localPropertiesFile.inputStream())
            }
            storeFile = file(properties.getProperty("storeFile"))
            storePassword = properties.getProperty("storePassword")
            keyAlias = properties.getProperty("keyAlias")
            keyPassword = properties.getProperty("keyPassword")
        }
    }

    buildTypes {

        if (isSignBuild) all {
            signingConfig = signingConfigs["release"]
        }

        named("debug") {
            postprocessing {
                isRemoveUnusedCode = false
                isObfuscate = false
                isOptimizeCode = false
                isRemoveUnusedResources = false
            }
        }

        named("release") {
            postprocessing {
                proguardFile("proguard-rules.pro")
                isRemoveUnusedCode = true
                isObfuscate = false
                isOptimizeCode = true
                isRemoveUnusedResources = true
            }
        }
    }

    buildFeatures {
        compose = true
    }
}

dependencies {

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2")

    implementation("androidx.appcompat:appcompat:1.3.1")

    // Lifecycle components
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.3.1")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.3.1")
    implementation("androidx.lifecycle:lifecycle-common-java8:2.3.1")

    // UI
    implementation("androidx.constraintlayout:constraintlayout:2.1.1")
    implementation("com.google.android.material:material:1.4.0")

    implementation("com.google.code.gson:gson:2.8.7")

    implementation(fileTree("libs") { include("*.jar") })
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.5.30")
    implementation("androidx.core:core-ktx:1.6.0")
    implementation("org.jsoup:jsoup:1.14.1")

    implementation("org.jetbrains.kotlin:kotlin-script-runtime:1.5.30-RC")

    implementation("com.afollestad.material-dialogs:core:3.2.1")

    // Glide
    implementation("com.github.bumptech.glide:glide:4.12.0")
    kapt("com.github.bumptech.glide:compiler:4.12.0")

    ///// Jetpack compose /////

    // Tooling support (Previews, etc.)
    implementation("androidx.compose.ui:ui:1.0.3")
    // Foundation (Border, Background, Box, Image, Scroll, shapes, animations, etc.)
    implementation("androidx.compose.ui:ui-tooling:1.0.3")
    // Material Design
    implementation("androidx.compose.foundation:foundation:1.0.3")

    // Material design icons
    implementation("androidx.compose.material:material:1.0.3")
    implementation("androidx.compose.material:material-icons-core:1.0.3")
    implementation("androidx.compose.material:material-icons-extended:1.0.3")

    // Integration with activities
    implementation("androidx.activity:activity-compose:1.3.1")

    // Integration with ViewModels
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.4.0-rc01")

    // Integration with observables
    implementation("androidx.compose.runtime:runtime:1.0.3")
    implementation("androidx.compose.runtime:runtime-livedata:1.0.3")

    // Toolbar collapsible
    implementation("me.onebone:toolbar-compose:2.1.2")
    implementation("com.github.nanihadesuka:LazyColumnScrollbar:1.0.3")

    implementation("com.google.protobuf:protobuf-java:3.18.0")
}