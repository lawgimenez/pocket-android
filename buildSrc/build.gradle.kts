/**
 * The buildSrc build.gradle.kts file is the top most level gradle file.
 * The dependencies defined here are used in all other gradle scripts.  They are NOT used in the
 * compiled app.  They essentially replace the root directories' gradle script's buildScript block.
 * It uses `implementation` instead of `classpath`
 */

plugins {
    `kotlin-dsl`
}

repositories {
    exclusiveContent {
        forRepository { google() }
        filter {
            includeGroupByRegex("^com\\.android.*")
            includeGroupByRegex("^androidx.*")
            includeGroupByRegex("com\\.google\\.testing.*")
        }
    }
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(libs.plugin.android)
    implementation(libs.plugin.kotlin)
    implementation(libs.plugin.kotlin.compose)
    implementation(libs.plugin.kotlin.serialization)
    implementation(libs.plugin.licensee)
    implementation(libs.plugin.aboutlibraries)
    implementation(libs.plugin.versions)
    implementation(libs.plugin.dagger)
    implementation(libs.plugin.androidx.navigation.safeargs)
    implementation(libs.plugin.sentry)
}
