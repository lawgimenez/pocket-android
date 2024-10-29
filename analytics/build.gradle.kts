plugins {
    pocketAndroidLib()
}
android {
    namespace = "com.pocket.analytics"
}
dependencies {
    implementation(project(Deps.Pocket.utilsAndroid))
    implementation(libs.snowplow.android.tracker)
    implementation(libs.androidx.core)
    implementation(libs.okhttp)
}
