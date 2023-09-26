package app.gaborbiro.flathunt.data.domain

import app.gaborbiro.flathunt.data.domain.model.Cookies
import app.gaborbiro.flathunt.data.domain.model.PersistedProperty
import app.gaborbiro.flathunt.data.domain.model.Property

interface Store {
    fun getJsonProperties(): String?

    fun getProperties(): List<PersistedProperty>

    fun overrideProperties(properties: List<Property>)

    fun overrideJsonProperties(json: String)

    fun clearProperties()

    fun getBlacklistWebIds(): List<String>

    fun saveBlacklistWebIds (webIds: List<String>)

    fun clearBlacklist()

    fun saveCookies(cookies: Cookies)

    fun getCookies(): Cookies?

    fun clearCookies()

    fun resetIndexCounter()
}
