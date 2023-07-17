package app.gaborbiro.flathunt.data

import app.gaborbiro.flathunt.data.model.Cookies
import app.gaborbiro.flathunt.data.model.PersistedProperty
import app.gaborbiro.flathunt.data.model.Property
import com.google.gson.Gson

class StoreImpl(
    serviceName: String,
    tag: String? = null,
) : Store {

    private val gson = Gson()

    private val prefPropertiesKey = "${PREF_PROPERTIES_KEY_BASE}_$serviceName" + tag?.let { "_$tag" }
    private val prefIndexKey = "${PREF_INDEX_KEY_BASE}_$serviceName" + tag?.let { "_$tag" }
    private val prefCookiesKey = "${PREF_COOKIES_KEY_BASE}_$serviceName" + tag?.let { "_$tag" }
    private val prefBlacklistKey = "${PREF_BLACKLIST_KEY_BASE}_$serviceName" + tag?.let { "_$tag" }

    // START Properties

    override fun getJsonProperties(): String? {
        return Preferences.get(prefPropertiesKey, null)
    }

    override fun getProperties(): List<PersistedProperty> {
        val jsonProperties = getJsonProperties() ?: return emptyList()
        return gson.fromJson(jsonProperties, PropertiesWrapper::class.java).data
    }

    override fun saveProperties(properties: List<Property>) {
        val jsonProperties = gson.toJson(PropertiesWrapper(properties.map {
            if (it is PersistedProperty) {
                it
            } else {
                PersistedProperty(it, getNextIndex().also { index -> println("new index: $index") })
            }
        }))
        saveJsonProperties(jsonProperties)
    }

    override fun saveJsonProperties(json: String) {
        Preferences.save(prefPropertiesKey, json)
    }

    override fun clearProperties() {
        Preferences.clear(prefPropertiesKey)
    }

    // END Properties

    // START Blacklist
    override fun getBlacklist(): List<String> {
        return Preferences.get(prefBlacklistKey, "")!!.split(",")
    }

    override fun saveBlacklist(ids: List<String>) {
        Preferences.save(prefBlacklistKey, ids.joinToString(","))
    }

    override fun clearBlacklist() {
        Preferences.clear(prefBlacklistKey)
    }

    // END Blacklist

    private fun getNextIndex(): Int {
        return synchronized(this) {
            val index = Preferences.getInt(prefIndexKey, 0)
            Preferences.setInt(prefIndexKey, index + 1)
            index + 1
        }
    }

    override fun saveCookies(cookies: Cookies) {
        Preferences.save(prefCookiesKey, gson.toJson(cookies))
    }

    override fun getCookies(): Cookies? {
        return Preferences.get(prefCookiesKey, null)?.let {
            gson.fromJson(it, Cookies::class.java)
        }
    }

    override fun clearCookies() {
        Preferences.clear(prefCookiesKey)
    }

    override fun resetIndexes() {
        Preferences.clear(prefIndexKey)
    }
}

private const val PREF_PROPERTIES_KEY_BASE = "properties"
private const val PREF_INDEX_KEY_BASE = "index"
private const val PREF_COOKIES_KEY_BASE = "cookies"
private const val PREF_BLACKLIST_KEY_BASE = "blacklist"