package app.gaborbiro.flathunt.data

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*

class DateTypeAdapter : TypeAdapter<Date>() {
    override fun write(writer: JsonWriter, value: Date?) {
        println(value)
    }

    override fun read(reader: JsonReader): Date {
//        val str = reader.nextString()
//        return LocalDateTime.parse(str, DateTimeFormatter.ofPattern("MMM d, YYYY, h:mm:ss A", Locale.US))
//            .toEpochSecond(ZoneOffset.UTC)
//            .let { Date(it * 1000) }
        return Date.from(LocalDateTime.now().plusDays(1L).atZone(ZoneId.systemDefault()).toInstant())
    }
}