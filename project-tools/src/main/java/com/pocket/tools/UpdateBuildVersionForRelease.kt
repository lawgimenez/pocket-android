package com.pocket.tools

import picocli.CommandLine.*
import java.io.File
import java.util.concurrent.Callable

@Command(
    name = "updateBuildVersionForRelease",
    description = ["Sets the build version"],
    mixinStandardHelpOptions = true,
)
class UpdateBuildVersionForRelease : Callable<Unit> {

    @Parameters(index = "0", paramLabel = "BRANCH_NAME")
    lateinit var branchName: String

    override fun call() {
        val versionNumbers = branchName.substringAfter("release-").split(".")
        // version numbers based on branch name
        val majorVersion = versionNumbers[0]
        val minorVersion = versionNumbers[1]
        val patchVersion = versionNumbers[2]

        val gradleFile = File("./Pocket/build.gradle.kts")
        val gradleFileText = gradleFile.readText()
        val gradleFileTextByLine = gradleFileText.split("\n")

        // version numbers based on current gradle file
        val oldMajorVersion = gradleFileTextByLine.getVersionNumber("val versionMajor =")
        val oldMinorVersion = gradleFileTextByLine.getVersionNumber("val versionMinor =")
        val oldPatchVersion = gradleFileTextByLine.getVersionNumber("val versionPatch =")
        val oldBuildVersion = gradleFileTextByLine.getVersionNumber("val versionBuild =")

        // line indexes of version numbers in the gradle file
        val majorVersionIndex = gradleFileTextByLine.indexOfFirst { it.startsWith("val versionMajor =") }
        val minorVersionIndex = gradleFileTextByLine.indexOfFirst { it.startsWith("val versionMinor =") }
        val patchVersionIndex = gradleFileTextByLine.indexOfFirst { it.startsWith("val versionPatch =") }
        val buildVersionIndex = gradleFileTextByLine.indexOfFirst { it.startsWith("val versionBuild =") }

        // the new gradle file string we will write
        val newTextByLine = gradleFileTextByLine.toMutableList()

        if (majorVersion == oldMajorVersion
            && minorVersion == oldMinorVersion
            && patchVersion == oldPatchVersion
        ) {
            // version are the same.  Update build version only
            newTextByLine.removeAt(buildVersionIndex)
            newTextByLine.add(
                buildVersionIndex,
                "val versionBuild = ${oldBuildVersion.toInt() + 1} // Max of three digits"
            )
        } else {
            // version are different. Update major, minor, patch, and build versions
            newTextByLine.removeAt(majorVersionIndex)
            newTextByLine.add(
                majorVersionIndex,
                "val versionMajor = $majorVersion // Max value of 200"
            )

            newTextByLine.removeAt(minorVersionIndex)
            newTextByLine.add(
                minorVersionIndex,
                "val versionMinor = $minorVersion // Max of two digits"
            )

            newTextByLine.removeAt(patchVersionIndex)
            newTextByLine.add(
                patchVersionIndex,
                "val versionPatch = $patchVersion // Max of two digits"
            )

            newTextByLine.removeAt(buildVersionIndex)
            newTextByLine.add(
                buildVersionIndex,
                "val versionBuild = 0 // Max of three digits"
            )
        }

        // update the gradle file
        gradleFile.writeText(newTextByLine.joinToString(separator = "\n") { it })
    }

    // used to get the version numbers from the gradle file
    private fun List<String>.getVersionNumber(startText: String): String =
        find { it.startsWith(startText) }!!
            .split("= ")
            .last()
            .split(" ")
            .first()
}