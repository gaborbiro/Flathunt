package app.gaborbiro.flathunt.repo.domain

import app.gaborbiro.flathunt.data.domain.model.Property

interface PropertyRepository {

    fun getProperty(idx: String): Property?

    fun getProperties(): List<Property>

    fun addOrUpdateProperty(property: Property): Boolean

    fun deleteProperty(index: Int, markAsUnsuitable: Boolean, safeMode: Boolean): Boolean

    fun getPropertyUrl(webId: String): String

    fun clearProperties()

    fun verify(directions: Boolean)

    fun getBlacklist(): List<String>

    fun addToBlacklist(webIds: List<String>)

    fun clearBlacklist()

    fun openLinks(property: Property)

    fun markAsUnsuitable(webId: String, unsuitable: Boolean)

    fun getNextProperty(idx: String): Property?

    fun reindex()
}