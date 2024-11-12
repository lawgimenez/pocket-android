package com.pocket.sync.type.path


/**
 * Parsing and representation of figment path syntax. See figment spec for more details.
 * This only contains the raw/string path data. See [Reference] for a validated/resolved version.
 *
 * Create instances using [from].
 */
data class Path private constructor(val parts: List<PathSegment> = emptyList()) {

    /** This constructor is provided for JVM users who just want to create an empty path and can't take advantage of kotlin's default params. */
    constructor() : this(emptyList())

    /** Return a new instance with a field `.field` appended.  */
    fun field(field: String): Path {
        val p: MutableList<PathSegment> = ArrayList(parts)
        p.add(PathSegment(this, field, PathSegmentType.FIELD))
        return Path(p)
    }

    /** Return a new instance with a map key `["key"]` appended.  */
    fun key(key: String): Path {
        val p: MutableList<PathSegment> = ArrayList(parts)
        p.add(PathSegment(this, key, PathSegmentType.MAP_KEY))
        return Path(p)
    }

    /** Return a new instance with an array index `[2]` appended.  */
    fun index(index: Int): Path {
        val p: MutableList<PathSegment> = ArrayList(parts)
        p.add(PathSegment(this, index.toString(), PathSegmentType.ARRAY_INDEX))
        return Path(p)
    }

    override fun toString(): String {
        val b = StringBuilder()
        for (segment in parts) {
            when (segment.type) {
                PathSegmentType.FIELD -> b.append(".").append(segment.value)
                PathSegmentType.MAP_KEY -> b.append("[\"").append(segment.value).append("\"]")
                PathSegmentType.ARRAY_INDEX -> b.append("[").append(segment.value).append("]")
            }
        }
        return b.toString()
    }

    companion object {
        fun from(provided: String?) : Path {
            var node = Path()
            if (provided == null || provided.length == 0) {
                return node
            } else {
                var path : String = provided
                while (path.isNotEmpty()) {
                    if (path[0] == '.') {
                        path = path.substring(1)
                        var end = path.length
                        val nextDot = path.indexOf(".")
                        val nextBracket = path.indexOf("[")
                        end = if (nextDot > -1) minOf(nextDot, end) else end
                        end = if (nextBracket > -1) minOf(nextBracket, end) else end
                        node = node.field(path.substring(0, end))
                        path = path.substring(end)
                    } else if (path[0] == '[') {
                        path = path.substring(1)
                        val indexEnd = path.indexOf("]")
                        val index = path.substring(0, indexEnd)
                        if (path[0] == '"') {
                            val key = index.substring(1, index.length - 1)
                            node = node.key(key)
                        } else {
                            node = node.index(index.toInt())
                        }
                        if (indexEnd + 1 < path.length) {
                            path = path.substring(indexEnd + 1)
                        } else {
                            path = ""
                        }
                    } else {
                        throw IllegalArgumentException("invalid path at $path after $node")
                    }
                }
                return node
            }
        }
    }

}


/**
 * A node/segment of a [Path].
 */
data class PathSegment(
    /** The parent path up to, but not including this node  */
    val parent: Path,
    /** The value of this node  */
    val value: String,
    /** The type of this node  */
    val type: PathSegmentType
) {

    /** The path including its parent and this node.  */
    fun path() = when (type) {
            PathSegmentType.FIELD -> parent.field(value)
            PathSegmentType.MAP_KEY -> parent.key(value)
            PathSegmentType.ARRAY_INDEX -> parent.index(value.toInt())
        }

    override fun toString(): String = path().toString()

}

enum class PathSegmentType {
    FIELD, ARRAY_INDEX, MAP_KEY
}