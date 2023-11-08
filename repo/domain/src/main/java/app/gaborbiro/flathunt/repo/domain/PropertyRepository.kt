package app.gaborbiro.flathunt.repo.domain

import app.gaborbiro.flathunt.data.domain.model.Property

interface PropertyRepository {

    fun getProperty(idx: String): Property?

    fun getProperties(): List<Property>

    fun addOrUpdateProperty(property: Property): Boolean

    fun deleteProperty(index: Int, markAsUnsuitable: Boolean): Boolean

    fun getPropertyUrl(webId: String): String

    fun clearProperties(): Int

    fun validate()

    fun getBlacklist(): List<String>

    fun addToBlacklist(webId: String)

    fun clearBlacklist(): Int

    fun openLinks(property: Property)

    fun markAsUnsuitable(webId: String, unsuitable: Boolean)

    fun getNextProperty(idx: String): Property?

    fun reindex()
}