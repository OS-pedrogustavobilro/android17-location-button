plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.pedroid.android17locationbutton.core"
    compileSdk = 37

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    // SharedFlow appears in LocationFetcher's public API, so coroutines must be api.
    api(libs.kotlinx.coroutines.android)
}
