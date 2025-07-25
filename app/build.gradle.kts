plugins {
    id("com.android.application")
    alias(libs.plugins.kotlin.android)
//    id("kotlin-kapt")
//    id("com.google.dagger.hilt.android")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.kims.recipe2"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.kims.recipe2"
        minSdk = 30
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures { viewBinding = true }

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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {

//    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
//    implementation(libs.material)
//    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
//    implementation(libs.androidx.camera.view)
//    testImplementation(libs.junit)
//    androidTestImplementation(libs.androidx.junit)
//    androidTestImplementation(libs.androidx.espresso.core)

    implementation(platform("com.google.firebase:firebase-bom:33.2.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")

    implementation("com.google.firebase:firebase-messaging-ktx")

    implementation("com.google.firebase:firebase-functions-ktx")

//    implementation("androidx.camera:camera-camera2:1.3.3")
//    implementation("androidx.camera:camera-lifecycle:1.3.3")
//    implementation("androidx.camera:camera-view:1.3.3")

//    implementation("com.google.android.gms:play-services-mlkit-object-detection")
//    implementation("com.google.mlkit:modeldownloader")
//    implementation("com.google.mlkit:image-labeling:17.0.9")
    // 현재 만들어 놓은 모델이 없어서 기본 모델을 사용해 보고, 만든 다음 커스텀으로 변경하자
//    implementation("com.google.mlkit:image-labeling-custom")

//    /* UI & Utils */
//    implementation("com.google.android.material:material:1.11.0")
//    implementation("io.coil-kt:coil:2.5.0")        // 이미지 로드
//    implementation("com.github.bumptech.glide:glide:4.16.0")       // (Coil가 안 맞을 때 선택)
//    implementation("com.google.code.gson:gson:2.10.1")

//    /* KotlinX */
//    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
//
//    implementation("com.google.dagger:hilt-android:2.51")
//    kapt("com.google.dagger:hilt-android-compiler:2.51")
//
//    implementation("com.google.firebase:firebase-analytics")
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.7")
    implementation("com.google.android.material:material:1.12.0")

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.activity:activity-ktx:1.8.2")
    implementation("com.kizitonwose.calendar:view:2.0.3")

    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")



}