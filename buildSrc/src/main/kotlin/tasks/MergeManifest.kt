package tasks

import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.kotlin.dsl.register

fun Project.registerCopyMergedManifestTask() {
    tasks.register("copyMergedManifest", Copy::class).get().apply {
        dependsOn("processPlayUnsignedReleaseManifest")
        from("$buildDir/intermediates/merged_manifests/playUnsignedRelease/processPlayUnsignedReleaseManifest/AndroidManifest.xml")
        into("./merged_manifests/playUnsignedRelease")
    }
}