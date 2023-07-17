package app.gaborbiro.flathunt.usecase

import app.gaborbiro.flathunt.GlobalVariables
import app.gaborbiro.flathunt.data.domain.Store
import app.gaborbiro.flathunt.data.domain.model.PersistedProperty
import app.gaborbiro.flathunt.data.domain.model.Property
import app.gaborbiro.flathunt.service.domain.Service
import app.gaborbiro.flathunt.usecase.base.*

class AddCheckUseCase(
    service: Service,
    store: Store,
    criteria: ValidationCriteria
) : BaseUseCase(service, store, criteria) {

    override val commands: List<Command<*>>
        get() = listOf(
            add,
            addOpen,
            check,
            checkOpen,
            forceAdd,
            reCheck,
            reIndex
        )

    private val add = command<String>(
        command = "add",
        description = "Fetches property (by id or url), validates it and labels it as needed. " +
                "(same as check, but also saves or marks the property)",
        argumentName = "id or url",
    )
    { (arg) ->
        fetchProperty(
            arg = arg.checkLastUsedIndexOrId(),
            save = SaveType.ADD,
            safeMode = GlobalVariables.safeMode
        )
    }

    private val addOpen = command<String>(
        command = "add open",
        description = "Fetches property (by id or url), validates it and labels it as needed. If valid, opens it in a browser. " +
                "(same as check, but also saves or marks the property)",
        argumentName = "id or url",
    )
    { (arg) ->
        service.popTabHandles()
        fetchProperty(
            arg = arg.checkLastUsedIndexOrId(),
            save = SaveType.ADD,
            safeMode = GlobalVariables.safeMode
        )?.let(::openLinks)
    }

    private val check = command<String>(
        command = "check",
        description = "Fetches property (by id or url), validates it and labels it as needed. " +
                "Same as 'add' but doesn't save it in the database.",
        argumentName = "id or url",
    )
    { (arg) ->
        fetchProperty(
            arg = arg.checkLastUsedIndexOrId(),
            save = SaveType.CHECK,
            safeMode = true
        )
    }

    private val checkOpen = command<String>(
        command = "check open",
        description = "Fetches property (by id or url), validates it and labels it as needed. " +
                "Opens it in a browser. Same as 'add open' but doesn't save it in the database.",
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


    private val forceAdd = command<String>(
        command = "force add",
        description = "Fetches property (by id or url) and adds it to the database. Also, it opens it in a browser.",
        argumentName = "id or url",
    )
    { (arg) ->
        fetchProperty(
            arg = arg.checkLastUsedIndexOrId(),
            save = SaveType.FORCE_ADD,
            safeMode = true
        )?.let(::openLinks)
    }

    private val reCheck = command(
        command = "recheck",
        description = "Removes invalid properties from the database. Useful for re-testing previously added properties after criteria change."
    )
    {
        val properties = store.getProperties()
        val validProperties = properties.filter { it.validate(criteria).isEmpty() }
        val toDelete = (properties - validProperties.toSet()).joinToString("\n") { it.index.toString() + " " + it.id }
        println("Deleting properties:")
        println(toDelete)
        if (!GlobalVariables.safeMode) {
            store.saveProperties(validProperties)
        }
    }

    private val reIndex = command(
        command = "reindex",
        description = "Re-indexes all properties in the database starting from 1"
    )
    {
        val properties: List<PersistedProperty> = store.getProperties()
        store.resetIndexes()
        if (!GlobalVariables.safeMode) {
            store.saveProperties(properties.map { Property(it) })
        }
        println("Done")
    }
}