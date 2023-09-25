package app.gaborbiro.flathunt.repo

import app.gaborbiro.flathunt.*
import app.gaborbiro.flathunt.console.ConsoleWriter
import app.gaborbiro.flathunt.data.domain.Store
import app.gaborbiro.flathunt.data.domain.model.PersistedProperty
import app.gaborbiro.flathunt.data.domain.model.Property
import app.gaborbiro.flathunt.repo.domain.PropertyRepository
import app.gaborbiro.flathunt.service.domain.Service
import org.koin.core.annotation.Singleton
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@Singleton
class PropertyRepositoryImpl : PropertyRepository, KoinComponent {

    private val store: Store by inject()
    private val service: Service by inject()
    private val criteria: ValidationCriteria by inject()
    private val validator: PropertyValidator by inject()
    private val console: ConsoleWriter by inject()

    override fun getProperty(indexOrId: String): PersistedProperty? {
        val properties = store.getProperties()
        return properties.firstOrNull { it.index.toString() == indexOrId }
            ?: properties.firstOrNull { it.id == indexOrId }
    }

    override fun getProperties(): List<PersistedProperty> {
        return store.getProperties()
    }

    /**
     * @return true if it was added, false if it was updated
     */
    override fun addOrUpdateProperty(property: Property): Boolean {
        val properties: MutableList<Property> = store.getProperties().toMutableList()
        val index = properties.indexOfFirst { it.id == property.id }
        return if (index > -1) {
            properties[index] = PersistedProperty(property, index)
            console.d("Property updated")
            false
        } else {
            properties.add(property)
            console.d("Property added")
            true
        }.also {
            store.overrideProperties(properties)
        }
    }

    override fun verifyAll() {
        val properties = getProperties()
        val validProperties = properties.filter { validator.validate(it).isEmpty() }
        val toDelete = (properties - validProperties.toSet()).joinToString("\n") { "#${it.index} ${it.id}" }
        console.d("Deleting properties:")
        console.d(toDelete)
        if (!GlobalVariables.safeMode) {
            store.overrideProperties(validProperties)
        }
    }

    override fun deleteProperty(index: Int, markAsUnsuitable: Boolean, safeMode: Boolean): Boolean {
        val properties = store.getProperties().toMutableList()
        return properties.firstOrNull { it.index == index }?.let { property ->
            properties.removeIf { it.id == property.id }
            store.overrideProperties(properties)
            console.d("Property deleted")
            if (markAsUnsuitable && !safeMode) {
                service.markAsUnsuitable(property.id, unsuitable = true, description = "($index)")
            }
            true
        } ?: run {
            false
        }
    }

    override fun getPropertyUrl(id: String): String {
        return service.getUrlFromId(id)
    }

    override fun clearProperties() {
        store.clearProperties()
    }

    override fun getBlacklist(): List<String> {
        return store.getBlacklist()
    }

    override fun addToBlacklist(ids: List<String>) {
        val blacklist = getBlacklist()
        store.saveBlacklist(ids - blacklist + blacklist)
    }

    override fun clearBlacklist() {
        store.clearBlacklist()
    }

    override fun openLinks(property: Property) {
        service.closeUnpinnedTabs()
        service.pinCurrentTabs()
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

        val staticMapUrl = property.location?.let {
            val nearestStations =
                property.routes?.filter { it.description == POI.NearestRailStation.description }?.map {
                    "&markers=size:mid|color:green|${it.coordinates.toGoogleCoords()}"
                }
            val pois = criteria.pointsOfInterest.filterIsInstance<POI.Destination>().map {
                "&markers=size:mid|color:blue|${escapeHTML(it.name)} Lisbon"
            }
            "http://maps.googleapis.com/maps/api/staticmap?" +
                    "&size=600x400" +
                    "&markers=size:mid|color:red|${property.location!!.toGoogleCoords()}" +
                    (nearestStations?.joinToString("") ?: "") +
                    pois.joinToString("") +
                    "&key=${LocalProperties.googleApiKey}"
        }?.replace(",", "%2C")

        service.openTabs(*urls.toTypedArray())

        val photoUrls = mutableListOf<String>()
        staticMapUrl?.let { photoUrls.add(it) }
        photoUrls.addAll(service.getPhotoUrls(property.id))
        if (photoUrls.isNotEmpty()) {
            val html = photoUrls.joinToString("") {
                "<img src=\"$it\" width=\"500\" align=\"top\">"
            }
            service.openHTML(html)
        }
    }

    override fun markAsUnsuitable(property: Property, unsuitable: Boolean) {
        val index = (property as? PersistedProperty)?.index
        val description = index?.let { "($it)" } ?: ""
        service.markAsUnsuitable(property.id, unsuitable, description)
    }

    override fun getNextProperty(indexOrId: String): PersistedProperty? {
        return getProperty(indexOrId)?.let { currentProperty ->
            store.getProperties().sortedBy { it.index }.firstOrNull { it.index > currentProperty.index }
        }
    }

    override fun reindex() {
        val properties: List<PersistedProperty> = store.getProperties()
        store.resetIndexCounter()
        if (!GlobalVariables.safeMode) {
            store.overrideProperties(properties.map { Property(it) })
        }
    }
}