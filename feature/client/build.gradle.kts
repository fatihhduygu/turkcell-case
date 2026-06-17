plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.turkcell.bip.feature.client"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}