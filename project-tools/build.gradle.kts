import tasks.registerJarTask

plugins {
    kotlinJvm()
    kotlinKapt()
}

dependencies {
    implementation(Deps.Picocli.picocli)
    kapt(Deps.Picocli.codeGen)

    implementation(Deps.Commons.IO.commonsIo)
}

registerJarTask(
    taskName = "legacyToolJar",
    mainClass = "com.pocket.legacy.ProjectTools",
    jarName = "legacyTools",
    configuration = configurations
)
registerJarTask(
    taskName = "toolJar",
    mainClass = "com.pocket.tools.ToolsKt",
    jarName = "tools",
    configuration = configurations
)
