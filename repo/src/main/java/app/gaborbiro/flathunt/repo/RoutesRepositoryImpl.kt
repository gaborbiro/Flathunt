package app.gaborbiro.flathunt.repo

import app.gaborbiro.flathunt.GlobalVariables
import app.gaborbiro.flathunt.ValidationCriteria
import app.gaborbiro.flathunt.console.ConsoleWriter
import app.gaborbiro.flathunt.data.domain.Store
import app.gaborbiro.flathunt.data.domain.model.PersistedProperty
import app.gaborbiro.flathunt.data.domain.model.Property
import app.gaborbiro.flathunt.google.calculateRoutes
import app.gaborbiro.flathunt.repo.domain.PropertyRepository
import app.gaborbiro.flathunt.repo.domain.RoutesRepository
import app.gaborbiro.flathunt.request.RequestCaller
import app.gaborbiro.flathunt.service.domain.Service
import org.koin.core.annotation.Singleton
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@Singleton
class RoutesRepositoryImpl : RoutesRepository, KoinComponent {

    private val store: Store by inject()
    private val service: Service by inject()
    private val criteria: ValidationCriteria by inject()
    private val validator: PropertyValidator by inject()
    private val propertyRepository: PropertyRepository by inject()
    private val requestCaller: RequestCaller by inject()
    private val console: ConsoleWriter by inject()

    override fun validateByRoutes(): Pair<List<Property>, List<Property>> {
        return validateByRoutes(store.getProperties())
    }

    /**
     * Calculate routes for each property. Marks properties that are too far.
     *
     * @return list of valid and invalid properties (according to route)
     */
    override fun validateByRoutes(properties: List<Property>): Pair<List<Property>, List<Property>> {
        if (properties.isEmpty()) {
            console.d("No saved properties. Fetch some")
            return Pair(emptyList(), emptyList())
        }
        val (toSave, unsuitable) = properties.partition { property ->
            val routes = calculateRoutes(property.location, criteria.pointsOfInterest, requestCaller)
            console.d("\n${property.webId}: ${property.title}:\n${routes.joinToString(", ")}")
            val propertyWithRoutes = property.withRoutes(routes)
            propertyRepository.addOrUpdateProperty(propertyWithRoutes)
            if (validator.checkValid(propertyWithRoutes)) {
                console.d("Valid")
                true
            } else {
                if (!propertyWithRoutes.markedUnsuitable) {
                    if (!GlobalVariables.safeMode) {
                        service.markAsUnsuitable(property.webId, unsuitable = true)
                    }
                }
                false
            }
        }
        return toSave to unsuitable
    }
}