package tasks

import org.gradle.api.Project
import org.gradle.api.tasks.JavaExec
import org.gradle.kotlin.dsl.register

const val SYNC_GEN_TASK_NAME = "syncGen"

/**
 * Generate Pocket's sync classes from schema.
 * The generator jar ends up in this module as a result of the sync-gen:pocketGenJarPublish gradle task
 */
fun Project.registerSyncPocketGenTask() {
    val schemaDir = "src/main/graphql"
    val outputDir = getSyncPocketGenTaskOutputDir()
    val jarFile = "sync-pocket-class-generator.jar"
    val usageFile = "sync-pocket-usage.txt"

    tasks.register(SYNC_GEN_TASK_NAME, JavaExec::class).get().apply {
        classpath = files(jarFile)
        args = listOf(schemaDir, outputDir, usageFile, "CLIENT_API")
        doFirst {
            delete(file(outputDir))
        }
        inputs.file(jarFile)
        inputs.dir(schemaDir)
        inputs.file(usageFile)
        outputs.dir(outputDir)
        outputs.file(usageFile)
    }
}

fun Project.getSyncPocketGenTaskOutputDir(): String = "$buildDir/generated/sources/sync/"