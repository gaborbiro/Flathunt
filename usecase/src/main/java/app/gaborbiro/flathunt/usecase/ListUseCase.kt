package app.gaborbiro.flathunt.usecase

import app.gaborbiro.flathunt.ValidationCriteria
import app.gaborbiro.flathunt.prettyPrint
import app.gaborbiro.flathunt.repo.domain.PropertyRepository
import app.gaborbiro.flathunt.strict
import app.gaborbiro.flathunt.usecase.base.BaseUseCase
import app.gaborbiro.flathunt.usecase.base.Command
import app.gaborbiro.flathunt.usecase.base.command
import org.koin.core.component.inject

class ListUseCase : BaseUseCase() {

    private val criteria: ValidationCriteria by inject()
    private val propertyRepository: PropertyRepository by inject()

    override val commands: List<Command<*>>
        get() = listOf(
            listProperties,
            listIds,
            listUrl,
            deleteList,
            deleteBlacklist,
            listBlacklist,
            addBlacklist,
            reindex,
            verifyAll,
        )

    private val listProperties = command(
        command = "list properties",
        description = "Prints properties"
    )
    {
        val properties = propertyRepository.getProperties()
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
        val properties = propertyRepository.getProperties()
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
        val properties = propertyRepository.getProperties()
        if (properties.isNotEmpty()) {
            strict { println("${properties.size} properties in database\n") }
            println(properties.joinToString("\n") {
                "${it.index}" +
                        "\t${propertyRepository.getPropertyUrl(it.id)}" +
                        "\t${it.location}" +
                        "\t\t\t${it.prices[0].pricePerMonthInt}" +
                        "\t\t\t\t${it.title}"
            })
        } else {
            strict { println("No saved properties") }
        }
    }

    private val deleteList = command(
        command = "clear properties",
        description = "Deletes all data"
    ) {
        propertyRepository.clearProperties()
    }

    private val deleteBlacklist = command(
        command = "clear blacklist",
        description = "Removes properties from blacklist"
    ) {
        propertyRepository.clearBlacklist()
    }

    private val listBlacklist = command(
        command = "list blacklist",
        description = "Print blacklisted ids"
    ) {
        propertyRepository.getBlacklist().forEach { println(it) }
    }

    private val addBlacklist = command<String>(
        command = "add blacklist",
        description = "Update blacklist with specified ids",
        argumentName = "ids"
    ) { (ids) ->
        propertyRepository.addToBlacklist(ids.split(Regex("[,\\s]+")))
    }

    private val reindex = command(
        command = "reindex",
        description = "Re-indexes all properties in the database starting from 1"
    )
    {
        propertyRepository.reindex()
        println("Done")
    }

    private val verifyAll = command(
        command = "verify",
        description = "Removes all invalid properties from the database. Useful for re-testing previously added properties after criteria change."
    )
    {
        propertyRepository.verifyAll()
    }
}