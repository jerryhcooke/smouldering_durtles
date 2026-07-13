plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.smouldering_durtles.wk"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.smouldering_durtles.wk"
        minSdk = 21
        targetSdk = 34
        versionCode = 84
        versionName = "1.2.3"
        multiDexEnabled = true

        vectorDrawables {
            useSupportLibrary = true
        }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testApplicationId = "com.smouldering_durtles.wk.test"
        testHandleProfiling = true
        testFunctionalTest = true

        javaCompileOptions {
            annotationProcessorOptions {
                arguments["room.schemaLocation"] = "$projectDir/schemas"
            }
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            resValue("string", "fileprovider_authority", "com.smouldering_durtles.wk.fileprovider")
            resValue("string", "searchprovider_authority", "com.smouldering_durtles.wk.db.SubjectContentProvider")
            resValue("string", "applabel", "@string/label")
            buildConfigField("String", "FILEPROVIDER_AUTHORITY", "\"com.smouldering_durtles.wk.fileprovider\"")
            signingConfig = signingConfigs.getByName("debug")
        }
        debug {
            applicationIdSuffix = ".debug"
            resValue("string", "fileprovider_authority", "com.smouldering_durtles.wk.debug.fileprovider")
            resValue("string", "searchprovider_authority", "com.smouldering_durtles.wk.debug.db.SubjectContentProvider")
            resValue("string", "applabel", "WK (Debug)")
            buildConfigField("String", "FILEPROVIDER_AUTHORITY", "\"com.smouldering_durtles.wk.debug.fileprovider\"")
        }
    }
    sourceSets {
        getByName("androidTest").assets.srcDirs(files("$projectDir/schemas"))
    }
    testOptions {
        reportDir = "$rootDir/test-reports"
        resultsDir = "$rootDir/test-results"
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.02.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)

    implementation(libs.material)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.junit)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.fragment.ktx)
    coreLibraryDesugaring(libs.desugar)

    annotationProcessor(libs.androidx.room.compiler)

    debugImplementation(libs.androidx.annotation)

    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation(libs.androidx.multidex)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.legacy.v4)
    implementation(libs.androidx.vectordrawable)
    implementation(libs.androidx.room.runtime)
    implementation(libs.jackson.databind)
    implementation(libs.androidx.legacy.core.utils)
    implementation(libs.androidx.preference)
    implementation(libs.androidx.security)
    implementation(libs.lottie)
    implementation(libs.androidx.work)
    implementation(libs.jsoup)
    implementation(libs.okhttp)
    implementation(libs.okhttp.urlconnection)
    implementation(libs.pikolo)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.gridlayout)
    implementation(libs.glide)
    annotationProcessor(libs.glide.compiler)
    implementation(libs.glide.transformations)
    implementation(libs.gson)

    testImplementation(libs.junit)

    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.annotation)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.room.testing)
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-Xlint:unchecked")
    options.isDeprecation = true
}