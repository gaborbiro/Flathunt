package app.gaborbiro.flathunt.data.domain

import app.gaborbiro.flathunt.data.domain.model.CookieSet
import app.gaborbiro.flathunt.data.domain.model.Property

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
}
