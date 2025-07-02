plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.sloth.registerapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.sloth.registerapp"
        minSdk = 21
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        viewBinding = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/rxjava.properties"
        }
        jniLibs {
            keepDebugSymbols.add("**/libdjivideo.so")
            keepDebugSymbols.add("**/libSDKRelativeJNI.so")
            keepDebugSymbols.add("**/libFlyForbid.so")
            keepDebugSymbols.add("**/libduml_vision_bokeh.so")
            keepDebugSymbols.add("**/libyuv2.so")
            keepDebugSymbols.add("**/libGroudStation.so")
            keepDebugSymbols.add("**/libFRCorkscrew.so")
            keepDebugSymbols.add("**/libUpgradeVerify.so")
            keepDebugSymbols.add("**/libFR.so")
            keepDebugSymbols.add("**/libDJIFlySafeCore.so")
            keepDebugSymbols.add("**/libdjifs_jni.so")
            keepDebugSymbols.add("**/libsfjni.so")
        }
    }
}

dependencies {

    implementation(libs.dji.sdk)
    implementation(libs.material)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.room.runtime.android)
    compileOnly(libs.dji.sdk.provided)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)


    implementation("androidx.compose.material:material-icons-extended")
    implementation("com.google.mlkit:face-detection:16.1.6")
}