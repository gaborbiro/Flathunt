package app.gaborbiro.flathunt.useCases

import app.gaborbiro.flathunt.ValidationCriteria
import app.gaborbiro.flathunt.command
import app.gaborbiro.flathunt.data.Store
import app.gaborbiro.flathunt.prettyPrint
import app.gaborbiro.flathunt.service.Service
import app.gaborbiro.flathunt.strict

class ListUseCase(
    private val service: Service,
    private val store: Store,
    criteria: ValidationCriteria
) : BaseUseCase(service, store, criteria) {

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

    override fun getCommands() =
        listOf(listProperties, listIds, listUrl, deleteList, deleteBlacklist, listBlacklist, addBlacklist)
}