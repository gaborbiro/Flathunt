package app.gaborbiro.flathunt

import app.gaborbiro.flathunt.data.model.Cookies
import app.gaborbiro.flathunt.google.TravelLimit
import app.gaborbiro.flathunt.google.TravelMode
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import java.io.UnsupportedEncodingException
import java.lang.reflect.Field
import java.lang.reflect.Type
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern


@Throws(Exception::class)
fun callGet(url: String): String {
    val connection = URL(url).openConnection()
    return connection.inputStream.bufferedReader().use { it.readText() }.also {
        (connection as HttpURLConnection).disconnect()
    }
}

fun callPost(url: String, payload: String, cookies: Cookies): Boolean {
    val type = MediaType.get("application/json; charset=utf-8")
    val body = RequestBody.create(type, payload)
    val request = Request.Builder()
        .addHeader("Cookie", cookies.cookies.joinToString("; "))
        .addHeader("Content-Type", "application/json")
        .url(url)
        .post(body)
        .build()
    if (GlobalVariables.debug) {
        println(request.toString() + "\n" + payload)
    }
    val result = OkHttpClient().newCall(request).execute()
    return if (result.code() < 200 || result.code() > 299) {
        println("Error ${result.code()}: " + result.message())
        false
    } else {
        true
    }
}

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
    return map.prettyPrint()
}

fun getAllFields(fields: MutableList<Field>, type: Class<*>): List<Field> {
    fields.addAll(listOf(*type.declaredFields))
    if (type.superclass != null) {
        getAllFields(fields, type.superclass)
    }
    return fields
}

fun WebDriver.findRegex(regex: String): Array<String>? {
    val matcher = pageSource.matcher(regex)
    return if (matcher.find()) {
        (1..matcher.groupCount()).map { matcher.group(it) }.toTypedArray()
    } else {
        null
    }
}

fun WebDriver.findSimpleText(xpath: String): String? {
    return runCatching { findElement(By.xpath(xpath)).text }.getOrNull()
}

fun WebDriver.linkExists(linkText: String): Boolean {
    return runCatching { findElement(By.linkText(linkText)) }.getOrNull() != null
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

infix fun Int.minutes(travelMode: TravelMode) = TravelLimit(travelMode, this)

infix fun TravelLimit.or(travelLimit: TravelLimit) = arrayOf(this, travelLimit)
infix fun Array<TravelLimit>.or(travelLimit: TravelLimit) = this + travelLimit

infix fun TravelLimit.or(minutes: Int) = TravelLimitBuilder(arrayOf(this), minutes)
infix fun Array<TravelLimit>.or(minutes: Int) = TravelLimitBuilder(this, minutes)

class TravelLimitBuilder(private val maxes: Array<TravelLimit>, private val minutes: Int) {
    fun build(travelMode: TravelMode) = maxes or (minutes minutes travelMode)
}

infix fun TravelLimitBuilder.minutes(travelMode: TravelMode) = build(travelMode)

fun Double.decimals(digits: Int = 2): String {
    return "%.${digits}f".format(this)
}

fun Float.decimals(digits: Int = 2): String {
    return "%.${digits}f".format(this)
}

fun Collection<*>.orNull() = if (this.isEmpty()) null else this

fun strict(lambda: () -> Unit) {
    if (!GlobalVariables.strict) {
        lambda()
    }
}

fun escapeHTML(s: String): String {
    val out = StringBuilder(16.coerceAtLeast(s.length))
    for (element in s) {
        if (element.code > 127 || element == '"' || element == '\'' || element == '<' || element == '>' || element == '&') {
            out.append("&#")
            out.append(element.code)
            out.append(';')
        } else {
            out.append(element)
        }
    }
    return out.toString()
}

