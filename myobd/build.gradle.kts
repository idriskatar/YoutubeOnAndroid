plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.youtubeonauto.myobd"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.youtubeonauto.myobd"
        minSdk = 35
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
}
