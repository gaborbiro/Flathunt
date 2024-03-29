package app.gaborbiro.flathunt.usecase

import app.gaborbiro.flathunt.console.ConsoleWriter
import app.gaborbiro.flathunt.nostrict
import app.gaborbiro.flathunt.repo.domain.PropertyRepository
import app.gaborbiro.flathunt.usecase.base.BaseUseCase
import app.gaborbiro.flathunt.usecase.base.Command
import app.gaborbiro.flathunt.usecase.base.command
import org.koin.core.component.inject
import org.koin.core.qualifier.StringQualifier

class ListUseCase : BaseUseCase() {

    private val propertyRepository: PropertyRepository by inject()
    private val console: ConsoleWriter by inject()
    private val serviceName: String by inject(StringQualifier("serviceName"))

    override val commands: List<Command<*>>
        get() = listOf(
            listIds,
            listTable,
            listBlacklist,
            clearProperties,
            clearBlacklist,
            clearAll,
            reindex,
        )

    private val listIds = command(
        command = "list ids",
        description = "Print all $serviceName property ids"
    )
    {
        val properties = propertyRepository.getProperties()
        if (properties.isNotEmpty()) {
            nostrict { console.d("${properties.size} properties in database\n") }
            console.d(properties.joinToString("\n") { "${it.index}: ${it.webId}" })
        } else {
            nostrict { console.d("No saved properties") }
        }
    }

    private val listTable = command(
        command = "list table",
        description = "Print all $serviceName properties as table. Fields are: url, location, price, title"
    )
    {
        val properties = propertyRepository.getProperties()
        if (properties.isNotEmpty()) {
            nostrict { console.d("${properties.size} properties in database\n") }
            console.d(properties.joinToString("\n") {
                "${it.index}" +
                        "\t${propertyRepository.getPropertyUrl(it.webId)}" +
                        "\t${it.location}" +
                        "\t\t\t${it.prices[0].pricePerMonthInt}" +
                        "\t\t\t\t${it.title}"
            })
        } else {
            nostrict { console.d("No saved properties") }
        }
    }

    private val listBlacklist = command(
        command = "list blacklist",
        description = "Print all blacklisted $serviceName ids"
    ) {
        propertyRepository.getBlacklist().forEach { console.d(it) }
    }

    private val clearProperties = command(
        command = "clear properties",
        description = "Delete all properties from the $serviceName database"
    ) {
        propertyRepository.clearProperties()
    }

    private val clearBlacklist = command(
        command = "clear blacklist",
        description = "Remove all properties from the $serviceName blacklist"
    ) {
        propertyRepository.clearBlacklist()
    }

    private val clearAll = command(
        command = "clear all",
        description = "Remove all properties from the $serviceName database and blacklist"
    ) {
        var count = propertyRepository.clearProperties()
        console.d("$count properties deleted")
        count = propertyRepository.clearBlacklist()
        console.d("$count blacklisted ids deleted")
    }

    private val reindex = command(
        command = "reindex",
        description = "Re-indexes all properties in the $serviceName database starting from 1"
    )
    {
        propertyRepository.reindex()
        console.d("Done")
    }
}