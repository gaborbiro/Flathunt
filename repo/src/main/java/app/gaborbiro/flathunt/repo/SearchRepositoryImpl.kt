package app.gaborbiro.flathunt.repo

import app.gaborbiro.flathunt.GlobalVariables
import app.gaborbiro.flathunt.ValidationCriteria
import app.gaborbiro.flathunt.console.ConsoleWriter
import app.gaborbiro.flathunt.data.domain.Store
import app.gaborbiro.flathunt.data.domain.model.Property
import app.gaborbiro.flathunt.google.GoogleLatLon
import app.gaborbiro.flathunt.google.calculateRoutes
import app.gaborbiro.flathunt.prettyPrint
import app.gaborbiro.flathunt.repo.domain.PropertyRepository
import app.gaborbiro.flathunt.repo.domain.SearchRepository
import app.gaborbiro.flathunt.request.RequestCaller
import app.gaborbiro.flathunt.service.domain.UtilsService
import app.gaborbiro.flathunt.service.domain.WebService
import org.koin.core.annotation.Singleton
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@Singleton
class SearchRepositoryImpl : SearchRepository, KoinComponent {

    private val store: Store by inject()
    private val webService: WebService by inject()
    private val utilsService: UtilsService by inject()
    private val criteria: ValidationCriteria by inject()
    private val validator: PropertyValidator by inject()
    private val propertyRepository: PropertyRepository by inject()
    private val requestCaller: RequestCaller by inject()
    private val console: ConsoleWriter by inject()

    override fun fetchPropertiesFromAllPages(searchUrl: String) {
        val storedIds = store.getProperties().map { it.webId }.toSet()
        val addedIds = mutableListOf<String>()
        val failedIds = mutableListOf<String>()
        var currentSearchUrl: String? = searchUrl
        var markedAsUnsuitableCount = 0
        do {
            val pageInfo = webService.getPageInfo(currentSearchUrl!!)
            console.d("Fetching page ${pageInfo.page}/${pageInfo.pageCount}")
            val blacklistedWebIds = store.getBlacklistWebIds().toSet()
            val newIds: List<String> = pageInfo.propertyWebIds - blacklistedWebIds - storedIds
            newIds.forEachIndexed { i, webId ->
                console.d(
                    "\n=======> Fetching $webId (${i + 1}/${newIds.size}, page ${pageInfo.page}/${pageInfo.pageCount}): ",
                    newLine = false
                )
                fetchAndProcessProperty(
                    webId,
                    addedIds,
                    failedIds,
                    onMarkedAsUnsuitable = { markedAsUnsuitableCount++ })
            }
            currentSearchUrl = utilsService.getNextPageUrl(pageInfo, markedAsUnsuitableCount)
        } while (currentSearchUrl != null)

        console.d("\nFinished")
        if (addedIds.isNotEmpty()) {
            console.i("New ids: ${addedIds.joinToString(",")}")
        }
        if (failedIds.isNotEmpty()) {
            console.i("Failed ids: ${failedIds.joinToString(",")}")
        }
    }

    private fun fetchAndProcessProperty(
        webId: String,
        addedIds: MutableList<String>,
        failedIds: MutableList<String>,
        onMarkedAsUnsuitable: () -> Unit
    ) {
        try {
            val rawProperty = webService.fetchProperty(webId)
            console.d(rawProperty.title)
            val propertyWithRoutes = processProperty(rawProperty)
            propertyWithRoutes
                ?.let {
                    addedIds.add(webId)
                    console.i(propertyWithRoutes.prettyPrint())
                }
                ?: run {
                    if (!rawProperty.markedUnsuitable && !GlobalVariables.safeMode) {
                        propertyRepository.markAsUnsuitable(webId, unsuitable = true)
                    }
                    onMarkedAsUnsuitable()
                    failedIds.add(webId)
                }
        } catch (t: Throwable) {
            console.d()
            t.printStackTrace()
            failedIds.add(webId)
        }
    }

    /**
     * @return property with fresh routes if property is valid, null otherwise
     */
    private fun processProperty(property: Property): Property? {
        // pre-validate to save on the Google Maps API call
        val rawPropertyValid = validator.checkValid(property)
        return if (rawPropertyValid) {
            val routes = property.location?.let {
                calculateRoutes(GoogleLatLon(it.latitude, it.longitude), criteria.pointsOfInterest, requestCaller)
            } ?: emptyList()
            val propertyWithRoutes = property.withRoutes(routes)
            val propertyWithRoutesValid = validator.checkValid(propertyWithRoutes)
            if (propertyWithRoutesValid) {
                console.d(routes.joinToString(""))
                propertyRepository.addOrUpdateProperty(propertyWithRoutes)
                propertyWithRoutes
            } else {
                null
            }
        } else {
            null
        }
    }
}