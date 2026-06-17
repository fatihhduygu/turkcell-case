plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.turkcell.bip.feature.splash"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}