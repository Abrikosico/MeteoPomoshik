plugins {
    alias(libs.plugins.android.application)
    id ("com.google.gms.google-services") // Плагин для Firebase
}

android {
    namespace = "com.example.meteopomoshik"
    compileSdk = 36 // Используем стабильную версию

    defaultConfig {
        applicationId = "com.example.meteopomoshik"
        minSdk = 24
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
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // Room (База данных)
    implementation(libs.room.common.jvm)
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)

    // Сеть
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.google.code.gson:gson:2.10.1")

    // Геолокация
    implementation("com.google.android.gms:play-services-location:21.0.1")

    // !! FIREBASE (АККАУНТЫ) !!
    // BoM управляет версиями, чтобы они не конфликтовали
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-auth")

    // Тесты
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // WorkManager (Фоновые задачи)
    implementation("androidx.work:work-runtime:2.9.0")

    // БЕСПЛАТНЫЕ КАРТЫ (OpenStreetMap)
    implementation("org.osmdroid:osmdroid-android:6.1.18")

    // Анимации Lottie
    implementation("com.airbnb.android:lottie:6.4.0")
}