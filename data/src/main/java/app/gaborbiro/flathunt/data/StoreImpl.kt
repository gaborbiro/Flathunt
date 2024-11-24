package app.gaborbiro.flathunt.data

import app.gaborbiro.flathunt.console.ConsoleWriter
import app.gaborbiro.flathunt.data.domain.Store
import app.gaborbiro.flathunt.data.domain.model.CookieSet
import app.gaborbiro.flathunt.data.domain.model.Property
import com.google.gson.GsonBuilder
import org.koin.core.annotation.Named
import org.koin.core.annotation.Singleton
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.openqa.selenium.Dimension
import org.openqa.selenium.Point

@Singleton
class StoreImpl(
    @Named("serviceName") serviceName: String,
    @Named("criteria") criteria: String,
) : Store, KoinComponent {

    private val console: ConsoleWriter by inject()

    private val gson = GsonBuilder()
//        .registerTypeAdapter(Date::class.java, DateTypeAdapter())
        .create()

    private val prefPropertiesKey = "${PREF_PROPERTIES_KEY_BASE}_${serviceName}_$criteria"
    private val prefIndexKey = "${PREF_INDEX_KEY_BASE}_${serviceName}_$criteria"
    private val prefCookiesKey = "${PREF_COOKIES_KEY_BASE}_${serviceName}_$criteria"
    private val prefBlacklistKey = "${PREF_BLACKLIST_KEY_BASE}_${serviceName}_$criteria"

    // START Properties

    override fun getJsonProperties(): String? {
        return Registry.get(prefPropertiesKey, null)
    }

    override fun getProperties(): List<Property> {
        val jsonProperties = getJsonProperties() ?: return emptyList()
        return gson.fromJson(jsonProperties, PropertiesWrapper::class.java).data
    }

    override fun overrideProperties(properties: List<Property>) {
        val jsonProperties = gson.toJson(PropertiesWrapper(properties.map {
            if (it.index != null) {
                it
            } else {
                it.copy(index = nextIndex().also { index -> console.i("new index: $index") })
            }
        }))
        overrideJsonProperties(jsonProperties)
    }

    override fun overrideJsonProperties(json: String) {
        Registry.save(prefPropertiesKey, json)
    }

    override fun clearProperties(): Int {
        val count = getProperties().size
        Registry.clear(prefPropertiesKey)
        return count
    }

    // END Properties

    // START Blacklist
    override fun getBlacklistWebIds(): List<String> {
        return Registry.get(prefBlacklistKey, null)?.split(",") ?: emptyList()
    }

    override fun saveBlacklistWebIds(webIds: List<String>) {
        Registry.save(prefBlacklistKey, webIds.joinToString(","))
    }

    override fun clearBlacklist(): Int {
        val count = getBlacklistWebIds().size
        Registry.clear(prefBlacklistKey)
        return count
    }

    // END Blacklist

    private fun nextIndex(): Int {
        return synchronized(this) {
            val index = Registry.getInt(prefIndexKey, 0)
            Registry.setInt(prefIndexKey, index + 1)
            index + 1
        }
    }

    override fun setCookies(cookies: CookieSet) {
        Registry.save(prefCookiesKey, gson.toJson(cookies))
    }

    override fun getCookies(): CookieSet? {
        return Registry.get(prefCookiesKey, null)?.let {
            gson.fromJson(it.replace(Regex("\\u202f"), " "), CookieSet::class.java)
        }
    }

    override fun clearCookies() {
        Registry.clear(prefCookiesKey)
    }

    override fun resetIndexCounter() {
        Registry.clear(prefIndexKey)
    }

    override fun setWindowPosition(point: Point) {
        Registry.setInt(PREF_WINDOW_POSITION_KEY_X, point.x)
        Registry.setInt(PREF_WINDOW_POSITION_KEY_Y, point.y)
    }

    override fun getWindowPosition(): Point {
        return Point(
            Registry.getInt(PREF_WINDOW_POSITION_KEY_X, 0),
            Registry.getInt(PREF_WINDOW_POSITION_KEY_Y, 0),
        )
    }

    override fun setWindowSize(size: Dimension) {
        Registry.setInt(PREF_WINDOW_SIZE_KEY_WIDTH, size.width)
        Registry.setInt(PREF_WINDOW_SIZE_KEY_HEIGHT, size.height)
    }

    override fun getWindowSize(): Dimension {
        return Dimension(
            /* width = */ Registry.getInt(PREF_WINDOW_SIZE_KEY_WIDTH, -1),
            /* height = */ Registry.getInt(PREF_WINDOW_SIZE_KEY_HEIGHT, -1),
        )
    }
}

private const val PREF_PROPERTIES_KEY_BASE = "properties"
private const val PREF_INDEX_KEY_BASE = "index"
private const val PREF_COOKIES_KEY_BASE = "cookies"
private const val PREF_BLACKLIST_KEY_BASE = "blacklist"
private const val PREF_WINDOW_POSITION_KEY_X = "window_position_x"
private const val PREF_WINDOW_POSITION_KEY_Y = "window_position_y"
private const val PREF_WINDOW_SIZE_KEY_HEIGHT = "window_size_height"
private const val PREF_WINDOW_SIZE_KEY_WIDTH = "window_size_width"