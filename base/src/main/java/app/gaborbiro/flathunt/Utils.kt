package app.gaborbiro.flathunt


import java.io.UnsupportedEncodingException
import java.lang.reflect.Field
import java.lang.reflect.Type
import java.net.URLDecoder
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern


fun String.matcher(regex: String): Matcher = Pattern.compile(regex).matcher(this)

fun addPadding(text: String, totalLength: Int): String {
    return if (text.length < totalLength) {
        text + String(CharArray(totalLength - text.length) { ' ' })
    } else {
        text
    }
}

fun Map<*, *>.prettyPrint(separator: String = "\n"): String {
    val longestKeyLength = keys.maxByOrNull { it.toString().length }!!.toString().length
    return map {
        addPadding(it.key.toString(), longestKeyLength + 1) + ": " + it.value
    }.joinToString(separator)
}

inline fun <reified T> T.prettyPrint(): String {
    val fields = mutableListOf<Field>().apply {
        getAllFields(this, T::class.java)
    }
    val map: Map<String, String> = fields.map {
        it.isAccessible = true
        val strValue = when (val value = it[this]) {
            is Iterable<*> -> value.joinToString(", ")
            is Array<*> -> value.joinToString(", ")
            else -> value?.toString() ?: ""
        }
        if (strValue == "missing") {
            it.name to ""
        } else {
            it.name to (strValue)
        }
    }.associate { it }
    return map.toSortedMap().prettyPrint()
}

fun getAllFields(fields: MutableList<Field>, type: Class<*>): List<Field> {
    fields.addAll(listOf(*type.declaredFields))
    if (type.superclass != null) {
        getAllFields(fields, type.superclass)
    }
    return fields
}

fun toType(arg: String, type: Type): Any {
    return when {
        type.typeName.contains(java.lang.Integer::class.java.name) -> arg.toInt()
        type.typeName.contains(java.lang.Float::class.java.name) -> arg.toFloat()
        type.typeName.contains(java.lang.Double::class.java.name) -> arg.toDouble()
        type.typeName.contains(java.lang.Boolean::class.java.name) -> arg.toBoolean()
        type.typeName.contains(java.lang.Long::class.java.name) -> arg.toLong()
        else -> arg
    }
}

@Throws(UnsupportedEncodingException::class)
fun splitQuery(url: String): Map<String, String?> {
    val queryPairs: MutableMap<String, String?> = LinkedHashMap()
    val pairs = url.split("&").toTypedArray()
    pairs.forEach { pair ->
        val idx = pair.indexOf("=")
        val key = if (idx > 0) URLDecoder.decode(pair.substring(0, idx), "UTF-8") else pair
        val value = if (idx > 0 && pair.length > idx + 1) URLDecoder.decode(pair.substring(idx + 1), "UTF-8") else null
        queryPairs[key] = value
    }
    return queryPairs
}

fun String.or(default: String? = null) = if (isBlank()) default else trim()

fun Collection<*>.orNull() = if (this.isEmpty()) null else this

fun nostrict(lambda: () -> Unit) {
    if (GlobalVariables.strict.not()) {
        lambda()
    }
}

data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
