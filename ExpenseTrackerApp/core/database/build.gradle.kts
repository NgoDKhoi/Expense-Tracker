plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.example.expensetracker.core.database"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        javaCompileOptions {
            annotationProcessorOptions {
                arguments["room.schemaLocation"] = "$projectDir/schemas"
            }
        }
    }

    // Migration tests read the exported schema JSON files. AGP 9 needs the new-DSL type for sourceSets here.
    (this as com.android.build.api.dsl.LibraryExtension).sourceSets.getByName("androidTest").assets.directories.add("$projectDir/schemas")

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
    androidTestImplementation(libs.room.testing)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.test.runner)
    
    // Dagger 2 DI
    implementation(libs.dagger)
    annotationProcessor(libs.dagger.compiler)
}
