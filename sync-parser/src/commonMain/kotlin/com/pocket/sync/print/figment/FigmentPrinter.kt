package com.pocket.sync.print.figment

import com.pocket.sync.Figments
import com.pocket.sync.parse.DescriptionType
import com.pocket.sync.parse.PropertyData
import com.pocket.sync.parse.Source
import com.pocket.sync.parse.TypeFlag
import com.pocket.sync.type.*
import com.pocket.sync.type.Enum
import okio.FileSystem
import okio.Path

const val INDENT = "    "

fun Figments.toFigmentFiles() : Map<Path, String> {
    // Get all definitions by source file
    val map = mutableMapOf<Path, MutableList<Definition>>()
    all().filterNot { it is Action && it.remoteBaseOf != null }
            .filterNot { it is Synthetic }
            .forEach { map.getOrPut(it.data.source.path) { mutableListOf() }.add(it) }

    val out = mutableMapOf<Path, String>()
    for ((key, definitions) in map) {
        out[key] = definitions.sortedBySource().joinToString("") { it.toFigment() }
    }
    return out.toMap()
}

fun Figments.writeToFigmentFiles(fs: FileSystem, outDirectory: Path) {
    toFigmentFiles().forEach { (path, text) ->
        var name = path.name
        if (name.isEmpty()) name = "out.figment"
        val out = outDirectory/name
        fs.write(out) { writeUtf8(text) }
    }
}

fun Property.toFigment() : String {
    return when(this) {
        is Definition -> toFigment()
        is Field -> toFigment()
        is EnumOption -> toFigment()
        is AuthFlag -> toFigment()
        is RemoteFlag -> toFigment()
        is UniqueFlag -> toFigment()
        is PriorityFlag -> toFigment()
        is ResolvesFlag -> toFigment()
        is RelatedFeature -> toFigment()
        is Endpoint -> toFigment()
        is FirstAvailable -> toFigment()
        is Instructions -> toFigment()
        is Reactives -> toFigment()
        is Remap -> toFigment()
        is Segment -> toFigment()
        is Spacer -> toFigment()
        is VarietyOption -> toFigment()
        is SliceRule -> toFigment()
        else -> throw RuntimeException("printer doesn't yet support $this")
    }
}

fun Definition.toFigment() : String {
    // Header
    var header = ""
    // Deprecated
    header += this.deprecated.then { "- " }
    // Special keywords
    when {
        this is Remote && this.default || this is Auth && this.default -> header += "default "
        this is Action && this.isBase -> header += "base "
        this is Syncable<*> && this.isInterface -> header += "interface "
    }
    // Keyword & Name
    header += "${figmentKeyword()} ${this.name}"
    // Interfaces
    val interfaces = if (this is Syncable<*>) this.interfaces.filter { it !is ActionInterface || !it.isBase } else null // Exclude base actions that might be included
    if (!interfaces.isNullOrEmpty()) {
        header += " : "
        header += interfaces.joinToString(", ") { it.name }
    }
    // Variety Options
    else if (this is Variety && data.source.isSingleLine()) {
        header += " : "
        header += options.filterNot { it.type is Synthetic }.joinToString(", ") { it.type.name }
    }
    // Body
    return header + body(this, properties())
}

fun Definition.figmentKeyword() = when (this) {
    is Thing -> "thing"
    is Action -> "action"
    is Value -> "value"
    is Enum -> "enum"
    is Remote -> "remote"
    is Auth -> "auth"
    is Feature -> "feature"
    is Variety -> "variety"
    is Slice -> "slice"
    else -> throw RuntimeException("$this isn't a known definition type.")
}

fun Definition.properties() : List<Property> {
    val props = mutableListOf<Property?>()
    props.addAll(description.self)
    props.addAll(related.self)
    if (this is Syncable<*>) {
        props.add(auth.self)
        props.add(remote.self)
        props.add(endpoint.self)
        props.addAll(fields.self)
    }
    if (this is Thing) props.add(unique.self)
    if (this is Action) {
        props.addAll(effect.self)
        props.add(priority.self)
        props.add(resolves.self)
    }
    if (this is Enum) props.addAll(options)
    if (this is Remote) {
        props.add(baseAction)
    }
    if (this is Variety && data.source.isMultiline()) {
        props.addAll(options.filterNot { it.type is Synthetic })
    }
    if (this is Slice) {
        props.addAll(rules)
    }
    return props.filterNotNull().toList()
}

