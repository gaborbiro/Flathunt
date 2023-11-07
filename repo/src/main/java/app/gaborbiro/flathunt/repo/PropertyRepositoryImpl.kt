package app.gaborbiro.flathunt.repo

import app.gaborbiro.flathunt.GlobalVariables
import app.gaborbiro.flathunt.console.ConsoleWriter
import app.gaborbiro.flathunt.data.domain.Store
import app.gaborbiro.flathunt.data.domain.model.Property
import app.gaborbiro.flathunt.orNull
import app.gaborbiro.flathunt.repo.domain.DirectionsRepository
import app.gaborbiro.flathunt.repo.domain.PropertyRepository
import app.gaborbiro.flathunt.repo.validator.PropertyValidator
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
    private val validator: PropertyValidator by inject()
    private val console: ConsoleWriter by inject()
    private val directionsRepository: DirectionsRepository by inject()

    override fun getProperty(idx: String): Property? {
        val properties = store.getProperties()
        return properties.firstOrNull { it.index.toString() == idx }
            ?: properties.firstOrNull { it.webId == idx }
    }

    override fun getProperties(): List<Property> {
        return store.getProperties()
    }

    /**
     * @return true if it was added, false if it was updated
     */
    override fun addOrUpdateProperty(property: Property): Boolean {
        val properties: MutableList<Property> = store.getProperties().toMutableList()
        val index = properties.indexOfFirst { it.webId == property.webId }
        return if (index > -1) {
            properties[index] = property.copy(index = index)
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

    override fun validate(directions: Boolean) {
        val properties = getProperties()
        val unsuitable = properties.filter { validator.validate(it).isNotEmpty() }
        val unsuitableStr = unsuitable.joinToString("\n") { "#${it.index} ${it.webId}" }
        console.d("Deleting properties:")
        console.d(unsuitableStr.orNull())
        if (!GlobalVariables.safeMode) {
            val newProperties = store.getProperties() - unsuitable.toSet()
            store.overrideProperties(newProperties)
        }
        if (directions) {
            val (_, invalidDirectionsProperties) = directionsRepository.validateDirections()
            val toDelete = invalidDirectionsProperties.joinToString("\n") { "#${it.index} ${it.webId}" }
            console.d("Deleting properties:")
            console.d(toDelete)
        }
    }

    override fun deleteProperty(index: Int, markAsUnsuitable: Boolean, safeMode: Boolean): Boolean {
        val properties = store.getProperties().toMutableList()
        return properties.firstOrNull { it.index == index }?.let { property ->
            if (markAsUnsuitable && !safeMode) {
                webService.markAsUnsuitable(property.webId, unsuitable = true)
            }
            properties.removeIf { it.webId == property.webId }
            store.overrideProperties(properties)
            console.d("Property deleted")

            true
        } ?: run {
            false
        }
    }

    override fun getPropertyUrl(webId: String): String {
        return utilsService.getUrlFromWebId(webId)
    }

    override fun clearProperties(): Int {
        return store.clearProperties()
    }

    override fun getBlacklist(): List<String> {
        return store.getBlacklistWebIds()
    }

    override fun addToBlacklist(webIds: List<String>) {
        val blacklist = getBlacklist()
        store.saveBlacklistWebIds(webIds - blacklist + blacklist)
    }

    override fun clearBlacklist(): Int {
        return store.clearBlacklist()
    }

    override fun openLinks(property: Property) {
        browser.closeUnpinnedTabs()
        browser.pinTabs()
        browser.openTabs(property.links)

        val photoUrls = mutableListOf<String>()
        property.staticMapUrl?.let { photoUrls.add(it) }
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

    override fun getNextProperty(idx: String): Property? {
        return getProperty(idx)?.let { currentProperty ->
            store.getProperties().sortedBy { it.index }.firstOrNull { it.index!! > currentProperty.index!! }
        }
    }

    override fun reindex() {
        val properties: List<Property> = store.getProperties()
        store.resetIndexCounter()
        if (!GlobalVariables.safeMode) {
            store.overrideProperties(properties.map { it.copy(index = null) })
        }
    }
}