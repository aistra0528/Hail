plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    namespace = "com.aistra.hail"
    buildToolsVersion = "35.0.0"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.aistra.hail"
        minSdk = 23
        targetSdk = 35
        versionCode = 33
        versionName = "1.9.0"
    }

    // Do not compress the dex files, so the apk can be imported as a privileged app
    androidResources {
        noCompress += "dex"
    }

    val signing = if (file("../signing.properties").exists()) {
        signingConfigs.create("release") {
            val props = `java.util`.Properties().apply { load(file("../signing.properties").reader()) }
            storeFile = file(props.getProperty("storeFile"))
            storePassword = props.getProperty("storePassword")
            keyAlias = props.getProperty("keyAlias")
            keyPassword = props.getProperty("keyPassword")
        }
    } else signingConfigs.getByName("debug")
    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signing
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
        }
    }
    applicationVariants.configureEach {
        outputs.configureEach {
            (this as? com.android.build.gradle.internal.api.ApkVariantOutputImpl)?.outputFileName =
                "Hail-v$versionName.apk"
        }
    }
    compileOptions {
        sourceCompatibility(JavaVersion.VERSION_17)
        targetCompatibility(JavaVersion.VERSION_17)
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    androidResources {
        generateLocaleConfig = true
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.biometric:biometric-ktx:1.2.0-alpha05")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.5")
    implementation("androidx.navigation:navigation-fragment-ktx:2.8.0")
    implementation("androidx.navigation:navigation-ui-ktx:2.8.0")
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    implementation("com.belerweb:pinyin4j:2.5.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("dev.chrisbanes.insetter:insetter:0.6.1")
    implementation("dev.rikka.rikkax.preference:simplemenu-preference:1.0.3")
    implementation("dev.rikka.shizuku:api:13.1.5")
    implementation("dev.rikka.shizuku:provider:13.1.5")
    implementation("io.github.iamr0s:Dhizuku-API:2.5.3")
    implementation("me.zhanghai.android.appiconloader:appiconloader:1.5.0")
    implementation("org.apache.commons:commons-text:1.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.lsposed.hiddenapibypass:hiddenapibypass:4.3")
}