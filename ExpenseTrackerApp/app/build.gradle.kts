plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.expensetracker"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.expensetracker"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    // Project modules (Clean Architecture)
    implementation(project(":core:domain"))
    implementation(project(":core:database"))
    implementation(project(":core:network"))

    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.core.testing)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
    
    // CameraX
    implementation(libs.camera.core)
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)
    
    // Glide
    implementation(libs.glide)
    annotationProcessor(libs.glide.compiler)
    
    // Lifecycle components
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)
    
    // MPAndroidChart
    implementation(libs.mpandroidchart)
    
    // Dagger 2 DI
    implementation(libs.dagger)
    annotationProcessor(libs.dagger.compiler)
}