package app.gaborbiro.flathunt.usecases

import app.gaborbiro.flathunt.*
import app.gaborbiro.flathunt.data.Store
import app.gaborbiro.flathunt.data.model.PersistedProperty
import app.gaborbiro.flathunt.data.model.Property
import app.gaborbiro.flathunt.google.POI
import app.gaborbiro.flathunt.google.calculateRoutes
import app.gaborbiro.flathunt.service.Service

abstract class BaseUseCase(
    private val service: Service,
    private val store: Store,
    protected val criteria: ValidationCriteria
) : UseCase {

    /**
     * Calculate routes for each property.
     * Deletes properties that are too far and marks corresponding message (if known).
     *
     * @return list of valid and invalid properties (according to route)
     */
    fun fetchRoutes(properties: List<Property>, safeMode: Boolean): Pair<List<Property>, List<Property>> {
        if (properties.isEmpty()) {
            println("No saved properties. Fetch some")
            return Pair(emptyList(), emptyList())
        }
        val (toSave, unsuitable) = properties.partition { property ->
            val routes = calculateRoutes(property.location, criteria.pointsOfInterest)
            println("\n${property.id}: ${property.title}:\n${routes.joinToString(", ")}")
            val propertyWithRoutes = property.withRoutes(routes)
            addOrUpdateProperty(propertyWithRoutes)
            if (propertyWithRoutes.checkValid(criteria)) {
                println("Valid")
                true
            } else {
                if (!propertyWithRoutes.markedUnsuitable) {
                    if (!safeMode) service.markAsUnsuitable(
                        property.id,
                        (propertyWithRoutes as? PersistedProperty)?.index,
                        true
                    )
                }
                false
            }
        }
        return toSave to unsuitable
    }

    /**
     * Ad-hoc scan of a property. Marks property as unsuitable if needed.
     */
    fun fetchProperty(arg: String, save: SaveType, safeMode: Boolean): Property? {
        val cleanUrl = service.checkUrlOrId(arg)
        if (cleanUrl != null) {
            println()
            println(cleanUrl)
            val id = service.getPropertyIdFromUrl(cleanUrl)
            GlobalVariables.lastUsedIndexOrId = id
            val property = service.fetchProperty(id, newTab = true)
            if (property.isBuddyUp && save != SaveType.FORCE_ADD) {
                println("\nBuddy up - skipping...")
                return null
            }
            val routes = calculateRoutes(property.location, criteria.pointsOfInterest)
            val propertyWithRoutes = property.withRoutes(routes)
            println(propertyWithRoutes.prettyPrint())
            if (propertyWithRoutes.checkValid(criteria)) {
                when (save) {
                    SaveType.FORCE_ADD -> addOrUpdateProperty(propertyWithRoutes)
                    SaveType.CHECK -> {
                    }

                    SaveType.ADD -> addOrUpdateProperty(propertyWithRoutes)
                }
            } else if (save == SaveType.FORCE_ADD) {
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

    /**
     * @return true if it was added, false if it was updated
     */
    fun addOrUpdateProperty(property: Property): Boolean {
        val properties: MutableList<Property> = store.getProperties().toMutableList()
        val index = properties.indexOfFirst { it.id == property.id }
        return if (index > -1) {
            properties[index] = PersistedProperty(property, (properties[index] as PersistedProperty).index)
            println("\nProperty updated")
            false
        } else {
            properties.add(property)
            println("\nProperty added")
            true
        }.also {
            store.saveProperties(properties)
        }
    }

    fun deleteProperty(index: Int, markAsUnsuitable: Boolean, safeMode: Boolean): Boolean {
        val properties = store.getProperties().toMutableList()
        return properties.firstOrNull { it.index == index }?.let { property ->
            properties.removeIf { it.id == property.id }
            store.saveProperties(properties)
            println("Property deleted")
            if (markAsUnsuitable && !safeMode) {
                service.markAsUnsuitable(property.id, (property as? PersistedProperty)?.index, true)
            }
            true
        } ?: run {
            false
        }
    }

    fun openLinks(property: Property) {
        val urls = mutableListOf<String>()
        property.messageUrl?.let(urls::add)
        if (property.routes?.isNotEmpty() != true) {
            property.location?.toURL()?.let(urls::add)
        }
        property.routes?.forEach {
            val destination = it.name?.let { escapeHTML(it) } ?: it.coordinates.toGoogleCoords()
            val url = "https://www.google.com/maps/dir/?api=1" +
                    "&origin=${property.location?.toGoogleCoords()}" +
                    "&destination=$destination" +
//                    (it.placeId?.let { "&destination_place_id=$it" } ?: "") +
                    "&travelmode=${it.travelLimit.mode.value}"
            urls.add(url)
        }
        property.location?.let {
            urls.add("https://www.google.com/maps/@?api=1&map_action=pano&viewpoint=${it.toGoogleCoords()}&heading=13&pitch=0&fov=80")
        }
        service.pushTabHandles()

        val staticMapUrl = property.location?.let {
            val nearestStations =
                property.routes?.filter { it.description == POI.NearestRailStation.description }?.map {
                    "&markers=size:mid|color:green|${it.coordinates.toGoogleCoords()}"
                }
            val pois = criteria.pointsOfInterest.filterIsInstance<POI.Destination>().map {
                "&markers=size:mid|color:blue|${escapeHTML(it.name)} London"
            }
            "http://maps.googleapis.com/maps/api/staticmap?" +
                    "&size=600x400" +
                    "&markers=size:mid|color:red|${property.location!!.toGoogleCoords()}" +
                    (nearestStations?.joinToString("") ?: "") +
                    pois.joinToString("") +
                    "&key=${LocalProperties.googleApiKey}"
        }?.replace(",", "%2C")

        val photoUrls = mutableListOf<String>()
        staticMapUrl?.let { photoUrls.add(it) }
        photoUrls.addAll(service.getPhotoUrls(property.id))
        if (photoUrls.isNotEmpty()) {
            val url = "http://127.0.0.1:8000/photos/" + photoUrls.joinToString(",")
            urls.add(url)
        }
        service.openTabs(*urls.toTypedArray())
    }

    protected fun String.checkLastUsedIndexOrId(): String {
        return GlobalVariables.lastUsedIndexOrId?.let {
            if (this == "$") {
                GlobalVariables.lastUsedIndexOrId
            } else {
                this
            }
        } ?: this
    }

    private fun openUrl(url: String) {
        Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler $url").waitFor()
    }
}