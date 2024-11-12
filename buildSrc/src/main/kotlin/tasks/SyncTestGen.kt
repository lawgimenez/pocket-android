package tasks

import com.android.build.gradle.api.UnitTestVariant
import org.gradle.api.Project
import org.gradle.api.tasks.JavaExec
import org.gradle.kotlin.dsl.create

/**
 * Generate the Sync unit test classes from graphql schema.
 * The generator jar ends up in this module as a result of the sync-gen:syncTestsGenJarPublish gradle task
 */
fun Project.registerSyncTestGenTask(variant: UnitTestVariant) {
    val outputDir = "${buildDir}/generated/source/sync/test/${variant.name}"
    val inputDir = "src/test/graphql"
    val usageFile = "sync-tests-usage.txt"
    val jarFile = "sync-tests-class-generator.jar"

    variant.registerJavaGeneratingTask(
        tasks.create("syncTestsGen${variant.name.capitalize()}", JavaExec::class).apply {
            classpath = files(jarFile)
            args = listOf(inputDir, outputDir, usageFile)
            doFirst {
                delete(file(outputDir).listFiles())
            }
            inputs.file(jarFile)
            inputs.dir(inputDir)
            inputs.file(usageFile)
            outputs.dir(outputDir)
            outputs.file(usageFile)
        },
        file(outputDir)
    )
}
