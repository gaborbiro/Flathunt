package app.gaborbiro.flathunt.service

import app.gaborbiro.flathunt.data.Store
import app.gaborbiro.flathunt.data.model.Cookies
import app.gaborbiro.flathunt.data.model.Property
import org.openqa.selenium.NoSuchWindowException
import org.openqa.selenium.UnexpectedAlertBehaviour
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.remote.CapabilityType
import org.openqa.selenium.remote.RemoteWebDriver
import java.nio.file.Paths
import java.util.*

abstract class BaseService(private val store: Store) : Service {

    protected lateinit var driver: WebDriver
    private val tabHandleStack = Stack<Set<String>>()

    abstract val rootUrl: String
    abstract val sessionCookieName: String
    abstract val sessionCookieDomain: String

    protected open fun beforeSession() {

    }

    protected open fun afterSession() {

    }

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
    override fun pushTabHandles() {
        runCatching {
            tabHandleStack.push(driver.windowHandles)
        }
    }

    override fun popTabHandles() {
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

//    override fun fetchLinksFromSearch(searchUrl: String, propertiesRemoved: Int): Page {
//        ensureTab(searchUrl)
//        val page: Int = getPageFromUrl(searchUrl)
//        val pageCount = driver.getPageCount()
//    }
//
//    abstract fun getPageFromUrl(url: String): Int
//
//    abstract fun WebDriver.getPageCount(): Int

    override fun fetchProperty(id: String, newTab: Boolean): Property {
        if (newTab && ::driver.isInitialized) {
            openNewTab()
            driver[getUrlFromId(id)]
        } else {
            ensurePageWithSession(getUrlFromId(id))
        }
        return fetchProperty(id)
    }

    abstract fun fetchProperty(id: String): Property

    override fun cleanup() {
        if (::driver.isInitialized) {
            driver.quit()
        }
    }

    fun ensurePageWithSession(vararg expectedUrls: String) {
        ensureBrowser()
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
            Thread.sleep(300)
            store.saveCookies(Cookies(driver.manage().cookies))
            driver[finalUrls[0]]
        }
    }

    private fun ensureSession(onSessionUnavailable: () -> Unit) {
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
                    beforeSession()
                    if (storedCookies.any { it.name == sessionCookieName && it.domain == sessionCookieDomain }) {
                        // we have session cookies stored
                        driver.manage().deleteAllCookies()
                        storedCookies.forEach { driver.manage().addCookie(it) }
                        afterSession()
                        sessionAvailable = true
                    }
                }
            }
        }
        if (!sessionAvailable) {
            onSessionUnavailable()
        }
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
}