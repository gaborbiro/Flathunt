package app.gaborbiro.flathunt.repo

import app.gaborbiro.flathunt.GlobalVariables
import app.gaborbiro.flathunt.console.ConsoleWriter
import app.gaborbiro.flathunt.prettyPrint
import app.gaborbiro.flathunt.repo.domain.DirectionsRepository
import app.gaborbiro.flathunt.repo.domain.FetchPropertyRepository
import app.gaborbiro.flathunt.repo.domain.PropertyRepository
import app.gaborbiro.flathunt.repo.domain.model.FetchPropertyResult
import app.gaborbiro.flathunt.repo.domain.model.SaveType
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
    private val repository: PropertyRepository by inject()
    private val propertyValidator: PropertyValidator by inject()
    private val directionsRepository: DirectionsRepository by inject()
    private val console: ConsoleWriter by inject()

    /**
     * Ad-hoc scan of a property. Marks property as unsuitable if needed.
     */
    override fun fetchProperty(idu: String, save: SaveType, safeMode: Boolean): FetchPropertyResult {
        val cleanUrl = utilsService.parseWebIdOrUrl(idu)

        return if (cleanUrl != null) {
            console.d()
            console.d(cleanUrl)
            val webId = utilsService.getPropertyIdFromUrl(cleanUrl)
            GlobalVariables.lastIdx = webId
            val property = webService.fetchProperty(webId)

            if (property.isBuddyUp && save != SaveType.FORCE_SAVE) {
                console.d("\nBuddy up - skipping...")
                if (property.markedUnsuitable.not() && safeMode.not()) {
                    repository.updateSuitability(webId, suitable = false)
                }
                return FetchPropertyResult.Unsuitable
            }
            if (propertyValidator.isValid(property)) {
                val propertyWithRoutes = directionsRepository.validateDirections(property)
                if (propertyWithRoutes != null) {
                    console.i(propertyWithRoutes.prettyPrint())
                    console.d()
                    when (save) {
                        SaveType.SAVE, SaveType.FORCE_SAVE -> repository.addOrUpdateProperty(propertyWithRoutes)
                        SaveType.CHECK -> {}
                    }
                    FetchPropertyResult.Property(propertyWithRoutes)
                } else {
                    if (save == SaveType.FORCE_SAVE) {
                        repository.addOrUpdateProperty(property)
                        FetchPropertyResult.Property(property)
                    } else if (property.markedUnsuitable.not()) {
                        if (safeMode.not()) {
                            repository.updateSuitability(webId, suitable = false)
                        }
                        FetchPropertyResult.Unsuitable
                    } else {
                        console.d("\nAlready marked unsuitable")
                        FetchPropertyResult.Unsuitable
                    }
                }
            } else {
                if (save == SaveType.FORCE_SAVE) {
                    repository.addOrUpdateProperty(property)
                    FetchPropertyResult.Property(property)
                } else if (property.markedUnsuitable.not()) {
                    if (safeMode.not()) {
                        repository.updateSuitability(webId, suitable = false)
                    }
                    FetchPropertyResult.Unsuitable
                } else {
                    console.d("\nAlready marked unsuitable")
                    FetchPropertyResult.Unsuitable
                }
            }
        } else {
            console.e("Invalid id or url: $idu")
            FetchPropertyResult.Failed
        }
    }
}