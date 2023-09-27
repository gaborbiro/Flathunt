package app.gaborbiro.flathunt.service.domain

import app.gaborbiro.flathunt.data.domain.model.Message
import app.gaborbiro.flathunt.data.domain.model.Property
import app.gaborbiro.flathunt.repo.domain.model.MessageTag
import app.gaborbiro.flathunt.service.domain.model.PageInfo
import org.openqa.selenium.Cookie

interface Service {

    fun openTabs(vararg urls: String): List<String>

    fun openHTML(html: String)

    /**
     * Starting from a page other than the first one is supported.
     */
    fun getPageInfo(searchUrl: String, propertiesRemoved: Int = 0): PageInfo

    fun fetchProperty(webId: String, newTab: Boolean = false): Property

    fun markAsUnsuitable(webId: String, unsuitable: Boolean)

    fun getPhotoUrls(webId: String): List<String>

    fun fetchMessages(safeMode: Boolean): List<Message>

    fun tagMessage(messageUrl: String, vararg tags: MessageTag)

    fun getNextPageUrl(page: PageInfo, markedAsUnsuitableCount: Int): String?

    fun getPropertyIdFromUrl(url: String): String

    fun getUrlFromWebId(webId: String): String

    fun isValidUrl(url: String): Boolean

    fun cleanUrl(url: String): String

    fun parseUrlOrWebId(arg: String): String?

    fun cleanup()

    fun pinCurrentTabs()

    fun closeUnpinnedTabs()

    fun clearCookies()

    fun addOrUpdateCookies(cookies: List<Cookie>)

    fun saveCookies()
}
