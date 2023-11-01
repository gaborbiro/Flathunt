package app.gaborbiro.flathunt.repo

import app.gaborbiro.flathunt.GlobalVariables
import app.gaborbiro.flathunt.console.ConsoleWriter
import app.gaborbiro.flathunt.criteria.POI
import app.gaborbiro.flathunt.criteria.ValidationCriteria
import app.gaborbiro.flathunt.data.domain.Store
import app.gaborbiro.flathunt.data.domain.model.Property
import app.gaborbiro.flathunt.directions.DirectionsService
import app.gaborbiro.flathunt.directions.model.DirectionsLatLon
import app.gaborbiro.flathunt.directions.model.Route
import app.gaborbiro.flathunt.prettyPrint
import app.gaborbiro.flathunt.repo.domain.PropertyRepository
import app.gaborbiro.flathunt.repo.domain.SearchRepository
import app.gaborbiro.flathunt.repo.mapper.Mapper
import app.gaborbiro.flathunt.repo.validator.LocationValidator
import app.gaborbiro.flathunt.repo.validator.PropertyValidator
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
    private val propertyValidator: PropertyValidator by inject()
    private val locationValidator: LocationValidator by inject()
    private val propertyRepository: PropertyRepository by inject()
    private val console: ConsoleWriter by inject()
    private val directionsService: DirectionsService by inject()
    private val mapper: Mapper by inject()

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
            val property = webService.fetchProperty(webId)
            console.d(property.title)
            val valid = validateProperty(property)
            if (valid) {
                addedIds.add(webId)
                console.i(property.prettyPrint())
            } else {
                if (!property.markedUnsuitable && !GlobalVariables.safeMode) {
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

    private fun validateProperty(property: Property): Boolean {
        // pre-validate to save on the Google Maps API call
        val propertyValid = propertyValidator.isValid(property)

        return if (propertyValid) {
            val routesResult: Map<POI, Route?> = criteria.pointsOfInterest.associateWith { poi ->
                property.location?.let {
                    directionsService.route(
                        from = DirectionsLatLon(it.latitude, it.longitude),
                        to = mapper.map(poi)
                    )
                }
            }
            val routesStr = routesResult.toList().joinToString { (poi, route) ->
                "\n${poi.description}: ${route?.toString()}"
            }
            console.d("\n${property.webId}: ${property.title}:\n$routesStr")

            if (locationValidator.isValid(routesResult)) {
                val links = mapper.mapLinks(property, routesResult)
                val staticMapUrl = property.location?.let { mapper.mapStaticMap(it, routesResult) }
                val commuteScore = mapper.mapCommuteScore(routesResult)
                val finalProperty = property.copy(
                    links = links,
                    staticMapUrl = staticMapUrl,
                    commuteScore = commuteScore
                )
                propertyRepository.addOrUpdateProperty(finalProperty)
                console.d("Valid")
                true
            } else {
                false
            }
        } else {
            false
        }
    }
}