package tasks

import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Exec
import org.gradle.kotlin.dsl.register

object TaskNames {
    const val POCKET_GEN_JAR = "pocketGenJar"
    const val SYNC_TESTS_GEN_JAR = "syncTestsGenJar"
    const val POCKET_JAR_PUBLISH = "pocketGenJarPublish"
    const val SYNC_TESTS_JAR_PUBLISH = "syncTestsGenJarPublish"
    const val GENERATE_EXAMPLES = "generateExamples"
}

/**
 * Moves jar generated above to the correct directory
 */
fun Project.registerPocketGenJarPublish() {
    tasks.register(TaskNames.POCKET_JAR_PUBLISH, Copy::class).get().apply {
        dependsOn(tasks.getByName(TaskNames.POCKET_GEN_JAR))
        from(file("build/libs/sync-pocket-class-generator.jar"))
        into(file("../sync-pocket/"))
    }
}

/**
 * Moves jar generated above to the correct directory
 */
fun Project.registerSyncTestsGenJarPublish() {
    tasks.register(TaskNames.SYNC_TESTS_JAR_PUBLISH, Copy::class).get().apply {
        dependsOn(tasks.getByName(TaskNames.SYNC_TESTS_GEN_JAR))
        from(file("build/libs/sync-tests-class-generator.jar"))
        into(file("../sync-pocket-android/"))
    }
}

/**
 * Creates example generated classes and places them in /sync-gen/examples/
 */
fun Project.registerGenerateExamples(classpath: String) {
    tasks.register(TaskNames.GENERATE_EXAMPLES, Exec::class).get().apply {
        val examplesDir = "$projectDir/examples"
        val inDir = "${examplesDir}/graphql/"
        val outDir = "${examplesDir}/output/"
        val usageFile = "${examplesDir}/examples-usage.txt"
        commandLine(
            "java",
            "-classpath",
            classpath + ":${buildDir.path}/libs/${project.name}.jar",
            "com.pocket.sync.print.java.examples.ExamplesGenerator",
            inDir,
            outDir,
            usageFile,
        )
        doFirst {
            delete(file(outDir).listFiles())
        }
        dependsOn(":${project.name}:jar")
    }
}