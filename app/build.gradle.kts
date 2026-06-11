plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

android {
    namespace = "com.carfry369.teleport"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.carfry369.teleport"
        minSdk = 26 // Required for Ktor Netty and Media3
        targetSdk = 35
        versionCode = (project.findProperty("versionCode") as? String)?.toIntOrNull() ?: 6
        versionName = (project.findProperty("versionName") as? String) ?: "1.0.0"
    }

    lint {
        lintConfig = file("lint.xml")
        abortOnError = false
        checkReleaseBuilds = true
    }

    signingConfigs {
        create("release") {
            val keystoreFile = if (project.hasProperty("keystoreFile")) file(project.property("keystoreFile") as String) else System.getenv("KEYSTORE_FILE")?.let { file(it) }
            val keystorePassword = if (project.hasProperty("keystorePassword")) project.property("keystorePassword") as String else System.getenv("KEYSTORE_PASSWORD")
            val keyAlias = if (project.hasProperty("keyAlias")) project.property("keyAlias") as String else System.getenv("KEY_ALIAS")
            val keyPassword = if (project.hasProperty("keyPassword")) project.property("keyPassword") as String else System.getenv("KEY_PASSWORD")

            if (keystoreFile != null && keystoreFile.exists() && keystorePassword != null && keyAlias != null && keyPassword != null) {
                storeFile = keystoreFile
                storePassword = keystorePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            } else {
                val debugConfig = signingConfigs.getByName("debug")
                storeFile = debugConfig.storeFile
                storePassword = debugConfig.storePassword
                this.keyAlias = debugConfig.keyAlias
                this.keyPassword = debugConfig.keyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
    packaging {
        resources {
            excludes += "META-INF/INDEX.LIST"
            excludes += "META-INF/io.netty.versions.properties"
        }
    }
}

dependencies {
    // Android Core - Updated to compatible versions for SDK 35 / AGP 8.9.0
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.12.0")

    // Jetpack Compose - Updated to latest compatible BOM
    val composeBom = platform("androidx.compose:compose-bom:2024.11.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    // Google Play In-App Updates SDK
    implementation("com.google.android.play:app-update:2.1.0")
    implementation("com.google.android.play:app-update-ktx:2.1.0")

    // Ktor Server (for TV server) - Updated to latest stable version
    val ktorVersion = "3.0.3"
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-websockets:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")

    // Ktor Client (for Mobile client)
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-okhttp:$ktorVersion")
    implementation("io.ktor:ktor-client-websockets:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")

    // Kotlinx Serialization JSON - Updated to latest version
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // Google Media3 (ExoPlayer for TV native streaming player) - Updated to compatible version for SDK 35
    val media3Version = "1.5.0"
    implementation("androidx.media3:media3-exoplayer:$media3Version")
    implementation("androidx.media3:media3-exoplayer-hls:$media3Version")
    implementation("androidx.media3:media3-exoplayer-dash:$media3Version")
    implementation("androidx.media3:media3-ui:$media3Version")
    implementation("androidx.media3:media3-session:$media3Version")

    // QR Code Scanning (ZXing)
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation("com.google.zxing:core:3.5.3")

    // Unit Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.11.1")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.1.0"))
    implementation("com.google.firebase:firebase-crashlytics-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")

    // Google Mobile Ads
    implementation("com.google.android.gms:play-services-ads:23.1.0")

    // Google User Messaging Platform (UMP) for Ad Consent
    implementation("com.google.android.ump:user-messaging-platform:2.2.0")

    // Google Play Billing Library
    implementation("com.android.billingclient:billing-ktx:6.2.1")
}
