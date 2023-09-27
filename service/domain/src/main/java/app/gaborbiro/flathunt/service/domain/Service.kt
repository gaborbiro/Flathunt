package app.gaborbiro.flathunt.service.domain

import app.gaborbiro.flathunt.data.domain.model.Message
import app.gaborbiro.flathunt.data.domain.model.Property
import app.gaborbiro.flathunt.repo.domain.model.MessageTag
import app.gaborbiro.flathunt.service.domain.model.PageInfo
import org.openqa.selenium.Cookie

interface Service {

    ///// Functions that require an open webpage

    fun getPageInfo(searchUrl: String, propertiesRemoved: Int = 0): PageInfo

    fun fetchProperty(webId: String): Property

    fun markAsUnsuitable(webId: String, unsuitable: Boolean)

    fun getPhotoUrls(webId: String): List<String>

    fun fetchMessages(safeMode: Boolean): List<Message>

    fun tagMessage(messageUrl: String, vararg tags: MessageTag)

    ///// Functions that require a browser instance but not an open webpage

    fun openTabs(vararg urls: String): List<String>

    fun openHTML(html: String)

    fun cleanup()

    fun pinCurrentTabs()

    fun closeUnpinnedTabs()

    fun clearCookies()

    fun addOrUpdateCookies(cookies: List<Cookie>)

    fun saveCookies()

    ///// Functions that are service dependent, but do not require a browser instance

    fun getNextPageUrl(page: PageInfo, markedAsUnsuitableCount: Int): String?

    fun getPropertyIdFromUrl(url: String): String

    fun getUrlFromWebId(webId: String): String

    fun isValidUrl(url: String): Boolean

    fun cleanUrl(url: String): String

    fun parseUrlOrWebId(arg: String): String?
}
