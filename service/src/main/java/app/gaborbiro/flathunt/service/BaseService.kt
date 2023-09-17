package app.gaborbiro.flathunt.service

import app.gaborbiro.flathunt.data.domain.Store
import app.gaborbiro.flathunt.data.domain.model.Cookies
import app.gaborbiro.flathunt.data.domain.model.Message
import app.gaborbiro.flathunt.data.domain.model.Property
import app.gaborbiro.flathunt.repo.domain.model.MessageTag
import app.gaborbiro.flathunt.service.domain.Service
import app.gaborbiro.flathunt.service.domain.model.Page
import org.koin.core.component.KoinComponent
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

    private lateinit var driver: WebDriver
    private val tabHandleStack = Stack<Set<String>>()

    protected abstract val rootUrl: String
    protected abstract val sessionCookieName: String
    protected abstract val sessionCookieDomain: String

    private val store: Store by inject<Store>()

    protected open fun beforeSession(driver: WebDriver) {

    }

    protected open fun afterSession(driver: WebDriver) {

    }

    final override fun login() {
        login(driver)
    }

    protected abstract fun login(driver: WebDriver)

    final override fun fetchLinksFromSearch(searchUrl: String, propertiesRemoved: Int): Page {
        ensureBrowser()
        return fetchLinksFromSearch(driver, searchUrl, propertiesRemoved)
    }

    protected abstract fun fetchLinksFromSearch(driver: WebDriver, searchUrl: String, propertiesRemoved: Int): Page

    override fun openTabs(vararg urls: String): List<String> {
        ensureBrowser()
        val oldWindowHandles = driver.windowHandles
        urls.mapNotNull { url ->
            (driver as RemoteWebDriver).executeScript("window.open('$url')")
        }
        return driver.windowHandles.filter { it !in oldWindowHandles }
    }

    /**
     * Tabs opened after calling this method can be al closed by calling a popTabHandles
     */
    override fun pinCurrentTabs() {
        runCatching {
            tabHandleStack.push(driver.windowHandles)
        }
    }

    final override fun getPhotoUrls(id: String): List<String> {
        ensureBrowser()
        return getPhotoUrls(driver, id)
    }

    protected abstract fun getPhotoUrls(driver: WebDriver, id: String): List<String>

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

    final override fun fetchProperty(id: String, newTab: Boolean): Property {
        if (newTab && ::driver.isInitialized) {
            openNewTab()
            driver[getUrlFromId(id)]
        } else {
            ensureBrowser()
            ensurePageWithSession(getUrlFromId(id))
        }
        return fetchProperty(driver, id)
    }

    protected abstract fun fetchProperty(driver: WebDriver, id: String): Property

    final override fun markAsUnsuitable(id: String, unsuitable: Boolean, description: String) {
        val blacklist = store.getBlacklist().toMutableList().also {
            it.add(id)
        }
        store.saveBlacklist(blacklist)
        ensureBrowser()
        markAsUnsuitable(driver, id, unsuitable, description)
    }

    protected abstract fun markAsUnsuitable(driver: WebDriver, id: String, unsuitable: Boolean, description: String)

    override fun cleanup() {
        if (::driver.isInitialized) {
            driver.quit()
        }
    }

    fun ensurePageWithSession(vararg expectedUrls: String) {
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
            // expected url already open
        }
        ensureSession {
            login()
            Thread.sleep(500)
            store.saveCookies(Cookies(driver.manage().cookies))
        }
        driver[finalUrls[0]]
    }

    private fun ensureSession(onSessionUnavailable: () -> Unit) {
        beforeSession(driver)
        val storedCookies = store.getCookies()?.cookies
        val browserCookies = driver.manage().cookies
        var sessionAvailable = false
        if (storedCookies != null) {
            runCatching {
                if (browserCookies
                        .firstOrNull { it.name == sessionCookieName && it.domain == sessionCookieDomain }
                        ?.expiry
                        ?.let { it < Date() } == true
                ) {
                    // browser already has session cookies, nothing to do
                    sessionAvailable = true
                } else {
                    // browser has no session cookies

                    if (storedCookies.any { it.name == sessionCookieName && it.domain == sessionCookieDomain }) {
                        // we have session cookies stored
                        driver.manage().deleteAllCookies()
                        storedCookies.forEach { driver.manage().addCookie(it) }
                        sessionAvailable = true
                    }
                }
            }
        }
        if (!sessionAvailable) {
            onSessionUnavailable()
        }
        afterSession(driver)
    }

    private fun ensureBrowser() {
        if (!::driver.isInitialized) {
            System.setProperty("webdriver.chrome.driver", Paths.get("chromedriver.exe").toString())
            driver = ChromeDriver(
                ChromeOptions().apply {
                    // https://peter.sh/experiments/chromium-command-line-switches/
                    // start-maximized
                    // window-position=0,0", "window-size=1,1
                    addArguments("start-maximized")
                    setCapability(CapabilityType.UNHANDLED_PROMPT_BEHAVIOUR, UnexpectedAlertBehaviour.DISMISS)
                }
            )
        }
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

    final override fun fetchMessages(safeMode: Boolean): List<Message> {
        ensureBrowser()
        return fetchMessages(driver, safeMode)
    }

    protected open fun fetchMessages(driver: WebDriver, safeMode: Boolean): List<Message> {
        throw NotImplementedError()
    }

    final override fun tagMessage(messageUrl: String, vararg tags: MessageTag) {
        ensureBrowser()
        tagMessage(driver, messageUrl, *tags)
    }

    protected open fun tagMessage(driver: WebDriver, messageUrl: String, vararg tags: MessageTag) {
        throw NotImplementedError()
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
}