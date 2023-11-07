package app.gaborbiro.flathunt.repo

import app.gaborbiro.flathunt.GlobalVariables
import app.gaborbiro.flathunt.criteria.ValidationCriteria
import app.gaborbiro.flathunt.console.ConsoleWriter
import app.gaborbiro.flathunt.criteria.POI
import app.gaborbiro.flathunt.data.domain.Store
import app.gaborbiro.flathunt.data.domain.model.Property
import app.gaborbiro.flathunt.directions.model.DirectionsLatLon
import app.gaborbiro.flathunt.directions.DirectionsService
import app.gaborbiro.flathunt.directions.model.Route
import app.gaborbiro.flathunt.repo.domain.PropertyRepository
import app.gaborbiro.flathunt.repo.domain.DirectionsRepository
import app.gaborbiro.flathunt.repo.mapper.Mapper
import app.gaborbiro.flathunt.repo.validator.LocationValidator
import app.gaborbiro.flathunt.repo.validator.PropertyValidator
import app.gaborbiro.flathunt.service.domain.WebService
import org.koin.core.annotation.Singleton
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@Singleton
class DirectionsRepositoryImpl : DirectionsRepository, KoinComponent {

    private val store: Store by inject()
    private val webService: WebService by inject()
    private val criteria: ValidationCriteria by inject()
    private val propertyValidator: PropertyValidator by inject()
    private val locationValidator: LocationValidator by inject()
    private val propertyRepository: PropertyRepository by inject()
    private val directionsService: DirectionsService by inject()
    private val console: ConsoleWriter by inject()
    private val mapper: Mapper by inject()

    override fun validateDirections(): Pair<List<Property>, List<Property>> {
        return validateDirections(store.getProperties())
    }

    /**
     * Calculate routes for each property. Marks properties that are too far.
     *
     * @return list of valid and invalid properties (according to route)
     */
    override fun validateDirections(properties: List<Property>): Pair<List<Property>, List<Property>> {
        if (properties.isEmpty()) {
            console.d("No saved properties. Fetch some")
            return Pair(emptyList(), emptyList())
        }
        val (suitable, unsuitable) = properties.partition { property ->
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

            if (propertyValidator.isValid(property) && locationValidator.isValid(routesResult)) {
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
                if (!property.markedUnsuitable && !GlobalVariables.safeMode) {
                    webService.markAsUnsuitable(property.webId, unsuitable = true)
                }
                false
            }
        }
        return suitable to unsuitable
    }
}