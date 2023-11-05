package app.gaborbiro.flathunt.usecase

import app.gaborbiro.flathunt.GlobalVariables
import app.gaborbiro.flathunt.console.ConsoleWriter
import app.gaborbiro.flathunt.criteria.POI
import app.gaborbiro.flathunt.criteria.POITravelMode
import app.gaborbiro.flathunt.data.domain.model.Property
import app.gaborbiro.flathunt.directions.DirectionsService
import app.gaborbiro.flathunt.directions.model.DirectionsLatLon
import app.gaborbiro.flathunt.directions.model.DirectionsTravelLimit
import app.gaborbiro.flathunt.directions.model.DirectionsTravelMode
import app.gaborbiro.flathunt.orNull
import app.gaborbiro.flathunt.prettyPrint
import app.gaborbiro.flathunt.repo.domain.PropertyRepository
import app.gaborbiro.flathunt.usecase.base.BaseUseCase
import app.gaborbiro.flathunt.usecase.base.Command
import app.gaborbiro.flathunt.usecase.base.command
import org.koin.core.component.inject

class ManagePropertyUseCase : BaseUseCase() {

    private val propertyRepository: PropertyRepository by inject()
    private val directionsService: DirectionsService by inject()
    private val console: ConsoleWriter by inject()

    override val commands: List<Command<*>>
        get() = listOf(
            print,
            open,
            tyn,
            delete,
            unsuitable,
            comment,
            commentAppend,
            stations,
            verify,
        )

    private val print = command<String>(
        command = "print",
        description = "Print a property",
        argumentDescription = "idx",
    )
    { (idx) ->
        val nonWildIdx = idx.checkLastUsedIdx()
        val property = propertyRepository.getProperty(nonWildIdx)
        property?.let {
            GlobalVariables.lastIdx = property.webId
        }
        property
            ?.prettyPrint()?.let(::println)
            ?: run { console.d("Cannot find property with index or web id $nonWildIdx") }
    }

    private val open = command<String>(
        command = "open",
        description = "Open properties in browser",
        argumentDescription = "idx (comma separated)",
    )
    { (idxs) ->
        getPropertiesByIdxs(idxs.checkLastUsedIdx())
            .forEach(propertyRepository::openLinks)
    }

    private val tyn = command(
        command = "tyn",
        description = "(Thank You Next) Delete last viewed property, mark it as unsuitable " +
                "and open next available index in browser",
    )
    {
        val next = propertyRepository.getNextProperty(GlobalVariables.lastIdx!!)
        getPropertiesByIdxs(GlobalVariables.lastIdx!!)
            .forEach {
                propertyRepository.deleteProperty(it.index!!, markAsUnsuitable = true, GlobalVariables.safeMode)
            }
        next?.let {
            console.d("${it.index} - ${it.webId}")
            GlobalVariables.lastIdx = it.webId
            propertyRepository.openLinks(it)
        } ?: run { console.d("Nothing to open") }
    }

    private val delete = command<String, Boolean>(
        command = "delete",
        description = "Delete one or more properties from the database",
        argumentName1 = "idx (comma separated)",
        argumentName2 = "unsuitable",
    )
    { (idxs, unsuitable) ->
        getPropertiesByIdxs(idxs.checkLastUsedIdx())
            .forEach {
                propertyRepository.deleteProperty(it.index!!, markAsUnsuitable = unsuitable, GlobalVariables.safeMode)
            }
    }

    private val unsuitable = command<String, Boolean>(
        command = "unsuitable",
        description = "Marks one or more properties as unsuitable/suitable",
        argumentName1 = "idx (comma separated)",
        argumentName2 = "unsuitable",
    )
    { (idxs, unsuitable) ->
        getPropertiesByIdxs(idxs.checkLastUsedIdx())
            .forEach { propertyRepository.markAsUnsuitable(it.webId, unsuitable) }
    }

    private val comment = command<String, String>(
        command = "comment",
        description = "Set the comment field of a property, by index or web id",
        argumentName1 = "idx",
        argumentName2 = "comment",
    )
    { (idx, comment) ->
        val nonWildIdx = idx.checkLastUsedIdx()
        val property = propertyRepository.getProperty(nonWildIdx)
        property
            ?.let {
                GlobalVariables.lastIdx = nonWildIdx
                propertyRepository.addOrUpdateProperty(it.copy(comment = comment))
            }
            ?: run { console.d("Cannot find property with index or web id $nonWildIdx") }
    }

    private val commentAppend = command<String, String>(
        command = "comment+",
        description = "Append comment to a property, by index or web id",
        argumentName1 = "idx",
        argumentName2 = "comment",
    )
    { (idx, comment) ->
        val nonWildIdx = idx.checkLastUsedIdx()
        val property = propertyRepository.getProperty(nonWildIdx)
        property
            ?.let {
                GlobalVariables.lastIdx = nonWildIdx
                if (comment.isNotBlank()) {
                    propertyRepository.addOrUpdateProperty(it.copy(comment = it.comment + " " + comment))
                }
            }
            ?: run { console.d("Cannot find property with index or web id $nonWildIdx") }
    }

    private val stations = command<String>(
        command = "stations",
        description = "Find nearest stations for property",
        argumentDescription = "idx",
    )
    { (idx) ->
        val nonWildIdx = idx.checkLastUsedIdx()
        val property = propertyRepository.getProperty(nonWildIdx)
        property
            ?.let {
                GlobalVariables.lastIdx = nonWildIdx
                it.location
                    ?.let {
                        val poi = POI.NearestRailStation.max[0]
                        val travelMode = when (poi.mode) {
                            POITravelMode.TRANSIT -> DirectionsTravelMode.TRANSIT
                            POITravelMode.CYCLING -> DirectionsTravelMode.CYCLING
                            POITravelMode.WALKING -> DirectionsTravelMode.WALKING
                        }
                        val routesStr = directionsService.getRoutesToNearestStations(
                            from = DirectionsLatLon(latitude = it.latitude, longitude = it.longitude),
                            limit = DirectionsTravelLimit(travelMode, poi.maxMinutes)
                        )
                            .orNull()
                            ?.joinToString("")
                            ?: "No nearby stations found"

                        console.d(routesStr)
                    } ?: run { console.d("Property $nonWildIdx does not have a location") }
            }
            ?: run { console.d("Cannot find property with index or web id $nonWildIdx") }
    }

    private val verify = command<Boolean>(
        command = "verify",
        description = "Remove all invalid properties from the database. Useful for revalidating previously added " +
                "properties after criteria change. Use the directions argument (false by default) to also rerun " +
                "directions calculations. Note: this operation marks properties as unsuitable on the website.",
        argumentDescription = "directions",
    )
    { (directions) ->
        propertyRepository.verify(directions)
    }

    private fun getPropertiesByIdxs(idxs: String): List<Property> {
        var properties = propertyRepository.getProperty(idxs)?.let { listOf(it) } ?: emptyList()

        if (properties.isEmpty()) {
            val tokens = idxs.split(Regex("[,\\s]+"))

            if (tokens.size > 1) {
                properties = tokens.map { getPropertiesByIdxs(it) }.flatten()
            }
            val notFound = tokens - properties.map { it.index.toString() } - properties.map { it.webId }
            if (notFound.isNotEmpty()) {
                console.d("The following properties were not found in database: ${notFound.joinToString(", ")}")
            }
        }
        if (properties.size == 1) {
            GlobalVariables.lastIdx = properties[0].webId
        }
        return properties
    }
}