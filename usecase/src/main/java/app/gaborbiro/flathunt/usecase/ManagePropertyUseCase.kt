package app.gaborbiro.flathunt.usecase

import app.gaborbiro.flathunt.GlobalVariables
import app.gaborbiro.flathunt.data.domain.model.PersistedProperty
import app.gaborbiro.flathunt.google.getRoutesToNearestStations
import app.gaborbiro.flathunt.orNull
import app.gaborbiro.flathunt.prettyPrint
import app.gaborbiro.flathunt.repo.domain.PropertyRepository
import app.gaborbiro.flathunt.usecase.base.BaseUseCase
import app.gaborbiro.flathunt.usecase.base.Command
import app.gaborbiro.flathunt.usecase.base.command
import org.koin.core.component.inject

class ManagePropertyUseCase : BaseUseCase() {

    private val propertyRepository: PropertyRepository by inject()

    override val commands: List<Command<*>>
        get() = listOf(
            print,
            open,
            tyn,
            delete,
            mark,
            comment,
            commentAppend,
            stations
        )

    private val print = command<String>(
        command = "print",
        description = "Print a saved property by index or id",
        argumentName = "index or id",
    )
    { (indexOrId) ->
        val indexOrId = indexOrId.checkLastUsedIndexOrId()
        val property = propertyRepository.getProperty(indexOrId)
        GlobalVariables.lastUsedIndexOrId = property?.let { indexOrId }
        property
            ?.prettyPrint()?.let(::println)
            ?: run { println("Cannot find property with index or id $indexOrId") }
    }

    private val open = command<String>(
        command = "open",
        description = "Opens property in browser (by index or id)",
        argumentName = "index(es) or id(s)",
    )
    { (arg) ->
        getPropertiesByIndexOrIdArray(arg).forEach(propertyRepository::openLinks)
    }

    private val tyn = command(
        command = "tyn",
        description = "(Thank You Next) Deletes last viewed property, marks it as unsuitable/hidden and opens next index in browser",
    )
    {
        val next = GlobalVariables.lastUsedIndexOrId?.let { propertyRepository.getNextProperty(it) }
        getPropertiesByIndexOrIdArray("$").forEach {
            propertyRepository.deleteProperty(it.index, markAsUnsuitable = true, GlobalVariables.safeMode)
        }
        next?.let {
            println("${it.index} - ${it.id}")
            GlobalVariables.lastUsedIndexOrId = it.id
            propertyRepository.openLinks(it)
        } ?: run { println("Nothing to open") }
    }

    private val delete = command<String, Boolean>(
        command = "delete",
        description = "Deletes one or more properties from the database, by indexes or ids (comma separated). " +
                "Pass '-u' after the index to also mark the property as unsuitable.",
        argumentName1 = "indexes or ids (comma separated)",
        argumentName2 = "mark as unsuitable (true/false)",
    )
    { (arg, mark) ->
        getPropertiesByIndexOrIdArray(arg).forEach {
            propertyRepository.deleteProperty(it.index, markAsUnsuitable = mark, GlobalVariables.safeMode)
        }
    }

    private val mark = command<String, Boolean>(
        command = "mark",
        description = "Marks one or more properties as unsuitable/hidden or removes such mark, by indexes or ids (comma separated)",
        argumentName1 = "indexes or ids (comma separated)",
        argumentName2 = "unsuitable"
    )
    { (arg, unsuitable) ->
        getPropertiesByIndexOrIdArray(arg).forEach { propertyRepository.markAsUnsuitable(it, unsuitable) }
    }

    private val comment = command<String, String>(
        command = "comment",
        description = "Set the comment field of a property, by index or id",
        argumentName1 = "index or id",
        argumentName2 = "comment",
    )
    { (indexOrId, comment) ->
        val indexOrId = indexOrId.checkLastUsedIndexOrId()
        val property = propertyRepository.getProperty(indexOrId)
        property
            ?.let {
                GlobalVariables.lastUsedIndexOrId = indexOrId
                propertyRepository.addOrUpdateProperty(it.clone(comment = comment))
            }
            ?: run { println("Cannot find property with index or id $indexOrId") }
    }

    private val commentAppend = command<String, String>(
        command = "comment+",
        description = "Append comment to a property, by index or id",
        argumentName1 = "index or id",
        argumentName2 = "comment",
    )
    { (indexOrId, comment) ->
        val indexOrId = indexOrId.checkLastUsedIndexOrId()
        val property = propertyRepository.getProperty(indexOrId)
        property
            ?.let {
                GlobalVariables.lastUsedIndexOrId = indexOrId
                if (comment.isNotBlank()) {
                    propertyRepository.addOrUpdateProperty(it.clone(comment = it.comment + " " + comment))
                }
            }
            ?: run { println("Cannot find property with index or id $indexOrId") }
    }

    private val stations = command<String>(
        command = "stations",
        description = "Find nearest stations, by index or id",
        argumentName = "index or id",
    )
    { (indexOrId) ->
        val indexOrId = indexOrId.checkLastUsedIndexOrId()
        val property = propertyRepository.getProperty(indexOrId)
        property
            ?.let {
                GlobalVariables.lastUsedIndexOrId = indexOrId
                it.location
                    ?.let {
                        println(getRoutesToNearestStations(it).orNull()?.joinToString("") ?: "No nearby stations found")
                    } ?: run {
                    println("Property $indexOrId does not have a location")
                }
            }
            ?: run { println("Cannot find property with index or id $indexOrId") }
    }

    private fun getPropertiesByIndexOrIdArray(arg: String): List<PersistedProperty> {
        val arg = arg.checkLastUsedIndexOrId()
        var properties = propertyRepository.getProperty(arg)?.let { listOf(it) }

        if (properties == null) {
            val tokens = arg.split(Regex("[,\\s]+"))
            properties = tokens.map { getPropertiesByIndexOrIdArray(it) }.flatten()
            val notFound = tokens - properties.map { it.index.toString() } - properties.map { it.id }
            if (notFound.isNotEmpty()) {
                println("The following properties were not found in database: ${notFound.joinToString(", ")}")
            }
        }
        if (properties.size == 1) {
            GlobalVariables.lastUsedIndexOrId = arg
        }
        return properties
    }
}