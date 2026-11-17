plugins {
 id("com.android.application")
 id("org.jetbrains.kotlin.android")
}
android {
 namespace = "com.buylora.studio"
 compileSdk = 35
 defaultConfig { applicationId = "com.buylora.studio"; minSdk = 26; targetSdk = 35; versionCode = 1; versionName = "2.0.0" }
 buildTypes { release { isMinifyEnabled = false } }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
 kotlinOptions { jvmTarget = "17" }
}
