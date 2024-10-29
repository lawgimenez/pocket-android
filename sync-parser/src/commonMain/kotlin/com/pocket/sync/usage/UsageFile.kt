package com.pocket.sync.usage

import com.pocket.sync.print.figment.figmentKeyword
import com.pocket.sync.type.*
import com.pocket.sync.type.Enum
import okio.FileSystem
import okio.Path


/**
 * Maintains a file that lists all of the definitions that have previously been used in builds.
 * See Sync's Backwards Compatibility section for more details.
 * @param path The file to read and write to. If null, it will only work in memory and won't read or write any changes.
 */
class UsageFile(private val fs: FileSystem, private val path: Path?) {

    /** A set of properties that marked as excluded in the usage file.  */
    private val excluded = mutableSetOf<Usage>()

    /** All registered usages and their ids, whether they came from previously or added during this run. */
    private val included = mutableMapOf<Usage, Int?>()

    /** Captured comments to carry over to the print out. */
    private val comments = mutableMapOf<Usage, List<String>>()

    init {
        val keywords = arrayOf(
            "action",
            "thing",
            "value",
            "enum"
        ) // Only these definition types are expected in the usage file

        if (path != null) {
            fs.read(path) {
                var i = 0
                while (!exhausted()) {
                    try {
                        val lineComments = mutableListOf<String>()
                        var text = readUtf8Line() ?: continue
                        while (text.isBlank() || text.startsWith("#")) {
                            lineComments += text
                            text = readUtf8Line() ?: continue
                        }
                        var line = text.trim()

                        val exclude: Boolean
                        if (line.startsWith("-")) {
                            exclude = true
                            line = line.drop(1).trim()
                        } else {
                            exclude = false
                        }

                        // Keyword (like action, thing, etc.)
                        val keyword: String = keywords.find { line.startsWith("$it ") } ?: throw error("unexpected keyword", i, text)
                        line = line.drop(keyword.length).trim()

                        // Definition
                        val definition: String = line.substringBefore(".", line.trim())
                        if (definition.isBlank()) throw error("missing definition", i, text)
                        line = line.substringAfter(definition).trim()

                        // Aspect
                        val aspect: String? = line.substringBefore(" ", line.trim()).run { if (this.isBlank()) null else this.substringAfter(".") }

                        // Id
                        val id: Int? = line.substringAfterLast(" ", "").toIntOrNull()

                        // Index this property / id
                        val usage = Usage(keyword, definition, aspect)
                        comments[usage] = lineComments
                        if (exclude) {
                            if (!excluded.add(usage)) throw error("duplicate line found.", i, text)
                            if (included.containsKey(usage)) throw error("property is both included and excluded.", i, text)
                        } else {
                            if (included.containsKey(usage)) throw error("duplicate line found.", i, text)
                            if (excluded.contains(usage)) throw error("property is both included and excluded.", i, text)
                            included[usage] = id
                        }
                    } catch (e: Throwable) {
                        throw RuntimeException("error at line $i", e)
                    }
                    i += 1
                }
            }
        }

        // Generate missing IDs for included aspects.
        for ((usage, id) in included) {
            if (usage.aspect != null && id == null) included[usage] = generateIdFor(usage)
        }
    }

    private fun error(message: String, lineNum: Int, line: String) = RuntimeException("Usage File Error. $message at line #$lineNum : $line of file $path")

    /** @return true if this definition was previously used in the app (listed in the file) */
    fun included(definition: Definition): Boolean = included.containsKey(definition.toUsage())

    /** @return true if this definition's aspect was previously used in the app (listed in the file) */
    fun included(aspect: Aspect) = included.containsKey(aspect.toUsage())

    /** @return true if this definition is marked as excluded in the usage file. */
    fun excluded(definition: Definition) = excluded.contains(definition.toUsage())

    /** @return true if this definition's aspect was marked as excluded in the usage file */
    fun excluded(aspect: Aspect) = excluded(aspect.context.current) || excluded.contains(aspect.toUsage())

    /** @return The assigned id of this aspect. [include] it first to ensure it has an id. */
    fun id(aspect: Aspect) = included[aspect.toUsage()] ?: throw RuntimeException("No id found for $aspect, make sure it's included in the usage file.")


    /**
     * Add this definition if not already present
     */
    fun include(definition: Definition) {
        check(definition.isUsageTracked()) { "$definition is a type of definition not tracked in usage files." }
        val usage = definition.toUsage()
        if (included.containsKey(usage)) return
        included[usage] = null
    }

    /**
     * Add this aspect if not already present
     * It will be assigned the next available id within its definition.
     */
    fun include(aspect: Aspect) {
        val usage = aspect.toUsage()
        if (included.containsKey(usage)) return
        included[usage] = generateIdFor(usage)
    }

    /** Exclude this definition. */
    fun exclude(definition: Definition) {
        check(definition.isUsageTracked()) { "$definition is a type of definition not tracked in usage files." }
        val usage = definition.toUsage()
        check(!included.containsKey(usage)) { "$definition can't be excluded, because it's already included." }
        excluded.add(usage)
    }

    /** Exclude this aspect (field/enum value). */
    fun exclude(aspect: Aspect) {
        val usage = aspect.toUsage()
        check(!included.containsKey(usage)) { "$aspect can't be excluded, because it's already included." }
        excluded.add(usage)
    }

    /** Write any additions to the file. */
    fun commit() {
        if (path == null) return
        val includes = included.map { commentFor(it.key) + it.key.toLine(it.value) }.joinToString("")
        val excludes = excluded.joinToString("") { commentFor(it) + "- " + it.toLine() }
        fs.write(path) { writeUtf8(includes + excludes) }
    }

    private fun generateIdFor(usage: Usage): Int {
        val highestId = included
            .filterKeys { it.keyword == usage.keyword && it.definition == usage.definition }
            .values
            .maxByOrNull { it ?: 0 }
            ?: 0
        return highestId + 1
    }

    private fun commentFor(usage: Usage): String {
        return comments[usage]?.joinToString("") { it + "\n" } ?: ""
    }
}

fun Definition.isUsageTracked() = when (this) {
    is Thing, is Action, is Value, is Enum -> true
    else -> false
}
private fun Definition.toUsage() = Usage(figmentKeyword(), name, null)
private fun Aspect.toUsage() = Usage(context.current.figmentKeyword(), context.current.name, when(this) {
    is EnumOption -> value // TODO looks like historically usage files had the enum value, but in the reference syntax we use [name] which is alias ?: value.  We should migrate old usage files to use [name] and get rid of this special handler.
    else -> name
})

private data class Usage(val keyword: String, val definition: String, val aspect: String?) {
    fun toLine(withId: Int? = null): String {
        val def = "$keyword $definition"
        val aspect = if (aspect == null) "" else ".$aspect"
        val id = if (withId == null) "" else " $withId"
        return def + aspect + id + "\n"
    }
}
