plugins {
    pocketAndroidLib()
}
android {
    namespace = "com.pocket.sync.android"
}
dependencies {
    api(project(Deps.Pocket.sync))
    api(project(Deps.Pocket.utilsAndroid))

    api(Deps.Google.Tink.tink)
    
    implementation(Deps.Commons.IO.commonsIo)
}
