package app.gaborbiro.flathunt.usecase.base

import app.gaborbiro.flathunt.*
import app.gaborbiro.flathunt.data.domain.Store
import app.gaborbiro.flathunt.data.domain.model.PersistedProperty
import app.gaborbiro.flathunt.data.domain.model.Property
import app.gaborbiro.flathunt.google.POI
import app.gaborbiro.flathunt.google.calculateRoutes
import app.gaborbiro.flathunt.service.domain.Service
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

abstract class BaseUseCase: UseCase, KoinComponent {

    private val store: Store by inject<Store>()
    private val service: Service by inject<Service>()
    private val criteria: ValidationCriteria by inject<ValidationCriteria>()

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