import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val localProps = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}

android {
    namespace = "com.github.bssthu.filesaver"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.github.bssthu.filesaver"
        minSdk = 24
        targetSdk = 35
        versionCode = 2
        versionName = "2.0"
        setProperty("archivesBaseName", "filesaver-v$versionName")
    }

    signingConfigs {
        create("release") {
            storeFile = localProps.getProperty("keystore.path")?.let { file(it) }
            keyAlias = localProps.getProperty("keystore.alias")
            storePassword = findProperty("storePassword") as String? ?: ""
            keyPassword = findProperty("keyPassword") as String? ?: ""
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

// Interactive password prompt — only triggered when building Release variants
val releaseSigningConfig = android.signingConfigs.getByName("release")
gradle.taskGraph.whenReady {
    if (allTasks.any { it.name.contains("Release") }) {
        if (releaseSigningConfig.storePassword.isNullOrEmpty()) {
            val console = System.console()
                ?: throw GradleException(
                    "Keystore password not set. Use: ./gradlew assembleRelease -PstorePassword=xxx -PkeyPassword=xxx"
                )
            releaseSigningConfig.storePassword =
                String(console.readPassword("\nKeystore password: ") ?: charArrayOf())
            releaseSigningConfig.keyPassword =
                String(console.readPassword("Key password: ") ?: charArrayOf())
        }
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.documentfile:documentfile:1.0.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
}
