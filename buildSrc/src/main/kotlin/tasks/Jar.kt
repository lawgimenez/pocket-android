package tasks

import org.gradle.api.Project
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.register

fun Project.registerJarTask(
    taskName: String,
    mainClass: String,
    jarName: String? = null,
    configuration: ConfigurationContainer
) {
    tasks.register(taskName, Jar::class).get().apply {
        dependsOn(tasks.getByName("build"))
        manifest.attributes(mapOf(Pair("Main-Class", mainClass)))
        jarName?.let { archiveBaseName.set(it) }
        from(
            configuration.getByName("runtimeClasspath").map {
                if (it.isDirectory) {
                    it
                } else {
                    zipTree(it)
                }
            },
            "build/classes/java/main/",
            "build/classes/kotlin/main/"
        )
        exclude("META-INF/*.RSA", "META-INF/*.SF", "META-INF/*.DSA")
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }
}