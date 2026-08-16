plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.example.expensetracker.core.network"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    
    testImplementation(libs.junit)
}
