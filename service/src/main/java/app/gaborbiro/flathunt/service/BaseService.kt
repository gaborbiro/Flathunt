package app.gaborbiro.flathunt.service

import app.gaborbiro.flathunt.data.domain.Store
import app.gaborbiro.flathunt.data.domain.model.Cookies
import app.gaborbiro.flathunt.data.domain.model.Message
import app.gaborbiro.flathunt.data.domain.model.Property
import app.gaborbiro.flathunt.matcher
import app.gaborbiro.flathunt.repo.domain.model.MessageTag
import app.gaborbiro.flathunt.service.domain.Service
import app.gaborbiro.flathunt.service.domain.model.PageInfo
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.component.inject
import org.openqa.selenium.Cookie
import org.openqa.selenium.NoSuchWindowException
import org.openqa.selenium.UnexpectedAlertBehaviour
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.remote.CapabilityType
import org.openqa.selenium.remote.RemoteWebDriver
import java.nio.file.Paths
import java.util.*

abstract class BaseService : Service, KoinComponent {

    private val tabHandleStack = Stack<Set<String>>()

    protected abstract val rootUrl: String
    protected abstract val sessionCookieName: String
    protected abstract val sessionCookieDomain: String

    private val store: Store by inject()
    private var browserLaunched: Boolean = false
    private val driver: WebDriver by lazy {
        browserLaunched = true
        get()
    }

    ///// Functions that require an open webpage

    final override fun getPageInfo(searchUrl: String, propertiesRemoved: Int): PageInfo {
        ensurePageWithSession(searchUrl)
        return getPageInfo(driver, searchUrl)
    }

    protected abstract fun getPageInfo(driver: WebDriver, searchUrl: String): PageInfo

    final override fun fetchProperty(webId: String): Property {
        ensurePageWithSession(getUrlFromWebId(webId))
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
    }

    protected abstract fun markAsUnsuitable(driver: WebDriver, webId: String, unsuitable: Boolean, description: String)

    final override fun getPhotoUrls(webId: String): List<String> {
        ensurePageWithSession(getUrlFromWebId(webId))
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

    ///// Functions that require a browser instance but not an open webpage

    override fun openTabs(vararg urls: String): List<String> {
        driver.switchTo().window("")
        val oldWindowHandles = driver.windowHandles
        urls.mapNotNull { url ->
            (driver as RemoteWebDriver).executeScript("window.open('$url')")
        }
        return driver.windowHandles.filter { it !in oldWindowHandles }
    }

    override fun openHTML(html: String) {
        openNewTab()
        driver["data:text/html;charset=utf-8,${String(html.toByteArray())}"]
    }

    override fun cleanup() {
        if (browserLaunched) {
            driver.quit()
        }
    }

    /**
     * Tabs opened after calling this method can be al closed by calling a popTabHandles
     */
    override fun pinCurrentTabs() {
        runCatching {
            tabHandleStack.push(driver.windowHandles)
        }
    }

    override fun closeUnpinnedTabs() {
        val handles: Set<String> = if (tabHandleStack.isNotEmpty()) {
            tabHandleStack.pop()
        } else emptySet()
        runCatching {
            (driver.windowHandles - handles).forEach {
                runCatching {
                    driver.switchTo().window(it)
                    if (driver.windowHandles.size > 1) {
                        driver.close()
                    }
                }
            }
        }
    }

    override fun clearCookies() {
        driver.manage().deleteAllCookies()
    }

    override fun addOrUpdateCookies(cookies: List<Cookie>) {
        driver.manage().run {
            cookies.forEach(::addCookie)
        }
        driver[driver.currentUrl]
    }

    override fun saveCookies() {
        store.saveCookies(Cookies(driver.manage().cookies))
    }

    ///// Functions that are service dependent, but do not require a browser instance

    override fun isValidUrl(url: String): Boolean {
        return url.startsWith("$rootUrl/") && url.split("$rootUrl/").size == 2
    }

    override fun cleanUrl(url: String): String {
        return url
    }

    override fun parseUrlOrWebId(arg: String): String? {
        var cleanUrl = cleanUrl(arg)
        if (!isValidUrl(cleanUrl)) {
            val matcher = cleanUrl.matcher("^([\\d]+)$")
            if (matcher.find()) {
                cleanUrl = getUrlFromWebId(arg)
            }
        }
        return if (isValidUrl(cleanUrl)) {
            cleanUrl
        } else {
            null
        }
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
                openNewTab()
                driver[finalUrls[0]]
            }
        } else {
            // at least one expected url already open
        }

        beforeEnsureSession(driver)
        val refresh = ensureSession()
        afterEnsureSession(driver)

//        if (refresh) {
//            driver[finalUrls[0]]
//        }
    }

    private fun ensureSession(): Boolean {
        var needsRefresh = false
        var sessionAvailable = false
        val storedCookies: Set<Cookie>? = store.getCookies()?.cookies

        if (storedCookies != null) {
            runCatching {
                if (driver.manage().cookies.hasSession()) {
                    // browser already has valid session cookies, nothing to do
                    sessionAvailable = true
                } else {
                    // browser has no session cookies
                    if (storedCookies.hasSession()) {
                        // we have session cookies stored
                        driver.manage().deleteAllCookies()
                        storedCookies.forEach { driver.manage().addCookie(it) }
                        sessionAvailable = true
                        needsRefresh = true
                    }
                }
            }
        }

        if (sessionAvailable.not() && login(driver)) {
            Thread.sleep(500)
            saveCookies()
        }

        return needsRefresh
    }

    protected abstract fun login(driver: WebDriver): Boolean

    protected open fun beforeEnsureSession(driver: WebDriver) {
        // override in subclass
    }


    protected open fun afterEnsureSession(driver: WebDriver) {
        // override in subclass
    }

    private fun Set<Cookie>?.hasSession(): Boolean {
        return this
            ?.firstOrNull { it.name == sessionCookieName && it.domain == sessionCookieDomain }
            ?.let { it.expiry > Date() }
            ?: false
    }

    /**
     * The window handle of the new tab is the last item in driver.windowHandles
     */
    private fun openNewTab(retry: Boolean = true): String? {
        val oldWindowHandles = driver.windowHandles
        return try {
            (driver as RemoteWebDriver).executeScript("window.open()")
            // selenium doesn't tell us the handle of the newly opened tab
            val newWindowHandles = driver.windowHandles
            val newHandle = newWindowHandles.first { it !in oldWindowHandles }
            driver.switchTo().window(newHandle)
            newHandle
        } catch (e: NoSuchWindowException) { // user can close main tab any time, pick a new one as main
            if (retry) {
                driver.switchTo().window("")
                openNewTab(retry = false)
            } else {
                null
            }
        }
    }
}