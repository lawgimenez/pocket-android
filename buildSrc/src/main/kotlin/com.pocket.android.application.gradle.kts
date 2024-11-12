plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    setDefaultConfigs()
}
androidComponents {
    beforeVariants {
        it.enableUnitTest = it.name == (System.getenv("PR_BUILD_VARIANT") ?: "developDebug")
    }
}
kotlin {
    setDefaultConfigs()
}
