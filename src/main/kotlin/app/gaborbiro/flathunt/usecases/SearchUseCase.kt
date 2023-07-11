package app.gaborbiro.flathunt.usecases

import app.gaborbiro.flathunt.*
import app.gaborbiro.flathunt.data.Store
import app.gaborbiro.flathunt.data.model.PersistedProperty
import app.gaborbiro.flathunt.data.model.Property
import app.gaborbiro.flathunt.google.calculateRoutes
import app.gaborbiro.flathunt.service.Page
import app.gaborbiro.flathunt.service.Service

class SearchUseCase(private val service: Service, private val store: Store, criteria: ValidationCriteria) :
    BaseUseCase(service, store, criteria) {

    override val commands
        get() = listOf(search)

    private val search = command<String>(
        command = "search",
        description = "Reads new properties from a search url. Marks, saves and prints found properties that are valid",
        argumentName = "search url",
    )
    { (searchUrl) ->
        fetchSearchResults(searchUrl, GlobalVariables.safeMode)
    }

    private fun fetchSearchResults(searchUrl: String, safeMode: Boolean) {
        val savedIds = store.getProperties().map { it.id }
        val addedIds = mutableListOf<String>()
        val failedIds = mutableListOf<String>()
        var page: Page? = service.fetchLinksFromSearch(searchUrl)
        while (page != null) {
            val thePage = page
            println("page ${thePage.page}/${thePage.pageCount}")
            val ids = thePage.urls.map { service.getPropertyIdFromUrl(it) }
            val newIds: List<String> = ids - store.getBlacklist() - savedIds // we don't re-check known properties
            if (newIds.isNotEmpty()) {
                newIds.forEachIndexed { i, id ->
                    print("\n=======> Fetching property $id (${i + 1}/${newIds.size}; page ${thePage.page}/${thePage.pageCount}): ")
                    try {
                        val property = service.fetchProperty(id)
                        processProperty(id, property, safeMode, thePage)?.let {
                            addedIds.add(it)
                        }
                    } catch (t: Throwable) {
                        t.printStackTrace()
                        failedIds.add(id)
                    }
                }
            }
            page = page.nextPage(page)
        }
        println("\nFinished")
        if (addedIds.isNotEmpty()) {
            println("New ids: ${addedIds.joinToString(",")}")
        }
        if (failedIds.isNotEmpty()) {
            println("Failed ids: ${failedIds.joinToString(",")}")
        }
    }

    /**
     * @return valid id if property was added, null otherwise (rejected or updated only)
     */
    private fun processProperty(id: String, property: Property, safeMode: Boolean, page: Page): String? {
        println(property.title)
        // pre-validate to save on the Google Maps API call
        if (!property.checkValid(criteria)) {
            if (!property.markedUnsuitable && !safeMode) {
                service.markAsUnsuitable(id, (property as? PersistedProperty)?.index, true)
                page.propertiesRemoved++
            }
        } else {
            val routes = calculateRoutes(property, criteria.pointsOfInterest)
            val propertyWithRoutes = property.withRoutes(routes)
            val errors = propertyWithRoutes.validate(criteria)
            if (errors.isNotEmpty()) {
                println(propertyWithRoutes.routes?.joinToString(""))
                println("\nRejected: ${errors.joinToString()}")
                if (!safeMode) {
                    service.markAsUnsuitable(id, (propertyWithRoutes as? PersistedProperty)?.index, true)
                    page.propertiesRemoved++
                }
            } else {
                if (addOrUpdateProperty(propertyWithRoutes)) {
                    return propertyWithRoutes.id
                }
                println()
                println(store.getProperties().firstOrNull { it.id == propertyWithRoutes.id }
                    ?.prettyPrint()
                    ?: "Oops. Couldn't find property we just saved (${propertyWithRoutes.id})")
            }
        }
        return null
    }
}