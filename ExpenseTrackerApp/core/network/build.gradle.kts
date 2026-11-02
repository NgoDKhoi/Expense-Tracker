import java.util.Properties

plugins {
    alias(libs.plugins.android.library)
}

// Supabase keys live in local.properties (gitignored), never in source control.
val localProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

android {
    namespace = "com.example.expensetracker.core.network"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "SUPABASE_URL", "\"${localProps.getProperty("SUPABASE_URL", "")}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${localProps.getProperty("SUPABASE_ANON_KEY", "")}\"")
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":core:domain"))
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)

    // Dagger 2 DI
    implementation(libs.dagger)
    annotationProcessor(libs.dagger.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
}
