package app.gaborbiro.flathunt.usecase

import app.gaborbiro.flathunt.GlobalVariables
import app.gaborbiro.flathunt.data.Store
import app.gaborbiro.flathunt.data.model.PersistedProperty
import app.gaborbiro.flathunt.google.getRoutesToNearestStations
import app.gaborbiro.flathunt.orNull
import app.gaborbiro.flathunt.prettyPrint
import app.gaborbiro.flathunt.service.Service
import app.gaborbiro.flathunt.usecase.base.BaseUseCase
import app.gaborbiro.flathunt.usecase.base.Command
import app.gaborbiro.flathunt.usecase.base.ValidationCriteria
import app.gaborbiro.flathunt.usecase.base.command

class PropertyUseCase(service: Service, private val store: Store, criteria: ValidationCriteria) :
    BaseUseCase(service, store, criteria) {

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
        val properties = store.getProperties()
        val property = properties.firstOrNull { it.index.toString() == indexOrId }
            ?: properties.firstOrNull { it.id == indexOrId }
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
        service.popTabHandles()
        getPropertiesByIndexOrIds(arg).forEach(::openLinks)
    }

    private val tyn = command(
        command = "tyn",
        description = "(Thank You Next) Deletes last viewed property, marks it as unsuitable/hidden and opens next index in browser",
    )
    {
        service.popTabHandles()
        val next = getNextProperty()
        getPropertiesByIndexOrIds("$").forEach {
            deleteProperty(it.index, markAsUnsuitable = true, GlobalVariables.safeMode)
        }
        next?.let {
            println("${it.index} - ${it.id}")
            GlobalVariables.lastUsedIndexOrId = it.index.toString()
            openLinks(it)
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
        service.popTabHandles()
        getPropertiesByIndexOrIds(arg).forEach {
            deleteProperty(it.index, markAsUnsuitable = mark, GlobalVariables.safeMode)
        }
    }

    private val mark = command<String, Boolean>(
        command = "mark",
        description = "Marks one or more properties as unsuitable/hidden or removes such mark, by indexes or ids (comma separated)",
        argumentName1 = "indexes or ids (comma separated)",
        argumentName2 = "unsuitable"
    )
    { (arg, unsuitable) ->
        getPropertiesByIndexOrIds(arg).forEach { service.markAsUnsuitable(it.id, it.index, unsuitable) }
    }

    private val comment = command<String, String>(
        command = "comment",
        description = "Set the comment field of a property, by index or id",
        argumentName1 = "index or id",
        argumentName2 = "comment",
    )
    { (indexOrId, comment) ->
        val indexOrId = indexOrId.checkLastUsedIndexOrId()
        val properties = store.getProperties()
        val property = properties.firstOrNull { it.index.toString() == indexOrId }
            ?: properties.firstOrNull { it.id == indexOrId }
        property
            ?.let {
                GlobalVariables.lastUsedIndexOrId = indexOrId
                addOrUpdateProperty(it.clone(comment = comment))
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
        val properties = store.getProperties()
        val property = properties.firstOrNull { it.index.toString() == indexOrId }
            ?: properties.firstOrNull { it.id == indexOrId }
        property
            ?.let {
                GlobalVariables.lastUsedIndexOrId = indexOrId
                if (comment.isNotBlank()) {
                    addOrUpdateProperty(it.clone(comment = it.comment + " " + comment))
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
        val properties = store.getProperties()
        val property = properties.firstOrNull { it.index.toString() == indexOrId }
            ?: properties.firstOrNull { it.id == indexOrId }
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

    private fun getPropertiesByIndexOrIds(arg: String): List<PersistedProperty> {
        val arg = arg.checkLastUsedIndexOrId()
        val properties = store.getProperties()
        return (properties.firstOrNull { it.index.toString() == arg }?.let { listOf(it) }
            ?: properties.firstOrNull { it.id == arg }?.let { listOf(it) })
            ?.let {
                GlobalVariables.lastUsedIndexOrId = arg
                it
            }
            ?: run {
                val tokens = arg.split(Regex("[,\\s]+"))
                properties.filter { it.index.toString() in tokens || it.id in tokens }.also {
                    val notFound = tokens - properties.map { it.index.toString() } - properties.map { it.id }
                    if (notFound.isNotEmpty()) {
                        println("The following properties were not found in database: ${notFound.joinToString(", ")}")
                    }
                }
            }
    }

    private fun getNextProperty(): PersistedProperty? {
        val indexOrId = "$".checkLastUsedIndexOrId()
        val properties = store.getProperties()
        val property = (properties.firstOrNull { it.index.toString() == indexOrId }?.let { listOf(it) }
            ?: properties.firstOrNull { it.id == indexOrId }?.let { listOf(it) })
            ?: null
        return property?.let {
            if (it.size == 1) {
                val index = it[0].index
                return properties.sortedBy { it.index }.firstOrNull { it.index > index }
            } else {
                null
            }
        }
    }
}