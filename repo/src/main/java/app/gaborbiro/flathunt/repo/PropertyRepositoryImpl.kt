package app.gaborbiro.flathunt.repo

import app.gaborbiro.flathunt.*
import app.gaborbiro.flathunt.console.ConsoleWriter
import app.gaborbiro.flathunt.data.domain.Store
import app.gaborbiro.flathunt.data.domain.model.PersistedProperty
import app.gaborbiro.flathunt.data.domain.model.Property
import app.gaborbiro.flathunt.repo.domain.PropertyRepository
import app.gaborbiro.flathunt.service.domain.Browser
import app.gaborbiro.flathunt.service.domain.UtilsService
import app.gaborbiro.flathunt.service.domain.WebService
import org.koin.core.annotation.Singleton
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@Singleton
class PropertyRepositoryImpl : PropertyRepository, KoinComponent {

    private val store: Store by inject()
    private val webService: WebService by inject()
    private val utilsService: UtilsService by inject()
    private val browser: Browser by inject()
    private val criteria: ValidationCriteria by inject()
    private val validator: PropertyValidator by inject()
    private val console: ConsoleWriter by inject()

    override fun getProperty(indexOrWebId: String): PersistedProperty? {
        val properties = store.getProperties()
        return properties.firstOrNull { it.index.toString() == indexOrWebId }
            ?: properties.firstOrNull { it.webId == indexOrWebId }
    }

    override fun getProperties(): List<PersistedProperty> {
        return store.getProperties()
    }

    /**
     * @return true if it was added, false if it was updated
     */
    override fun addOrUpdateProperty(property: Property): Boolean {
        val properties: MutableList<Property> = store.getProperties().toMutableList()
        val index = properties.indexOfFirst { it.webId == property.webId }
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
        val toDelete = (properties - validProperties.toSet()).joinToString("\n") { "#${it.index} ${it.webId}" }
        console.d("Deleting properties:")
        console.d(toDelete)
        if (!GlobalVariables.safeMode) {
            store.overrideProperties(validProperties)
        }
    }

    override fun deleteProperty(index: Int, markAsUnsuitable: Boolean, safeMode: Boolean): Boolean {
        val properties = store.getProperties().toMutableList()
        return properties.firstOrNull { it.index == index }?.let { property ->
            properties.removeIf { it.webId == property.webId }
            store.overrideProperties(properties)
            console.d("Property deleted")
            if (markAsUnsuitable && !safeMode) {
                webService.markAsUnsuitable(property.webId, unsuitable = true)
            }
            true
        } ?: run {
            false
        }
    }

    override fun getPropertyUrl(webId: String): String {
        return utilsService.getUrlFromWebId(webId)
    }

    override fun clearProperties() {
        store.clearProperties()
    }

    override fun getBlacklist(): List<String> {
        return store.getBlacklistWebIds()
    }

    override fun addToBlacklist(webIds: List<String>) {
        val blacklist = getBlacklist()
        store.saveBlacklistWebIds(webIds - blacklist + blacklist)
    }

    override fun clearBlacklist() {
        store.clearBlacklist()
    }

    override fun openLinks(property: Property) {
        browser.closeUnpinnedTabs()
        browser.pinTabs()
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

        browser.openTabs(*urls.toTypedArray())

        val photoUrls = mutableListOf<String>()
        staticMapUrl?.let { photoUrls.add(it) }
        photoUrls.addAll(webService.getPhotoUrls(property.webId))
        if (photoUrls.isNotEmpty()) {
            val html = photoUrls.joinToString("") {
                "<img src=\"$it\" width=\"500\" align=\"top\">"
            }
            browser.openHTML(html)
        }
    }

    override fun markAsUnsuitable(webId: String, unsuitable: Boolean) {
        webService.markAsUnsuitable(webId, unsuitable)
    }

    override fun getNextProperty(indexOrWebId: String): PersistedProperty? {
        return getProperty(indexOrWebId)?.let { currentProperty ->
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