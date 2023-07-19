package app.gaborbiro.flathunt.repo.domain

import app.gaborbiro.flathunt.data.domain.model.PersistedProperty
import app.gaborbiro.flathunt.data.domain.model.Property

interface PropertyRepository {

    fun getProperty(indexOrId: String): PersistedProperty?

    fun getProperties(): List<PersistedProperty>

    fun addOrUpdateProperty(property: Property): Boolean

    fun deleteProperty(index: Int, markAsUnsuitable: Boolean, safeMode: Boolean): Boolean

    fun getPropertyUrl(id: String): String

    fun clearProperties()

    fun verifyAll()

    fun getBlacklist(): List<String>

    fun addToBlacklist(ids: List<String>)

    fun clearBlacklist()

    fun openLinks(property: Property)

    fun markAsUnsuitable(property: Property, unsuitable: Boolean)

    fun getNextProperty(indexOrId: String): PersistedProperty?

    fun reindex()
}