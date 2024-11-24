package app.gaborbiro.flathunt.data.domain

import app.gaborbiro.flathunt.data.domain.model.CookieSet
import app.gaborbiro.flathunt.data.domain.model.Property
import org.openqa.selenium.Dimension
import org.openqa.selenium.Point

interface Store {
    fun getJsonProperties(): String?

    fun getProperties(): List<Property>

    fun overrideProperties(properties: List<Property>)

    fun overrideJsonProperties(json: String)

    fun clearProperties(): Int

    fun getBlacklistWebIds(): List<String>

    fun saveBlacklistWebIds(webIds: List<String>)

    fun clearBlacklist(): Int

    fun setCookies(cookies: CookieSet)

    fun getCookies(): CookieSet?

    fun clearCookies()

    fun resetIndexCounter()

    fun setWindowPosition(point: Point)

    fun getWindowPosition(): Point

    fun setWindowSize(size: Dimension)

    fun getWindowSize(): Dimension
}
