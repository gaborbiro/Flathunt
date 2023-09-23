package app.gaborbiro.flathunt.repo

import app.gaborbiro.flathunt.GlobalVariables
import app.gaborbiro.flathunt.ValidationCriteria
import app.gaborbiro.flathunt.data.domain.Store
import app.gaborbiro.flathunt.data.domain.model.Property
import app.gaborbiro.flathunt.google.calculateRoutes
import app.gaborbiro.flathunt.prettyPrint
import app.gaborbiro.flathunt.repo.domain.PropertyRepository
import app.gaborbiro.flathunt.repo.domain.SearchRepository
import app.gaborbiro.flathunt.request.RequestCaller
import app.gaborbiro.flathunt.service.domain.Service
import app.gaborbiro.flathunt.service.domain.model.Page
import org.koin.core.annotation.Singleton
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@Singleton
class SearchRepositoryImpl : SearchRepository, KoinComponent {

    private val store: Store by inject()
    private val service: Service by inject()
    private val criteria: ValidationCriteria by inject()
    private val validator: PropertyValidator by inject()
    private val propertyRepository: PropertyRepository by inject()
    private val requestCaller: RequestCaller by inject()

    override fun fetchSearchResults(searchUrl: String) {
        val savedIds = store.getProperties().map { it.id }
        val addedIds = mutableListOf<String>()
        val failedIds = mutableListOf<String>()
        var page: Page? = service.fetchLinksFromSearch(searchUrl)
        while (page != null) {
            val thePage = page
            println("Fetching page ${thePage.page}/${thePage.pageCount}")
            val ids = thePage.urls.map { service.getPropertyIdFromUrl(it) }
            val newIds: List<String> =
                ids - store.getBlacklist().toSet() - savedIds.toSet() // we don't re-check known properties
            if (newIds.isNotEmpty()) {
                newIds.forEachIndexed { i, id ->
                    print("\n=======> Fetching property $id (${i + 1}/${newIds.size}; page ${thePage.page}/${thePage.pageCount}): ")
                    try {
                        val property = service.fetchProperty(id)
                        processProperty(property, thePage)?.let {
                            addedIds.add(it)
                        }
                    } catch (t: Throwable) {
                        t.printStackTrace()
                        failedIds.add(id)
                    }
                }
            }
            page = page.nextPage(page)?.let { service.fetchLinksFromSearch(it) }
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
    private fun processProperty(property: Property, page: Page): String? {
        println(property.title)
        // pre-validate to save on the Google Maps API call
        if (!validator.checkValid(property)) {
            if (!property.markedUnsuitable && !GlobalVariables.safeMode) {
                propertyRepository.markAsUnsuitable(property, unsuitable = true)
                page.propertiesRemoved++
            }
        } else {
            val routes = calculateRoutes(property.location, criteria.pointsOfInterest, requestCaller)
            val propertyWithRoutes = property.withRoutes(routes)
            val errors = validator.validate(propertyWithRoutes)
            if (errors.isNotEmpty()) {
                println(propertyWithRoutes.routes?.joinToString(""))
                println("\nRejected: ${errors.joinToString()}")
                if (!GlobalVariables.safeMode) {
                    propertyRepository.markAsUnsuitable(propertyWithRoutes, unsuitable = true)
                    page.propertiesRemoved++
                }
            } else {
                if (propertyRepository.addOrUpdateProperty(propertyWithRoutes)) {
                    return propertyWithRoutes.id
                }
                println()
                println(
                    propertyRepository.getProperty(propertyWithRoutes.id)
                        ?.prettyPrint()
                        ?: "Oops. Couldn't find property we just saved (${propertyWithRoutes.id})"
                )
            }
        }
        return null
    }
}