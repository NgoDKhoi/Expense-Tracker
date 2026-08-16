plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.example.expensetracker.core.domain"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
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
    // LiveData for reactive patterns - needed for repository interface
    compileOnly(libs.lifecycle.livedata)

    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.core.testing)
    testImplementation(libs.lifecycle.livedata)
    
    // JSR-330 Inject for Domain
    implementation(libs.javax.inject)
}
