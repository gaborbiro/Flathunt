package app.gaborbiro.flathunt.repo

import app.gaborbiro.flathunt.console.ConsoleWriter
import app.gaborbiro.flathunt.criteria.ValidationCriteria
import app.gaborbiro.flathunt.data.domain.model.Property
import app.gaborbiro.flathunt.directions.DirectionsService
import app.gaborbiro.flathunt.directions.model.POIResult
import app.gaborbiro.flathunt.repo.domain.DirectionsRepository
import app.gaborbiro.flathunt.repo.mapper.Mapper
import app.gaborbiro.flathunt.repo.validator.LocationValidator
import org.koin.core.annotation.Singleton
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@Singleton
class DirectionsRepositoryImpl : DirectionsRepository, KoinComponent {

    private val criteria: ValidationCriteria by inject()
    private val locationValidator: LocationValidator by inject()
    private val directionsService: DirectionsService by inject()
    private val console: ConsoleWriter by inject()
    private val mapper: Mapper by inject()

    override fun validateDirections(property: Property): Property? {
        val location = property.location!!
        val poiResults: List<POIResult> = criteria.pointsOfInterest.map { poi ->
            directionsService.directions(
                from = mapper.map(location),
                to = mapper.map(poi, location)
            )
        }

        return if (locationValidator.isValid(poiResults)) {
            val links = mapper.mapLinks(property, poiResults)
            val staticMapUrl = property.location?.let { mapper.mapStaticMap(it, poiResults) }
            val commuteScore = mapper.mapCommuteScore(poiResults)
            val finalProperty = property.copy(
                links = links,
                staticMapUrl = staticMapUrl,
                commuteScore = commuteScore
            )
            console.d("\n${property.webId}: ${property.title}")
            finalProperty
        } else {
            null
        }
    }
}