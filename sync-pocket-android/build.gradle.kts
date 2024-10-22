import tasks.registerSyncTestGenTask

plugins {
    pocketAndroidLib()
}
android {
    namespace = "com.pocket.sdk"
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

android.unitTestVariants.all {
    project.registerSyncTestGenTask(this)
}

dependencies {
    api(project(Deps.Pocket.syncPocket))
    api(project(Deps.Pocket.syncAndroid))

    testImplementation(Deps.JUnit.junit)
    testImplementation(Deps.Mockito.core)
    testImplementation(libs.robolectric)
    testImplementation(Deps.Commons.IO.commonsIo)
    testImplementation(Deps.Jetbrains.Kotlin.jUnit)
}
