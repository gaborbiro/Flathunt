package app.gaborbiro.flathunt.service

import app.gaborbiro.flathunt.console.ConsoleWriter
import app.gaborbiro.flathunt.data.domain.Store
import app.gaborbiro.flathunt.data.domain.model.Message
import app.gaborbiro.flathunt.data.domain.model.Property
import app.gaborbiro.flathunt.matcher
import app.gaborbiro.flathunt.repo.domain.model.MessageTag
import app.gaborbiro.flathunt.service.domain.UtilsService
import app.gaborbiro.flathunt.service.domain.WebService
import app.gaborbiro.flathunt.service.domain.model.PageInfo
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.component.inject
import org.openqa.selenium.NoSuchWindowException
import org.openqa.selenium.WebDriver

abstract class BaseWebService : WebService, KoinComponent {


    protected abstract val rootUrl: String
    protected abstract val sessionCookieName: String
    protected abstract val sessionCookieDomain: String

    private val store: Store by inject()
    private val console: ConsoleWriter by inject()
    protected val utilsService: UtilsService by inject()
    private var browserLaunched: Boolean = false

    private val driver: WebDriver by lazy {
        browserLaunched = true
        get()
    }

    private val browser: BrowserImpl by inject()

    ///// Functions that require an open webpage

    final override fun getPageInfo(searchUrl: String, propertiesRemoved: Int): PageInfo {
        ensurePageWithSession(searchUrl)
        return getPageInfo(driver, searchUrl)
    }

    protected abstract fun getPageInfo(driver: WebDriver, searchUrl: String): PageInfo

    final override fun fetchProperty(webId: String): Property {
        ensurePageWithSession(utilsService.getUrlFromWebId(webId))
        return fetchProperty(driver, webId)
    }

    protected abstract fun fetchProperty(driver: WebDriver, webId: String): Property

    final override fun markAsUnsuitable(webId: String, unsuitable: Boolean) {
        val description = store.getProperties().firstOrNull { it.webId == webId }?.index?.let { "($it)" } ?: "()"
        val blacklist = store.getBlacklistWebIds().toMutableList().also {
            it.add(webId)
        }
        store.saveBlacklistWebIds(blacklist)
        markAsUnsuitable(driver, webId, unsuitable, description)
        console.d("Marked as unsuitable")
    }

    protected abstract fun markAsUnsuitable(driver: WebDriver, webId: String, unsuitable: Boolean, description: String)

    final override fun getPhotoUrls(webId: String): List<String> {
        ensurePageWithSession(utilsService.getUrlFromWebId(webId))
        return getPhotoUrls(driver, webId)
    }

    protected abstract fun getPhotoUrls(driver: WebDriver, webId: String): List<String>


    final override fun fetchMessages(safeMode: Boolean): List<Message> {
        return fetchMessages(driver, safeMode)
    }

    protected open fun fetchMessages(driver: WebDriver, safeMode: Boolean): List<Message> {
        throw NotImplementedError()
    }

    final override fun tagMessage(messageUrl: String, vararg tags: MessageTag) {
        tagMessage(driver, messageUrl, *tags)
    }

    protected open fun tagMessage(driver: WebDriver, messageUrl: String, vararg tags: MessageTag) {
        throw NotImplementedError()
    }

    ///// Functions that are not service dependent

    protected fun ensurePageWithSession(vararg expectedUrls: String) {
        runCatching { driver.currentUrl }.exceptionOrNull()?.let {
            driver.switchTo().window("")
        }
        val finalUrls = if (expectedUrls.isEmpty()) {
            arrayOf(rootUrl)
        } else expectedUrls

        if (finalUrls.none { url -> driver.currentUrl.startsWith(url) }) {
            // none of the expected urls are open
            try {
                driver[finalUrls[0]]
            } catch (e: NoSuchWindowException) {
                browser.openNewTab()
                driver[finalUrls[0]]
            }
        } else {
            // at least one expected url already open
        }

        beforeEnsureSession(driver)
        val (sessionAvailable, refresh) = browser.ensureSession(sessionCookieName, sessionCookieDomain)
        afterEnsureSession(driver)

        if (sessionAvailable.not() && login(driver)) {
            Thread.sleep(500)
            browser.saveCookies()
        }
        if (refresh) {
            driver[finalUrls[0]]
        }
    }

    protected abstract fun login(driver: WebDriver): Boolean

    protected open fun beforeEnsureSession(driver: WebDriver) {
        // override in subclass
    }

    protected open fun afterEnsureSession(driver: WebDriver) {
        // override in subclass
    }
}