plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    setDefaultConfigs()
}
androidComponents {
    beforeVariants(selector().withBuildType("debug")) {
        it.enable = false
    }
}
kotlin {
    setDefaultConfigs()
}
