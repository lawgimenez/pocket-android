#!/usr/bin/env kotlin

val versionCatalog = java.io.File(args[0])
val sorted = versionCatalog.readText()
    // Split into sections on the empty line.
    .split("\n\n")
    .map {
        // Sort each section.
        it.split("\n")
            .sorted()
            .joinToString("\n")
    }
    // Join sections back into a single string.
    .joinToString(separator = "\n", postfix = "\n")

// Overwrite the version catalog with the sorted version.
versionCatalog.writeText(sorted)
