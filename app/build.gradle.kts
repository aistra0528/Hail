import org.jetbrains.kotlin.konan.properties.Properties

plugins {
    id("com.android.application")
    kotlin("android")
}
val version = Properties().apply { load(file("../version.properties").reader()) }
val props = Properties().apply { load(file("androidSign/androidSign/signing.properties").reader()) }
val keyStoreFile = file("androidSign/androidSign/" + props.getProperty("storeFile"))
android {
    compileSdk = 33
    buildToolsVersion = "33.0.0"

    defaultConfig {
        applicationId = "com.aistra.hail"
        minSdk = 23
        targetSdk = 33
        versionCode = version.getProperty("versionCode").toInt()
        versionName = version.getProperty("versionName")
        resourceConfigurations += arrayOf("en", "es", "it", "ja-rJP", "ru", "zh-rCN", "zh-rTW")
    }
    signingConfigs {
        create("release") {
            storeFile = keyStoreFile
            storePassword = props.getProperty("storePassword")
            keyAlias = props.getProperty("keyAlias")
            keyPassword = props.getProperty("keyPassword")
        }
    }
    buildTypes {
        debug {
            versionNameSuffix = "-alpha"
            signingConfig = signingConfigs.getByName("release")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    applicationVariants.all {
        outputs.all {
            (this as? com.android.build.gradle.internal.api.ApkVariantOutputImpl)
                ?.outputFileName = "Hail.apk"
        }
    }
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildFeatures {
        viewBinding = true
    }
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")
    implementation("androidx.core:core-ktx:1.8.0")
    implementation("androidx.appcompat:appcompat:1.4.2")
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.navigation:navigation-fragment-ktx:2.5.0")
    implementation("androidx.navigation:navigation-ui-ktx:2.5.0")
    implementation("androidx.preference:preference-ktx:1.2.0")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("androidx.work:work-runtime-ktx:2.7.1")
    implementation("com.google.android.material:material:1.6.1")
    implementation("dev.rikka.shizuku:api:12.1.0")
    implementation("dev.rikka.shizuku:provider:12.1.0")
    implementation("me.zhanghai.android.appiconloader:appiconloader:1.4.0")
    implementation("com.belerweb:pinyin4j:2.5.1")
}