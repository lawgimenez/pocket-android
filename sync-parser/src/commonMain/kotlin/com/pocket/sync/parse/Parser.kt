package com.pocket.sync.parse

import okio.FileSystem
import okio.Path

/**
 * Parsing has two major steps to it:
 * 1. Extracting the raw data as it is described in the schema file, as [PropertyData] and [FigmentsData]. This step only validates the syntax rules.
 * 2. Taking that data and resolving references between definitions and fully validating it. This produces instances that are strongly typed, as [com.pocket.sync.type.Property] and [com.pocket.sync.Figments].
 *
 * [Parser] instances are only responsible for this first part.
 * Once you have the data, use [FigmentsData.resolve] to validate and resolve it.
 */
interface Parser {
    /** Parse raw [text] into valid in-memory "figments" raw data. */
    fun parse(text: String) : FigmentsData

    /**
     * Parse a file or files into valid in-memory "figments" raw data.
     * 
     * Implementations should consider using [allWithExtension] helper.
     * 
     * @param path can be a single file, or if a directory will look into it for all files with
     *      an extension specific to this parser
     */
    fun parse(fs: FileSystem, path: Path) : FigmentsData
}

/**
 * @return if [path] is a directory, returns all files within that match the [extension],
 *      otherwise just returns [path]
 */
fun FileSystem.allWithExtension(path: Path, extension: String): List<Path> {
    return if (metadata(path).isDirectory) {
        list(path).filter { it.name.endsWith(".$extension") }
    } else {
        listOf(path)
    }
}
