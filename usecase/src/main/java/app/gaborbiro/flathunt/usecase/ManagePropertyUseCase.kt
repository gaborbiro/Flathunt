package app.gaborbiro.flathunt.usecase

import app.gaborbiro.flathunt.GlobalVariables
import app.gaborbiro.flathunt.console.ConsoleWriter
import app.gaborbiro.flathunt.data.domain.model.PersistedProperty
import app.gaborbiro.flathunt.directions.DirectionsLatLon
import app.gaborbiro.flathunt.directions.getRoutesToNearestStations
import app.gaborbiro.flathunt.orNull
import app.gaborbiro.flathunt.prettyPrint
import app.gaborbiro.flathunt.repo.domain.PropertyRepository
import app.gaborbiro.flathunt.request.RequestCaller
import app.gaborbiro.flathunt.usecase.base.BaseUseCase
import app.gaborbiro.flathunt.usecase.base.Command
import app.gaborbiro.flathunt.usecase.base.command
import org.koin.core.component.inject

class ManagePropertyUseCase : BaseUseCase() {

    private val propertyRepository: PropertyRepository by inject()
    private val requestCaller: RequestCaller by inject()
    private val console: ConsoleWriter by inject()

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
        description = "Print a saved property by index or web id",
        argumentName = "index or web id",
    )
    { (indexOrWebId) ->
        val indexOrWebId = indexOrWebId.checkLastUsedIndexOrWebId()
        val property = propertyRepository.getProperty(indexOrWebId)
        GlobalVariables.lastUsedIndexOrWebId = property?.let { indexOrWebId }
        property
            ?.prettyPrint()?.let(::println)
            ?: run { console.d("Cannot find property with index or web id $indexOrWebId") }
    }

    private val open = command<String>(
        command = "open",
        description = "Opens property in browser (by index or web id)",
        argumentName = "index(es) or web id(s)",
    )
    { (arg) ->
        getPropertiesByIndexOrWebIdArray(arg).forEach(propertyRepository::openLinks)
    }

    private val tyn = command(
        command = "tyn",
        description = "(Thank You Next) Deletes last viewed property, marks it as unsuitable/hidden and opens next index in browser",
    )
    {
        val next = GlobalVariables.lastUsedIndexOrWebId?.let { propertyRepository.getNextProperty(it) }
        getPropertiesByIndexOrWebIdArray("$").forEach {
            propertyRepository.deleteProperty(it.index, markAsUnsuitable = true, GlobalVariables.safeMode)
        }
        next?.let {
            console.d("${it.index} - ${it.webId}")
            GlobalVariables.lastUsedIndexOrWebId = it.webId
            propertyRepository.openLinks(it)
        } ?: run { console.d("Nothing to open") }
    }

    private val delete = command<String, Boolean>(
        command = "delete",
        description = "Deletes one or more properties from the database, by indexes or web ids (comma separated). " +
                "Pass '-u' after the index to also mark the property as unsuitable.",
        argumentName1 = "indexes or web ids (comma separated)",
        argumentName2 = "mark as unsuitable (true/false)",
    )
    { (arg, mark) ->
        getPropertiesByIndexOrWebIdArray(arg).forEach {
            propertyRepository.deleteProperty(it.index, markAsUnsuitable = mark, GlobalVariables.safeMode)
        }
    }

    private val mark = command<String, Boolean>(
        command = "mark",
        description = "Marks one or more properties as unsuitable/hidden or removes such mark, by indexes or web ids (comma separated)",
        argumentName1 = "indexes or web ids (comma separated)",
        argumentName2 = "unsuitable"
    )
    { (arg, unsuitable) ->
        getPropertiesByIndexOrWebIdArray(arg).forEach { propertyRepository.markAsUnsuitable(it.webId, unsuitable) }
    }

    private val comment = command<String, String>(
        command = "comment",
        description = "Set the comment field of a property, by index or web id",
        argumentName1 = "index or web id",
        argumentName2 = "comment",
    )
    { (indexOrWebId, comment) ->
        val indexOrWebId = indexOrWebId.checkLastUsedIndexOrWebId()
        val property = propertyRepository.getProperty(indexOrWebId)
        property
            ?.let {
                GlobalVariables.lastUsedIndexOrWebId = indexOrWebId
                propertyRepository.addOrUpdateProperty(it.clone(comment = comment))
            }
            ?: run { console.d("Cannot find property with index or web id $indexOrWebId") }
    }

    private val commentAppend = command<String, String>(
        command = "comment+",
        description = "Append comment to a property, by index or web id",
        argumentName1 = "index or web id",
        argumentName2 = "comment",
    )
    { (indexOrWebId, comment) ->
        val indexOrWebId = indexOrWebId.checkLastUsedIndexOrWebId()
        val property = propertyRepository.getProperty(indexOrWebId)
        property
            ?.let {
                GlobalVariables.lastUsedIndexOrWebId = indexOrWebId
                if (comment.isNotBlank()) {
                    propertyRepository.addOrUpdateProperty(it.clone(comment = it.comment + " " + comment))
                }
            }
            ?: run { console.d("Cannot find property with index or web id $indexOrWebId") }
    }

    private val stations = command<String>(
        command = "stations",
        description = "Find nearest stations, by index or web id",
        argumentName = "index or web id",
    )
    { (indexOrWebId) ->
        val indexOrWebId = indexOrWebId.checkLastUsedIndexOrWebId()
        val property = propertyRepository.getProperty(indexOrWebId)
        property
            ?.let {
                GlobalVariables.lastUsedIndexOrWebId = indexOrWebId
                it.location
                    ?.let {
                        console.d(
                            getRoutesToNearestStations(DirectionsLatLon(it.latitude, it.longitude), requestCaller)
                                .orNull()
                                ?.joinToString("")
                                ?: "No nearby stations found"
                        )
                    } ?: run {
                    console.d("Property $indexOrWebId does not have a location")
                }
            }
            ?: run { console.d("Cannot find property with index or web id $indexOrWebId") }
    }

    private fun getPropertiesByIndexOrWebIdArray(arg: String): List<PersistedProperty> {
        val arg = arg.checkLastUsedIndexOrWebId()
        var properties = propertyRepository.getProperty(arg)?.let { listOf(it) }

        if (properties == null) {
            val tokens = arg.split(Regex("[,\\s]+"))
            properties = tokens.map { getPropertiesByIndexOrWebIdArray(it) }.flatten()
            val notFound = tokens - properties.map { it.index.toString() } - properties.map { it.webId }
            if (notFound.isNotEmpty()) {
                console.d("The following properties were not found in database: ${notFound.joinToString(", ")}")
            }
        }
        if (properties.size == 1) {
            GlobalVariables.lastUsedIndexOrWebId = arg
        }
        return properties
    }
}