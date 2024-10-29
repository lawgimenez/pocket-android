plugins {
    kotlinJvm()
}

dependencies {
    api(project(Deps.Pocket.utils))

    api(Deps.Google.Guava.guava)
    api(libs.okio)

    api(platform(libs.kotlinx.coroutines.bom))
    api(libs.kotlinx.coroutines.core)
}
