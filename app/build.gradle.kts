plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    val signingProps = file("../signing.properties")
    val commitShort = providers.exec {
        workingDir = rootDir
        commandLine = "git rev-parse --short HEAD".split(" ")
    }.standardOutput.asText.get().trim()

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

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = ".$commitShort"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = if (signingProps.exists()) {
                val props = `java.util`.Properties().apply { load(signingProps.reader()) }
                signingConfigs.create("release") {
                    storeFile = file(props.getProperty("storeFile"))
                    storePassword = props.getProperty("storePassword")
                    keyAlias = props.getProperty("keyAlias")
                    keyPassword = props.getProperty("keyPassword")
                }
            } else signingConfigs.getByName("debug")
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
    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(21)
        }
    }
    kotlin {
        jvmToolchain(21)
    }
    androidResources {
        generateLocaleConfig = true
        // Do not compress the dex files, so the apk can be imported as a privileged app
        noCompress += "dex"
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
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.biometric.ktx)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.pinyin4j)
    implementation(libs.material)
    implementation(libs.insetter)
    implementation(libs.simplemenu.preference)
    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)
    implementation(libs.dhizuku.api)
    implementation(libs.appiconloader)
    implementation(libs.commons.text)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.hiddenapibypass)
}