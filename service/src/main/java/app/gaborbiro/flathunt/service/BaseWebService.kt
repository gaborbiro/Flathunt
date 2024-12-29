package app.gaborbiro.flathunt.service

import app.gaborbiro.flathunt.console.ConsoleWriter
import app.gaborbiro.flathunt.data.domain.Store
import app.gaborbiro.flathunt.data.domain.model.Message
import app.gaborbiro.flathunt.data.domain.model.Property
import app.gaborbiro.flathunt.repo.domain.model.MessageTag
import app.gaborbiro.flathunt.service.domain.UtilsService
import app.gaborbiro.flathunt.service.domain.WebService
import app.gaborbiro.flathunt.service.domain.model.PageInfo
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.openqa.selenium.NoSuchWindowException
import org.openqa.selenium.WebDriver

abstract class BaseWebService : WebService, KoinComponent {

    protected abstract val rootUrl: String

    /**
     * Will override from store, even if partially
     */
    protected open val overrideCookies: List<Pair<String, String>> = emptyList()

    /**
     * If any of these are missing from session, will try to set them from store, but only if all are present in store
     */
    protected open val importantCookies: List<Pair<String, String>> = emptyList()

    private val store: Store by inject()
    private val console: ConsoleWriter by inject()
    protected val utilsService: UtilsService by inject()

    private val browser: BrowserImpl by inject()

    ///// Functions that require an open webpage

    final override fun getPageInfo(searchUrl: String, propertiesRemoved: Int): PageInfo {
        ensurePageWithSession(searchUrl, log = true)
        return getPageInfo(browser, searchUrl)
    }

    protected abstract fun getPageInfo(driver: WebDriver, searchUrl: String): PageInfo

    final override fun fetchProperty(webId: String): Property {
        ensurePageWithSession(utilsService.getUrlFromWebId(webId))
        return fetchProperty(browser, webId)
    }

    protected abstract fun fetchProperty(driver: WebDriver, webId: String): Property

    final override fun updateSuitability(webId: String, suitable: Boolean) {
        val properties = store.getProperties()
        val property = properties.firstOrNull { it.webId == webId }
        val description = property?.index?.let { "($it)" } ?: "()"
        updateSuitability(browser, webId, suitable, description)
        property?.let {
            store.overrideProperties(properties - property + property.copy(markedUnsuitable = suitable.not()))
        }
        console.d("Marked as suitable=$suitable")
    }

    protected abstract fun updateSuitability(driver: WebDriver, webId: String, suitable: Boolean, description: String)

    final override fun getPhotoUrls(webId: String): List<String> {
        ensurePageWithSession(utilsService.getUrlFromWebId(webId))
        return getPhotoUrls(browser, webId)
    }

    protected abstract fun getPhotoUrls(driver: WebDriver, webId: String): List<String>


    final override fun fetchMessages(safeMode: Boolean): List<Message> {
        return fetchMessages(browser, safeMode)
    }

    protected open fun fetchMessages(driver: WebDriver, safeMode: Boolean): List<Message> {
        throw NotImplementedError()
    }

    final override fun tagMessage(messageUrl: String, vararg tags: MessageTag) {
        tagMessage(browser, messageUrl, *tags)
    }

    protected open fun tagMessage(driver: WebDriver, messageUrl: String, vararg tags: MessageTag) {
        throw NotImplementedError()
    }

    ///// Functions that are not service dependent

    protected fun ensurePageWithSession(vararg expectedUrls: String, log: Boolean = false) {
        runCatching { browser.currentUrl }.exceptionOrNull()?.let {
            browser.switchTo().window("")
        }
        val finalUrls = if (expectedUrls.isEmpty()) {
            arrayOf(rootUrl)
        } else expectedUrls

        if (finalUrls.none { url -> browser.currentUrl.startsWith(url) }) {
            // none of the expected urls are open
            try {
                browser[finalUrls[0]]
            } catch (e: NoSuchWindowException) {
                browser.openNewTab()
                browser[finalUrls[0]]
            }
        } else {
            // at least one expected url already open
        }

        beforeEnsureSession(browser)
        val (importantCookiesAvailable, refresh) = browser.ensureImportantCookies(
            overrideCookies = overrideCookies,
            importantCookies = importantCookies,
            storedCookies = store.getCookies(),
            log,
        )
        if (refresh) {
            browser[finalUrls[0]]
        }
        login(browser)

        Thread.sleep(500)
        browser[finalUrls[0]]
        if (importantCookiesAvailable.not()) {
            browser.getCookies()
        }
        afterEnsureSession(browser)
    }

    override fun login() {
        ensurePageWithSession(log = true)
    }

    protected abstract fun login(driver: WebDriver): Boolean

    protected open fun beforeEnsureSession(driver: WebDriver) {
        // override in subclass
    }

    protected open fun afterEnsureSession(driver: WebDriver) {
        // override in subclass
    }
}