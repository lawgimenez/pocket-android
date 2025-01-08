// From Gradle docs:
// > We recommend that you also create a buildSrc/settings.gradle.kts file, which you may leave empty.
// Source: https://docs.gradle.org/current/userguide/custom_plugins.html#sec:precompiled_plugins

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "buildSrc"
