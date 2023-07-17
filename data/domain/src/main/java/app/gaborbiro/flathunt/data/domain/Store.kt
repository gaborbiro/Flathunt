package app.gaborbiro.flathunt.data.domain

import app.gaborbiro.flathunt.data.domain.model.Cookies
import app.gaborbiro.flathunt.data.domain.model.PersistedProperty
import app.gaborbiro.flathunt.data.domain.model.Property

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
