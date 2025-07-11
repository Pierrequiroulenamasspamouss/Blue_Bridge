plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.bluebridgeapp.bluebridge"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.bluebridgeapp.bluebridge"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.4.9"

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
    }
    dependenciesInfo {
        includeInApk = true
        includeInBundle = true
    }
    //buildToolsVersion = "33.0.1"
    ndkVersion = "27.0.12077973"

    lint {
        baseline = file("lint-baseline.xml")
        abortOnError = false
    }

    // Add SMS functionality configuration
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

    // Kotlin serialization
    implementation(libs.hilt.android)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.retrofit)
    implementation(libs.converter.scalars)
    implementation(libs.jakewharton.retrofit2.kotlinx.serialization.converter)
    implementation(libs.kotlinx.serialization.json.v163)
    implementation(libs.androidx.datastore.preferences.v114)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.datastore.core)
    implementation(libs.androidx.navigation.compose.v277)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.benchmark.common)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.google.play.services.location)
    // Google AdMob
    //noinspection UseTomlInstead,GradleDependency
    implementation("com.google.android.gms:play-services-ads:24.2.0")
    implementation(libs.accompanist.permissions)

    // OSMDroid dependencies
    implementation(libs.osmdroid.android)
    implementation(libs.osmdroid.mapsforge)
    implementation(libs.coil.compose)

    // Firebase dependencies
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging.ktx)
    implementation(libs.firebase.analytics.ktx)
    implementation(libs.firebase.inappmessaging.display)

    // Add SMS functionality dependencies
    implementation(libs.play.services.auth)
    implementation(libs.play.services.auth.api.phone)
    testImplementation(libs.junit)
    testImplementation(libs.junit.jupiter)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)


}
