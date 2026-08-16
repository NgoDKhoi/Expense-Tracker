plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.example.expensetracker.core.database"
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
    implementation(project(":core:domain"))
    
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)
    
    implementation(libs.lifecycle.livedata)
    
    testImplementation(libs.junit)
    
    // Dagger 2 DI
    implementation(libs.dagger)
    annotationProcessor(libs.dagger.compiler)
}
