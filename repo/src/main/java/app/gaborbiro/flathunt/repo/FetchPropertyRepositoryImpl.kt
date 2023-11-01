package app.gaborbiro.flathunt.repo

import app.gaborbiro.flathunt.GlobalVariables
import app.gaborbiro.flathunt.console.ConsoleWriter
import app.gaborbiro.flathunt.criteria.POI
import app.gaborbiro.flathunt.criteria.ValidationCriteria
import app.gaborbiro.flathunt.data.domain.model.Property
import app.gaborbiro.flathunt.directions.DirectionsService
import app.gaborbiro.flathunt.directions.model.DirectionsLatLon
import app.gaborbiro.flathunt.directions.model.Route
import app.gaborbiro.flathunt.prettyPrint
import app.gaborbiro.flathunt.repo.domain.FetchPropertyRepository
import app.gaborbiro.flathunt.repo.domain.PropertyRepository
import app.gaborbiro.flathunt.repo.domain.model.SaveType
import app.gaborbiro.flathunt.repo.mapper.Mapper
import app.gaborbiro.flathunt.repo.validator.LocationValidator
import app.gaborbiro.flathunt.repo.validator.PropertyValidator
import app.gaborbiro.flathunt.service.domain.UtilsService
import app.gaborbiro.flathunt.service.domain.WebService
import org.koin.core.annotation.Singleton
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@Singleton
class FetchPropertyRepositoryImpl : FetchPropertyRepository, KoinComponent {

    private val webService: WebService by inject()
    private val utilsService: UtilsService by inject()
    private val criteria: ValidationCriteria by inject()
    private val repository: PropertyRepository by inject()
    private val propertyValidator: PropertyValidator by inject()
    private val locationValidator: LocationValidator by inject()
    private val console: ConsoleWriter by inject()
    private val directionsService: DirectionsService by inject()
    private val mapper: Mapper by inject()

    /**
     * Ad-hoc scan of a property. Marks property as unsuitable if needed.
     */
    override fun fetchProperty(arg: String, save: SaveType, safeMode: Boolean): Property? {
        val cleanUrl = utilsService.parseUrlOrWebId(arg)

        return if (cleanUrl != null) {
            console.d()
            console.d(cleanUrl)
            val webId = utilsService.getPropertyIdFromUrl(cleanUrl)
            GlobalVariables.lastUsedIndexOrWebId = webId
            val property = webService.fetchProperty(webId)

            if (property.isBuddyUp && save != SaveType.FORCE_SAVE) {
                console.d("\nBuddy up - skipping...")
                return null
            }
            console.i(property.prettyPrint())

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

            val links = mapper.mapLinks(property, routesResult)
            val staticMapUrl = property.location?.let { mapper.mapStaticMap(it, routesResult) }
            val commuteScore = mapper.mapCommuteScore(routesResult)
            val finalProperty = property.copy(
                links = links,
                staticMapUrl = staticMapUrl,
                commuteScore = commuteScore
            )

            if (propertyValidator.isValid(finalProperty) && locationValidator.isValid(routesResult)) {
                when (save) {
                    SaveType.SAVE, SaveType.FORCE_SAVE -> repository.addOrUpdateProperty(finalProperty)
                    SaveType.CHECK -> {}
                }
                console.d("Valid")
            } else if (save == SaveType.FORCE_SAVE) {
                repository.addOrUpdateProperty(finalProperty)
            } else if (!finalProperty.markedUnsuitable) {
                if (!safeMode) {
                    webService.markAsUnsuitable(webId, unsuitable = true)
                }
            } else {
                console.d("\nAlready marked unsuitable")
            }
            finalProperty
        } else {
            console.e("Invalid url: $arg")
            null
        }
    }
}