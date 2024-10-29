package com.pocket.sync.print.java

import com.pocket.sync.Figments
import com.pocket.sync.parse.FigmentsData
import com.pocket.sync.parse.graphql.QueryParser
import com.pocket.sync.parse.graphql.SpecParser
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import java.io.File
import java.io.IOException

/**
 * A standard implementation for a jar that generates files. Have your main() method call [.main].
 */
object CommandLineGeneration {
    /**
     * @param config How to create your configuration
     * @param arg The command line args.
     *              0 should be the directory or file containing your GraphQL schema.
     *              1 should be the output directory.
     *              2 is optional, the location of the usage/compat file.
     *              3 is option, the name of the remote for GraphQL queries
     * @throws IOException
     */
    @JvmStatic
    @Suppress("MagicNumber")
    fun main(config: ConfigCreator, arg: Array<String>) {
        val specIn = File(arg[0])
        val sourcesOut = File(arg[1])
        val usageFile = arg.getOrNull(2)?.let { File(it) }
        val remote = arg.getOrNull(3)

        val fs = FileSystem.SYSTEM
        val specData = SpecParser().parse(fs, specIn.toOkioPath())
        val queryData = QueryParser(specData, remote).parse(fs, specIn.toOkioPath())
        val figments = Figments(
            FigmentsData(
                specData.definitions
                    .filter {
                        // Don't generate code for Query and Mutation objects.
                        // We generate code for query and mutation operations via [QueryParser].
                        // We also represent legacy and local actions as Mutation extensions,
                        // but this is handled inside of [SpecParser].
                        it.definition.name != "Query" && it.definition.name != "Mutation"
                    }
                    .plus(queryData.definitions)
            )
        )
        Generator(figments, config.create(sourcesOut, usageFile)).generate()
    }

    interface ConfigCreator {
        fun create(srcOut: File, usageFile: File?): Config
    }
}
