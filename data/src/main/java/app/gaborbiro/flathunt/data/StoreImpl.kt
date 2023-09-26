package app.gaborbiro.flathunt.data

import app.gaborbiro.flathunt.console.ConsoleWriter
import app.gaborbiro.flathunt.data.domain.Store
import app.gaborbiro.flathunt.data.domain.model.Cookies
import app.gaborbiro.flathunt.data.domain.model.PersistedProperty
import app.gaborbiro.flathunt.data.domain.model.Property
import com.google.gson.Gson
import org.koin.core.annotation.Named
import org.koin.core.annotation.Singleton
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@Singleton
class StoreImpl(
    @Named("serviceName") serviceName: String,
    @Named("criteria") criteria: String,
) : Store, KoinComponent {

    private val console: ConsoleWriter by inject()

    private val gson = Gson()

    private val prefPropertiesKey = "${PREF_PROPERTIES_KEY_BASE}_${serviceName}_$criteria"
    private val prefIndexKey = "${PREF_INDEX_KEY_BASE}_${serviceName}_$criteria"
    private val prefCookiesKey = "${PREF_COOKIES_KEY_BASE}_${serviceName}_$criteria"
    private val prefBlacklistKey = "${PREF_BLACKLIST_KEY_BASE}_${serviceName}_$criteria"

    // START Properties

    override fun getJsonProperties(): String? {
        return Preferences.get(prefPropertiesKey, null)
    }

    override fun getProperties(): List<PersistedProperty> {
        val jsonProperties = getJsonProperties() ?: return emptyList()
        return gson.fromJson(jsonProperties, PropertiesWrapper::class.java).data
    }

    override fun overrideProperties(properties: List<Property>) {
        val jsonProperties = gson.toJson(PropertiesWrapper(properties.map {
            if (it is PersistedProperty) {
                it
            } else {
                PersistedProperty(it, nextIndex().also { index -> console.i("new index: $index") })
            }
        }))
        overrideJsonProperties(jsonProperties)
    }

    override fun overrideJsonProperties(json: String) {
        Preferences.save(prefPropertiesKey, json)
    }

    override fun clearProperties() {
        Preferences.clear(prefPropertiesKey)
    }

    // END Properties

    // START Blacklist
    override fun getBlacklistWebIds(): List<String> {
        return Preferences.get(prefBlacklistKey, "")!!.split(",")
    }

    override fun saveBlacklistWebIds(webIds: List<String>) {
        Preferences.save(prefBlacklistKey, webIds.joinToString(","))
    }

    override fun clearBlacklist() {
        Preferences.clear(prefBlacklistKey)
    }

    // END Blacklist

    private fun nextIndex(): Int {
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

    override fun resetIndexCounter() {
        Preferences.clear(prefIndexKey)
    }
}

private const val PREF_PROPERTIES_KEY_BASE = "properties"
private const val PREF_INDEX_KEY_BASE = "index"
private const val PREF_COOKIES_KEY_BASE = "cookies"
private const val PREF_BLACKLIST_KEY_BASE = "blacklist"