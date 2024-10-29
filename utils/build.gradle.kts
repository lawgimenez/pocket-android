plugins {
    kotlinJvm()
}

dependencies {
    api(platform(libs.kotlinx.coroutines.bom))
    api(libs.kotlinx.coroutines.core)
    api(Deps.RxJava.rxJava)
    api(Deps.Jackson.core)
    api(Deps.Jackson.databind)
    api(Deps.Apache.Commons.commonsLang)
    api(Deps.ThreeTen.threeTenBp)
}
