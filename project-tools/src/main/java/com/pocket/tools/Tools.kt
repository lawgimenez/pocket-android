package com.pocket.tools

import picocli.CommandLine
import picocli.CommandLine.Command
import kotlin.system.exitProcess

/** Entry point for the new tools jar based on picocli. */
fun main(args: Array<String>) : Unit = exitProcess(CommandLine(Tools()).execute(*args))

/**
 * A top level "command" that acts just as a list of actual commands you can call. It is
 * intentionally empty and acts just as a holder for all the [Command.subcommands].
 * 
 * If you want to add a command, create one wherever you please (might be in a new file,
 * might be in an existing file if it makes sense to group a few together, or heck might be
 * even here, technically nothing is stopping us) and add it to the [Command.subcommands] array.
 * 
 * Useful resources for writing a picocli command:
 * * [picocli docs](https://picocli.info/),
 */
@Command(
    name = "project-tools",
    mixinStandardHelpOptions = true,
    subcommands = [
        UpdateBuildVersionForRelease::class,
    ]
)
class Tools
