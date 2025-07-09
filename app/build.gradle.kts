import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.example.shutterup"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.shutterup"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // local.properties에서 Mapbox 토큰 읽기
        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localProperties.load(localPropertiesFile.inputStream())
        }
        
        val mapboxToken = localProperties.getProperty("MAPBOX_ACCESS_TOKEN", "")
        println("Mapbox Token length: ${mapboxToken.length}")
        
        buildConfigField("String", "MAPBOX_ACCESS_TOKEN", "\"$mapboxToken\"")
        manifestPlaceholders["MAPBOX_ACCESS_TOKEN"] = mapboxToken
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
        buildConfig = true  // BuildConfig 활성화 (중요!)
    }
    
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.10"
    }
    
    hilt {
        enableAggregatingTask = false
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.appcompat:appcompat:1.6.1")

    implementation("androidx.compose.material:material-icons-extended") //이거 navigation bar의 icon때문에 넣었음
    implementation("androidx.compose.material3:material3:1.2.1") // 버전은 최신으로


    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.android.compiler)
    // Hilt Compose Integration (선택 사항, 필요시)
    // implementation(libs.hilt.navigation.compose)

    // Compose LiveData
    implementation(libs.androidx.lifecycle.livedata.runtime.ktx)

    // ViewModel Compose
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Kotlinx Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.compose.runtime.livedata)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.coil.compose)
    implementation(libs.androidx.hilt.navigation.compose)

    // Mapbox
    implementation(libs.mapbox.android)
    implementation(libs.mapbox.compose)

    // Google Play Services Location
    implementation("com.google.android.gms:play-services-location:21.0.1")

    // Core Splash Screen (시스템 스플래시 제어용)
    implementation("androidx.core:core-splashscreen:1.0.1")

}