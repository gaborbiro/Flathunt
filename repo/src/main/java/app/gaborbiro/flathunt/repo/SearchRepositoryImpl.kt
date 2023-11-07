package app.gaborbiro.flathunt.repo

import app.gaborbiro.flathunt.GlobalVariables
import app.gaborbiro.flathunt.console.ConsoleWriter
import app.gaborbiro.flathunt.criteria.ValidationCriteria
import app.gaborbiro.flathunt.data.domain.Store
import app.gaborbiro.flathunt.data.domain.model.Property
import app.gaborbiro.flathunt.directions.DirectionsService
import app.gaborbiro.flathunt.prettyPrint
import app.gaborbiro.flathunt.repo.domain.DirectionsRepository
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
import java.io.IOException

@Singleton
class SearchRepositoryImpl : SearchRepository, KoinComponent {

    private val store: Store by inject()
    private val webService: WebService by inject()
    private val utilsService: UtilsService by inject()
    private val propertyValidator: PropertyValidator by inject()
    private val propertyRepository: PropertyRepository by inject()
    private val directionsRepository: DirectionsRepository by inject()
    private val console: ConsoleWriter by inject()

    override fun fetchPropertiesFromAllPages(url: String) {
        val storedIds = store.getProperties().map { it.webId }.toSet()
        val addedIds = mutableListOf<String>()
        val failedIds = mutableListOf<String>()
        var currentSearchUrl: String? = url
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
            val basicProperty = webService.fetchProperty(webId)
            console.d(basicProperty.title)
            val property = processProperty(basicProperty)

            if (property != null) {
                addedIds.add(webId)
                console.i(property.prettyPrint())
                console.d()
            } else {
                onMarkedAsUnsuitable()
                failedIds.add(webId)
            }
        } catch (t: Throwable) {
            console.d()
            t.printStackTrace()
            if (t is IOException) {
                throw t
            }
            failedIds.add(webId)
        }
    }

    private fun processProperty(property: Property): Property? {
        val propertyValid = propertyValidator.isValid(property)

        return if (propertyValid) {
            val propertyWithRoutes = directionsRepository.validateDirections(property)

            if (propertyWithRoutes != null) {
                propertyRepository.addOrUpdateProperty(propertyWithRoutes)
            } else {
                if (!property.markedUnsuitable && !GlobalVariables.safeMode) {
                    propertyRepository.markAsUnsuitable(property.webId, unsuitable = true)
                }
            }
            propertyWithRoutes
        } else {
            null
        }
    }
}