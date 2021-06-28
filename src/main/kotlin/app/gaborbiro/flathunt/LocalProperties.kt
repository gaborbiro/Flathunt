package app.gaborbiro.flathunt

import java.io.File
import java.io.IOException
import java.util.*

object LocalProperties {

    private val configProp: Properties = Properties()

    init {
        try {
            configProp.load(File("local.properties").inputStream())
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    val googleApiKey: String
        get() = configProp.getProperty(GOOGLE_API_KEY_KEY)

    fun getProperty(key: String?): String {
        return configProp.getProperty(key)
    }

    val allPropertyNames: Set<String>
        get() = configProp.stringPropertyNames()

    fun containsKey(key: String?): Boolean {
        return configProp.containsKey(key)
    }
}

private const val GOOGLE_API_KEY_KEY = "google.api.key"