fun FieldType.toFigment() : String {
    return when(this) {
        is ReferenceType<*> -> toFigment()
        is ListType -> toFigment()
        is MapType -> toFigment()
        is VarietyType -> toFigment()
        is InterfaceType -> toFigment()
        else -> throw RuntimeException("printer doesn't yet support $this")
    }
}
fun DefinitionType<*>.toFigment() = definition.name + flag.toFigment()
fun ListType.toFigment() = "[${inner.toFigment()}]" + flag.toFigment()
fun MapType.toFigment() = "Map<${inner.toFigment()}>" + flag.toFigment()
fun TypeFlag.toFigment() = when (this) {
    TypeFlag.UNSPECIFIED -> ""
    TypeFlag.REQUIRED -> "!"
    TypeFlag.OPTIONAL -> "?"
}

fun EnumOption.toFigment() : String {
    var s = ""
    s += deprecated.then { "- " }
    s += value
    s += alias.then { " = $alias" }
    s += body(this, description.self)
    return s
}
fun Field.toFigment() : String {
    var s = ""
    s += deprecated.then { "-" }
    s += identifying.then { "=" }
    s += hashTarget.then { "%" }
    s += localOnly.then { "_" }
    s += root.then { "->" }
    s += s.isNotEmpty().then { " " }
    if (aliases.isNotEmpty()) {
        s += "("
        s += aliases.map { "${it.value} @ ${it.key.name}" }.joinToString(", ")
        s += ")"
    } else {
        s += name
    }
    s += " : "
    s += type.toFigment()
    s += body(this, description.self.plus(derives.all()))
    return s
}
fun VarietyOption.toFigment() = "${type.name}${body(this, description.self)}"
fun SliceRule.toFigment() = toString() + "\n"
fun FirstAvailable.toFigment() = "-> " + options.joinToString(" ?: ") { it.toString() } + "\n"
fun Remap.toFigment() = "-> [] -> ${list.toString().drop(1)}.$field\n"
fun Instructions.toFigment() = "~ $instruction\n"
fun Reactives.toFigment() = "(" + reactives.joinToString(", ") { it.toString() } + ")\n"
fun Endpoint.toFigment() = "@ $address" + method.then{ " $it" } + "\n"
fun AuthFlag.toFigment() = "! ${auth.name}\n"
fun RemoteFlag.toFigment() = "> ${remote.name}\n"
fun UniqueFlag.toFigment() = "=\n"
fun RelatedFeature.toFigment() = "^ ${related.name}\n"
fun PriorityFlag.toFigment() = "* ${priority.name}\n"
fun ResolvesFlag.toFigment() = "-> ${type.toFigment()}\n"
fun Segment.toFigment() = text.lines().joinToString("") { "${type.toFigment()} $it".trim() + "\n" }
fun DescriptionType.toFigment() = when (this) {
    DescriptionType.DESCRIPTION -> "#"
    DescriptionType.EFFECT, DescriptionType.INSTRUCTION -> "~"
}

private fun body(of: Property, properties: List<Property?>) : String {
    val singleLine = of.data.source.isSingleLine()
    // If single line there should either by no properties, or just a single trailing description.
    if (properties.isEmpty() && singleLine) return "\n"
    val description = properties.filterIsInstance<Segment>().firstOrNull()
    if (description != null && properties.size == 1 && singleLine) return " ${description.toFigment()}"

    // Else use the multiline block.
    return " {\n" +
            properties.filterNotNull().sortedBySource().joinToString("") { it.toFigment() } // First pass is not indented, assumes all toFigment() calls return a line with a trailing line break
                .lines().dropLast(1).joinToString("") { if (it.isBlank()) "\n" else INDENT+it+"\n" } + // Then apply indenting, emptying out lines that are only whitespace.
            "}\n"
}

private fun <T> T?.then(then: (T) -> String) : String = if (this != null) then.invoke(this) else ""
private fun Boolean?.then(then: () -> String) : String = if (this != null && this) then.invoke() else ""

private fun List<Property>.sortedBySource() : List<Property> {
    val sorted = sortedWith(compareBy<Property> { it.data.source.path }.thenBy { it.data.source.startLine })

    // Insert spacers where properties had blank lines between them
    val withSpaces = mutableListOf<Property>()
    sorted.forEachIndexed { i, p ->
        val start = p.data.source.startLine
        if (i > 0) {
            val previousEnd = sorted[i-1].data.source.endLine
            if (start-previousEnd > 1) withSpaces.add(Spacer(SpacerData(p.data.source.copy(startLine = previousEnd+1, endLine = start-1)), p.context))
        }
        withSpaces.add(p)
    }

    return withSpaces.toList()
}

private class Spacer(override val data: SpacerData, override val context: Context) : Property {
    fun toFigment() = "\n".repeat(data.source.lines())
    override fun equals(other: Any?) = dataEquals(this, other)
    override fun hashCode() = dataHashCode(this)
}
private data class SpacerData(override val source: Source) : PropertyData