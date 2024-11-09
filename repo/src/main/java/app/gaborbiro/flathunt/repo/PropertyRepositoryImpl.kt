package app.gaborbiro.flathunt.repo

import app.gaborbiro.flathunt.GlobalVariables
import app.gaborbiro.flathunt.console.ConsoleWriter
import app.gaborbiro.flathunt.data.domain.Store
import app.gaborbiro.flathunt.data.domain.model.Property
import app.gaborbiro.flathunt.or
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
    private val console: ConsoleWriter by inject()
    private val validator: PropertyValidator by inject()
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

    override fun validate() {
        webService.openRoot()
        val unsuitable = store.getProperties().filter { property ->
            if (validator.isValid(property)) {
                val propertyWithRoutes = directionsRepository.validateDirections(property)
                if (propertyWithRoutes != null) {
                    addOrUpdateProperty(propertyWithRoutes)
                    true
                } else {
                    if (property.markedUnsuitable.not() && GlobalVariables.safeMode.not()) {
                        updateSuitability(property.webId, suitable = false)
                    }
                    false
                }
            } else {
                if (property.markedUnsuitable.not() && GlobalVariables.safeMode.not()) {
                    updateSuitability(property.webId, suitable = false)
                }
                false
            }
        }
        val unsuitableStr = unsuitable.joinToString("\n") { "#${it.index} ${it.webId}" }
        console.d("Unsuitable properties: ", newLine = false)
        console.d(unsuitableStr.or("none"))
    }

    override fun deleteProperty(index: Int): Boolean {
        val properties = store.getProperties().toMutableList()
        return properties.firstOrNull { it.index == index }?.let { property ->
            properties.removeIf { it.webId == property.webId }
            store.overrideProperties(properties)
            console.d("Property deleted")
            true
        } ?: run {
            console.d("Index $index not found")
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

    override fun addToBlacklist(webId: String) {
        val newBlacklist = getBlacklist() + webId
        store.saveBlacklistWebIds(newBlacklist.distinct())
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

    override fun updateSuitability(webId: String, suitable: Boolean) {
        webService.updateSuitability(webId, suitable)
    }

    override fun getNextProperty(idx: String): Property? {
        return getProperty(idx)?.let { currentProperty ->
            store.getProperties().sortedBy { it.index }.firstOrNull { it.index!! > currentProperty.index!! }
        }
    }

    override fun reindex() {
        val properties: List<Property> = store.getProperties()
        store.resetIndexCounter()
        if (GlobalVariables.safeMode.not()) {
            store.overrideProperties(properties.map { it.copy(index = null) })
        }
    }
}