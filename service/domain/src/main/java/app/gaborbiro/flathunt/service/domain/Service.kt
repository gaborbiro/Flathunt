package app.gaborbiro.flathunt.service.domain

import app.gaborbiro.flathunt.data.domain.model.Message
import app.gaborbiro.flathunt.data.domain.model.Property
import app.gaborbiro.flathunt.repo.domain.model.MessageTag
import app.gaborbiro.flathunt.service.domain.model.Page
import org.openqa.selenium.Cookie

interface Service {

    /**
     * Assumes root url is open
     */
    fun login()

    fun openTabs(vararg urls: String): List<String>

    /**
     * Starting from a page other than the first one is supported.
     */
    fun fetchLinksFromSearch(searchUrl: String, propertiesRemoved: Int = 0): Page

    fun fetchProperty(id: String, newTab: Boolean = false): Property

    fun markAsUnsuitable(id: String, unsuitable: Boolean, description: String)

    fun getPropertyIdFromUrl(url: String): String

    fun getUrlFromId(id: String): String

    fun isValidUrl(url: String): Boolean

    fun cleanUrl(url: String): String

    fun checkUrlOrId(arg: String): String?

    fun cleanup()

    fun getPhotoUrls(id: String): List<String>

    fun pinCurrentTabs()

    fun closeUnpinnedTabs()

    fun fetchMessages(safeMode: Boolean): List<Message>

    fun tagMessage(messageUrl: String, vararg tags: MessageTag)

    fun clearCookies()

    fun addOrUpdateCookies(cookies: List<Cookie>)
}
