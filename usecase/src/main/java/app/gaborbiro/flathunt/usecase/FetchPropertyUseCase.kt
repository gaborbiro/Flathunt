package app.gaborbiro.flathunt.usecase

import app.gaborbiro.flathunt.GlobalVariables
import app.gaborbiro.flathunt.repo.domain.FetchPropertyRepository
import app.gaborbiro.flathunt.repo.domain.PropertyRepository
import app.gaborbiro.flathunt.repo.domain.model.FetchPropertyResult
import app.gaborbiro.flathunt.repo.domain.model.SaveType
import app.gaborbiro.flathunt.usecase.base.BaseUseCase
import app.gaborbiro.flathunt.usecase.base.Command
import app.gaborbiro.flathunt.usecase.base.command
import org.koin.core.component.inject

class FetchPropertyUseCase : BaseUseCase() {

    private val fetchPropertyRepository: FetchPropertyRepository by inject()
    private val propertyRepository: PropertyRepository by inject()

    override val commands: List<Command<*>>
        get() = listOf(
            fetch,
            forceFetch,
            peek,
        )

    private val fetch = command<String>(
        command = "fetch",
        description = "Fetch property, validate it and label it as needed. If suitable, open it in a browser",
        argumentName1 = "idu",
    )
    { (idu) ->
        val result = fetchPropertyRepository.fetchProperty(
            idu = idu.checkLastUsedIdx(),
            save = SaveType.SAVE,
            safeMode = GlobalVariables.safeMode
        )
        if (result is FetchPropertyResult.Property) {
            propertyRepository.openLinks(result.property)
        }
    }

    private val peek = command<String>(
        command = "peek",
        description = "Fetch property, validate it and label it as needed. If suitable, open it in a browser.",
        argumentName1 = "idu",
    )
    { (idu) ->
        val result = fetchPropertyRepository.fetchProperty(
            idu = idu.checkLastUsedIdx(),
            save = SaveType.CHECK,
            safeMode = true
        )
        if (result is FetchPropertyResult.Property) {
            propertyRepository.openLinks(result.property)
        }
    }


    private val forceFetch = command<String>(
        command = "force fetch",
        description = "Fetch property and add it to the database (regardless of suitability). If suitable open it in browser.",
        argumentName1 = "idu",
    )
    { (idu) ->
        val result = fetchPropertyRepository.fetchProperty(
            idu = idu.checkLastUsedIdx(),
            save = SaveType.FORCE_SAVE,
            safeMode = true
        )
        if (result is FetchPropertyResult.Property) {
            propertyRepository.openLinks(result.property)
        }
    }
}