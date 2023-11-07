package app.gaborbiro.flathunt.usecase

import app.gaborbiro.flathunt.GlobalVariables
import app.gaborbiro.flathunt.repo.domain.FetchPropertyRepository
import app.gaborbiro.flathunt.repo.domain.PropertyRepository
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
            fetchAndOpen,
            peek,
            peekOpen,
            forceFetch,
        )

    private val fetch = command<String>(
        command = "fetch",
        description = "Fetch property, validate it and label it as needed. " +
                "Same as peek, but also saves or marks the property as unsuitable.",
        argumentDescription = "idu",
    )
    { (idu) ->
        fetchPropertyRepository.fetchProperty(
            idu = idu.checkLastUsedIdx(),
            save = SaveType.SAVE,
            safeMode = GlobalVariables.safeMode
        )
    }

    private val fetchAndOpen = command<String>(
        command = "fetch open",
        description = "Fetch property, validate it and label it as needed. If valid, open it in a browser. " +
                "Same as peek, but also saves or marks the property as unsuitable.",
        argumentDescription = "idu",
    )
    { (idu) ->
        fetchPropertyRepository.fetchProperty(
            idu = idu.checkLastUsedIdx(),
            save = SaveType.SAVE,
            safeMode = GlobalVariables.safeMode
        )?.let(propertyRepository::openLinks)
    }

    private val peek = command<String>(
        command = "peek",
        description = "Fetch property, validate it and even label it as needed. " +
                "Same as 'fetch' but doesn't save it in the database.",
        argumentDescription = "idu",
    )
    { (idu) ->
        fetchPropertyRepository.fetchProperty(
            idu = idu.checkLastUsedIdx(),
            save = SaveType.CHECK,
            safeMode = true
        )
    }

    private val peekOpen = command<String>(
        command = "peek open",
        description = "Fetch property, validate it and label it as needed. " +
                "Opens it in a browser. Same as 'fetch open' but doesn't save it in the database.",
        argumentDescription = "idu",
    )
    { (idu) ->
        fetchPropertyRepository.fetchProperty(
            idu = idu.checkLastUsedIdx(),
            save = SaveType.CHECK,
            safeMode = true
        )?.let(propertyRepository::openLinks)
    }


    private val forceFetch = command<String>(
        command = "force fetch",
        description = "Fetch property and add it to the database (without validation). Also, it open it in browser.",
        argumentDescription = "idu",
    )
    { (idu) ->
        fetchPropertyRepository.fetchProperty(
            idu = idu.checkLastUsedIdx(),
            save = SaveType.FORCE_SAVE,
            safeMode = true
        )?.let(propertyRepository::openLinks)
    }
}