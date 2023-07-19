package app.gaborbiro.flathunt.usecase

import app.gaborbiro.flathunt.GlobalVariables
import app.gaborbiro.flathunt.data.domain.model.PersistedProperty
import app.gaborbiro.flathunt.data.domain.model.Property
import app.gaborbiro.flathunt.google.calculateRoutes
import app.gaborbiro.flathunt.prettyPrint
import app.gaborbiro.flathunt.service.domain.Service
import app.gaborbiro.flathunt.usecase.base.*
import org.koin.core.component.inject

class FetchPropertyUseCase : BaseUseCase() {

    private val service: Service by inject<Service>()
    private val criteria: ValidationCriteria by inject<ValidationCriteria>()

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
                "(same as check, but also saves or marks the property)",
        argumentName = "id or url",
    )
    { (arg) ->
        fetchProperty(
            arg = arg.checkLastUsedIndexOrId(),
            save = SaveType.SAVE,
            safeMode = GlobalVariables.safeMode
        )
    }

    private val fetchAndOpen = command<String>(
        command = "fetch open",
        description = "Fetches property (by id or url), validates it and labels it as needed. If valid, opens it in a browser. " +
                "(same as check, but also saves or marks the property)",
        argumentName = "id or url",
    )
    { (arg) ->
        service.popTabHandles()
        fetchProperty(
            arg = arg.checkLastUsedIndexOrId(),
            save = SaveType.SAVE,
            safeMode = GlobalVariables.safeMode
        )?.let(::openLinks)
    }

    private val peek = command<String>(
        command = "peek",
        description = "Fetches property (by id or url), validates it and even labels it as needed. " +
                "Same as 'fetch' but doesn't save it in the database.",
        argumentName = "id or url",
    )
    { (arg) ->
        fetchProperty(
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
        service.popTabHandles()
        fetchProperty(
            arg = arg.checkLastUsedIndexOrId(),
            save = SaveType.CHECK,
            safeMode = true
        )?.let(::openLinks)
    }


    private val forceFetch = command<String>(
        command = "force fetch",
        description = "Fetches property (by id or url) and adds it to the database (without validation). Also, it opens it in a browser.",
        argumentName = "id or url",
    )
    { (arg) ->
        fetchProperty(
            arg = arg.checkLastUsedIndexOrId(),
            save = SaveType.FORCE_SAVE,
            safeMode = true
        )?.let(::openLinks)
    }

    /**
     * Ad-hoc scan of a property. Marks property as unsuitable if needed.
     */
    private fun fetchProperty(arg: String, save: SaveType, safeMode: Boolean): Property? {
        val cleanUrl = service.checkUrlOrId(arg)
        if (cleanUrl != null) {
            println()
            println(cleanUrl)
            val id = service.getPropertyIdFromUrl(cleanUrl)
            GlobalVariables.lastUsedIndexOrId = id
            val property = service.fetchProperty(id, newTab = true)
            if (property.isBuddyUp && save != SaveType.FORCE_SAVE) {
                println("\nBuddy up - skipping...")
                return null
            }
            val routes = calculateRoutes(property.location, criteria.pointsOfInterest)
            val propertyWithRoutes = property.withRoutes(routes)
            println(propertyWithRoutes.prettyPrint())
            if (propertyWithRoutes.checkValid(criteria)) {
                when (save) {
                    SaveType.FORCE_SAVE -> addOrUpdateProperty(propertyWithRoutes)
                    SaveType.CHECK -> {}
                    SaveType.SAVE -> addOrUpdateProperty(propertyWithRoutes)
                }
            } else if (save == SaveType.FORCE_SAVE) {
                addOrUpdateProperty(propertyWithRoutes)
            } else if (!propertyWithRoutes.markedUnsuitable) {
                if (!safeMode) service.markAsUnsuitable(id, (propertyWithRoutes as? PersistedProperty)?.index, true)
            } else {
                println("\nAlready marked unsuitable")
            }
            return propertyWithRoutes
        }
        return null
    }
}