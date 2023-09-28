package app.gaborbiro.flathunt.repo

import app.gaborbiro.flathunt.GlobalVariables
import app.gaborbiro.flathunt.ValidationCriteria
import app.gaborbiro.flathunt.console.ConsoleWriter
import app.gaborbiro.flathunt.data.domain.model.Property
import app.gaborbiro.flathunt.directions.DirectionsLatLon
import app.gaborbiro.flathunt.directions.calculateRoutes
import app.gaborbiro.flathunt.prettyPrint
import app.gaborbiro.flathunt.repo.domain.FetchPropertyRepository
import app.gaborbiro.flathunt.repo.domain.PropertyRepository
import app.gaborbiro.flathunt.repo.domain.model.SaveType
import app.gaborbiro.flathunt.request.RequestCaller
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
    private val validator: PropertyValidator by inject()
    private val requestCaller: RequestCaller by inject()
    private val console: ConsoleWriter by inject()

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
            val routes = property.location?.let {
                calculateRoutes(DirectionsLatLon(it.latitude, it.longitude), criteria.pointsOfInterest, requestCaller)
            } ?: emptyList()
            val propertyWithRoutes = property.withRoutes(routes)
            console.i(propertyWithRoutes.prettyPrint())
            if (validator.checkValid(propertyWithRoutes)) {
                when (save) {
                    SaveType.SAVE, SaveType.FORCE_SAVE -> repository.addOrUpdateProperty(propertyWithRoutes)
                    SaveType.CHECK -> {}
                }
            } else if (save == SaveType.FORCE_SAVE) {
                repository.addOrUpdateProperty(propertyWithRoutes)
            } else if (!propertyWithRoutes.markedUnsuitable) {
                if (!safeMode) {
                    webService.markAsUnsuitable(webId, unsuitable = true)
                }
            } else {
                console.d("\nAlready marked unsuitable")
            }
            propertyWithRoutes
        } else {
            console.e("Invalid url: $arg")
            null
        }
    }
}