package app.gaborbiro.flathunt.data

import app.gaborbiro.flathunt.data.model.Cookies
import app.gaborbiro.flathunt.data.model.PersistedProperty
import app.gaborbiro.flathunt.data.model.Property

interface Store {
    fun getJsonProperties(): String?

    fun getProperties(): List<PersistedProperty>

    fun saveProperties(properties: List<Property>)

    fun saveJsonProperties(json: String)

    fun clearProperties()

    fun getBlacklist(): List<String>

    fun saveBlacklist(ids: List<String>)

    fun clearBlacklist()

    fun saveCookies(cookies: Cookies)

    fun getCookies(): Cookies?

    fun clearCookies()

    fun resetIndexes()
}
