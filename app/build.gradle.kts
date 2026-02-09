import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp")
}

val localProperties = Properties().apply {
    val localFile = rootProject.file("local.properties")
    if (localFile.exists()) {
        localFile.inputStream().use { this.load(it) }
    }
}

val weatherApiKey = (
    localProperties.getProperty("WEATHER_API_KEY")
        ?: System.getenv("WEATHER_API_KEY")
        ?: ""
).trim().replace("\"", "\\\"")

android {
    namespace = "com.sloth.registerapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.sloth.registerapp"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        buildConfigField("boolean", "ALLOW_PUBLIC_FLIGHT_OPS", "true")
        buildConfigField("String", "WEATHER_API_KEY", "\"$weatherApiKey\"")

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
        buildConfig = true
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

    //Gemini Dependencies
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.datastore.preferences)
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.camera.view)
    implementation(libs.litert.gpu)

    // Room Database
    val roomVersion = "2.8.3"

    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // Gson para converter FloatArray em String
    implementation("com.google.code.gson:gson:2.10.1")

    // Compose BOM (versão compatível)
    val composeBom = platform("androidx.compose:compose-bom:2024.04.01")
    implementation(composeBom)

    // Compose libraries
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.9.0")
    debugImplementation("androidx.compose.ui:ui-tooling")


    implementation(libs.litert)

    val camerax_version = "1.5.1"
    implementation(libs.dji.sdk)
    implementation(libs.material)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.room.runtime.android)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.paging.common.android)
    implementation(libs.androidx.games.activity)
    implementation(libs.androidx.camera.lifecycle)
    implementation("androidx.camera:camera-camera2:${camerax_version}")
    implementation(libs.androidx.camera.core)
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

    // Accompanist para permissões
    implementation("com.google.accompanist:accompanist-permissions:0.32.0")

    // Paging 3 para Runtime (Core)
    implementation("androidx.paging:paging-runtime-ktx:3.3.0")
    // Paging 3 para integração com Jetpack Compose
    implementation("androidx.paging:paging-compose:3.3.0")
    // Coil para carregamento de imagens
    implementation("io.coil-kt:coil-compose:2.6.0")


    implementation("androidx.compose.material:material-icons-extended")
    implementation("com.google.mlkit:face-detection:16.1.6")

    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // Mapbox Maps SDK
    implementation("com.mapbox.maps:android-ndk27:11.17.0")

    // RTMP streaming (camera do celular)
    implementation("com.github.pedroSG94.RootEncoder:library:2.6.7")

    // Localização (GPS do operador)
    implementation("com.google.android.gms:play-services-location:21.3.0")

}
