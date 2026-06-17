plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.turkcell.bip.feature.home"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}