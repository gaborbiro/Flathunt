package app.gaborbiro.flathunt.usecase

import app.gaborbiro.flathunt.GlobalVariables
import app.gaborbiro.flathunt.repo.domain.FetchPropertyRepository
import app.gaborbiro.flathunt.repo.domain.PropertyRepository
import app.gaborbiro.flathunt.repo.domain.model.SaveType
import app.gaborbiro.flathunt.usecase.base.*
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
        description = "Fetches property (by id or url), validates it and labels it as needed. " +
                "(same as peek, but also saves or marks the property)",
        argumentName = "id or url",
    )
    { (arg) ->
        fetchPropertyRepository.fetchProperty(
            arg = arg.checkLastUsedIndexOrId(),
            save = SaveType.SAVE,
            safeMode = GlobalVariables.safeMode
        )
    }

    private val fetchAndOpen = command<String>(
        command = "fetch open",
        description = "Fetches property (by id or url), validates it and labels it as needed. If valid, opens it in a browser. " +
                "(same as peek, but also saves or marks the property)",
        argumentName = "id or url",
    )
    { (arg) ->
        fetchPropertyRepository.fetchProperty(
            arg = arg.checkLastUsedIndexOrId(),
            save = SaveType.SAVE,
            safeMode = GlobalVariables.safeMode
        )?.let(propertyRepository::openLinks)
    }

    private val peek = command<String>(
        command = "peek",
        description = "Fetches property (by id or url), validates it and even labels it as needed. " +
                "Same as 'fetch' but doesn't save it in the database.",
        argumentName = "id or url",
    )
    { (arg) ->
        fetchPropertyRepository.fetchProperty(
            arg = arg.checkLastUsedIndexOrId(),
            save = SaveType.CHECK,
            safeMode = true
        )
    }

    private val peekOpen = command<String>(
        command = "peek open",
        description = "Fetches property (by id or url), validates it and even labels it as needed. " +
                "Opens it in a browser. Same as 'fetch open' but doesn't save it in the database.",
        argumentName = "id or url",
    )
    { (arg) ->
        fetchPropertyRepository.fetchProperty(
            arg = arg.checkLastUsedIndexOrId(),
            save = SaveType.CHECK,
            safeMode = true
        )?.let(propertyRepository::openLinks)
    }


    private val forceFetch = command<String>(
        command = "force fetch",
        description = "Fetches property (by id or url) and adds it to the database (without validation). Also, it opens it in a browser.",
        argumentName = "id or url",
    )
    { (arg) ->
        fetchPropertyRepository.fetchProperty(
            arg = arg.checkLastUsedIndexOrId(),
            save = SaveType.FORCE_SAVE,
            safeMode = true
        )?.let(propertyRepository::openLinks)
    }
}