plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.pedroid.android17locationbutton.compose"
    compileSdk = 37

    defaultConfig {
        minSdk = 24
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    // Core types (LocationButtonState, LocationStrategy, LocationFetcher, etc.)
    // are exposed as api so consumers don't need to depend on controller-core directly.
    api(project(":controller-core"))

    // LocationButtonTextType appears in LocationButtonView's public API.
    api(libs.androidx.core.locationbutton.compose)

    // Compose types (Modifier, Color, Dp, PaddingValues) appear in public API.
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.foundation)

    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
}
