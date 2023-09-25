package app.gaborbiro.flathunt.repo

import app.gaborbiro.flathunt.GlobalVariables
import app.gaborbiro.flathunt.ValidationCriteria
import app.gaborbiro.flathunt.console.ConsoleWriter
import app.gaborbiro.flathunt.data.domain.model.PersistedProperty
import app.gaborbiro.flathunt.data.domain.model.Property
import app.gaborbiro.flathunt.google.calculateRoutes
import app.gaborbiro.flathunt.prettyPrint
import app.gaborbiro.flathunt.repo.domain.FetchPropertyRepository
import app.gaborbiro.flathunt.repo.domain.PropertyRepository
import app.gaborbiro.flathunt.repo.domain.model.SaveType
import app.gaborbiro.flathunt.request.RequestCaller
import app.gaborbiro.flathunt.service.domain.Service
import org.koin.core.annotation.Singleton
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@Singleton
class FetchPropertyRepositoryImpl : FetchPropertyRepository, KoinComponent {

    private val service: Service by inject()
    private val criteria: ValidationCriteria by inject()
    private val repository: PropertyRepository by inject()
    private val validator: PropertyValidator by inject()
    private val requestCaller: RequestCaller by inject()
    private val console: ConsoleWriter by inject()

    /**
     * Ad-hoc scan of a property. Marks property as unsuitable if needed.
     */
    override fun fetchProperty(arg: String, save: SaveType, safeMode: Boolean): Property? {
        val cleanUrl = service.checkUrlOrId(arg)
        if (cleanUrl != null) {
            console.d()
            console.d(cleanUrl)
            val id = service.getPropertyIdFromUrl(cleanUrl)
            GlobalVariables.lastUsedIndexOrId = id
            val property = service.fetchProperty(id, newTab = true)
            if (property.isBuddyUp && save != SaveType.FORCE_SAVE) {
                console.d("\nBuddy up - skipping...")
                return null
            }
            val routes = calculateRoutes(property.location, criteria.pointsOfInterest, requestCaller)
            val propertyWithRoutes = property.withRoutes(routes)
            console.d(propertyWithRoutes.prettyPrint())
            if (validator.checkValid(propertyWithRoutes)) {
                when (save) {
                    SaveType.FORCE_SAVE -> repository.addOrUpdateProperty(propertyWithRoutes)
                    SaveType.CHECK -> {}
                    SaveType.SAVE -> repository.addOrUpdateProperty(propertyWithRoutes)
                }
            } else if (save == SaveType.FORCE_SAVE) {
                repository.addOrUpdateProperty(propertyWithRoutes)
            } else if (!propertyWithRoutes.markedUnsuitable) {
                if (!safeMode) {
                    val index = (propertyWithRoutes as? PersistedProperty)?.index
                    val description = index?.let { "($it)" } ?: ""
                    service.markAsUnsuitable(id, unsuitable = true, description)
                }
            } else {
                console.d("\nAlready marked unsuitable")
            }
            return propertyWithRoutes
        }
        return null
    }
}