package app.gaborbiro.flathunt.usecase

import app.gaborbiro.flathunt.GlobalVariables
import app.gaborbiro.flathunt.data.domain.Store
import app.gaborbiro.flathunt.data.domain.model.PersistedProperty
import app.gaborbiro.flathunt.data.domain.model.Property
import app.gaborbiro.flathunt.prettyPrint
import app.gaborbiro.flathunt.service.domain.Service
import app.gaborbiro.flathunt.strict
import app.gaborbiro.flathunt.usecase.base.*
import org.koin.core.component.inject

class ListUseCase : BaseUseCase() {

    private val store: Store by inject<Store>()
    private val service: Service by inject<Service>()
    private val criteria: ValidationCriteria by inject<ValidationCriteria>()

    override val commands: List<Command<*>>
        get() = listOf(
            listProperties,
            listIds,
            listUrl,
            deleteList,
            deleteBlacklist,
            listBlacklist,
            addBlacklist,
            reIndex,
            reCheck,
        )

    private val listProperties = command(
        command = "list properties",
        description = "Prints properties"
    )
    {
        val properties = store.getProperties()
        if (properties.isNotEmpty()) {
            strict { println("${properties.size} properties in database\n") }
            println(properties.joinToString("\n\n\n") { it.prettyPrint() })
        } else {
            strict { println("No saved properties") }
        }
    }

    private val listIds = command(
        command = "list ids",
        description = "Prints property ids"
    )
    {
        val properties = store.getProperties()
        if (properties.isNotEmpty()) {
            strict { println("${properties.size} properties in database\n") }
            println(properties.joinToString("\n") { "${it.index}: ${it.id}" })
        } else {
            strict { println("No saved properties") }
        }
    }

    private val listUrl = command(
        command = "list urls",
        description = "Prints property urls"
    )
    {
        val properties = store.getProperties()
        if (properties.isNotEmpty()) {
            strict { println("${properties.size} properties in database\n") }
            println(properties.joinToString("\n") { "${it.index}\t${service.getUrlFromId(it.id)}\t${it.location}\t\t\t${it.prices[0].pricePerMonthInt}\t\t\t\t${it.title}" })
        } else {
            strict { println("No saved properties") }
        }
    }

    private val deleteList = command(command = "clear properties", description = "Deletes all data") {
        store.clearProperties()
    }

    private val deleteBlacklist =
        command(command = "clear blacklist", description = "Removes properties from blacklist") {
            store.clearBlacklist()
        }

    private val listBlacklist = command(command = "list blacklist", description = "Print blacklisted ids") {
        store.getBlacklist().forEach { println(it) }
    }

    private val addBlacklist = command<String>(
        command = "add blacklist",
        description = "Update blacklist with specified ids",
        argumentName = "ids"
    ) { (ids) ->
        val blacklist = store.getBlacklist()
        store.saveBlacklist(ids.split(",") - blacklist + blacklist)
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

    private val reCheck = command(
        command = "recheck",
        description = "Removes all invalid properties from the database. Useful for re-testing previously added properties after criteria change."
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
}