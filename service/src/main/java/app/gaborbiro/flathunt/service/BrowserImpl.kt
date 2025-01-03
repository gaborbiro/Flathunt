package app.gaborbiro.flathunt.service

import app.gaborbiro.flathunt.console.ConsoleWriter
import app.gaborbiro.flathunt.data.domain.Store
import app.gaborbiro.flathunt.data.domain.model.CookieSet
import app.gaborbiro.flathunt.service.domain.Browser
import org.koin.core.annotation.Singleton
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.component.inject
import org.openqa.selenium.*
import org.openqa.selenium.remote.RemoteWebDriver
import java.util.*

@Singleton
class BrowserImpl : Browser, KoinComponent, JavascriptExecutor {

    private val console: ConsoleWriter by inject()
    private val store: Store by inject()

    private val tabHandleStack = Stack<Set<String>>()

    private var driverInitialized = false

    private val driver: WebDriver by lazy {
        driverInitialized = true
        get()
    }

    fun ensureImportantCookies(
        overrideCookies: List<Pair<String, String>>,
        importantCookies: List<Pair<String, String>>,
        storedCookies: CookieSet?,
        log: Boolean,
    ): Pair<Boolean, Boolean> {
        var needsRefresh = false
        var importantCookiesAvailable = false
        val cookies = storedCookies?.cookies
        if (cookies != null) {
            runCatching {
                val options = driver.manage()
                val browserCookies = options.cookies

                if (overrideCookies.isNotEmpty()) {
                    if (log) {
                        console.i("Overriding ${overrideCookies.size} cookies. If page doesn't load, override following cookies manually: ${overrideCookies.joinToString { it.first + "/" + it.second }}")
                    }
                }
                overrideCookies.forEach { (name, _) ->
                    cookies.firstOrNull { it.name == name }
                        ?.let {
                            options.addCookie(it)
                            needsRefresh = true
                        }
                }

                if (importantCookies.all { (name, domain) -> browserCookies.containsCookie(name, domain) }) {
                    // browser already has valid session cookies, nothing to do
                    importantCookiesAvailable = true
                    if (log && importantCookies.isNotEmpty()) {
                        console.i("${importantCookies.size} important cookies already present")
                    }
                } else {
                    if (log && importantCookies.isNotEmpty()) {
                        console.i("Setting ${importantCookies.size} cookies")
                    }
                    // browser is missing important cookies
                    if (importantCookies.all { (name, domain) -> cookies.containsCookie(name, domain) }) {
                        // we have session cookies stored
                        options.deleteAllCookies()
                        cookies.forEach { options.addCookie(it) }
                        importantCookiesAvailable = true
                        needsRefresh = true
                    }
                }
            }
        }

        return importantCookiesAvailable to needsRefresh
    }

    override fun openTabs(urls: List<String>): List<String> {
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

    override fun savePositionAndSize() {
        store.setWindowPosition(driver.manage().window().position)
        store.setWindowSize(driver.manage().window().size)
    }

    override fun cleanup() {
        if (driverInitialized) {
            driver.quit()
        }
    }

    /**
     * Tabs opened after calling this method can be al closed by calling a popTabHandles
     */
    override fun pinTabs() {
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

    override fun addOrUpdateCookies(cookies: CookieSet) {
        driver.switchTo().window("")
        driver.manage().run {
            cookies.cookies.forEach(::addCookie)
        }
        driver[driver.currentUrl]
    }

    override fun getCookies(): CookieSet {
        return CookieSet(driver.manage().cookies)
    }

    /**
     * The window handle of the new tab is the last item in driver.windowHandles
     */
    override fun openNewTab(retry: Boolean): String? {
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

    private fun Set<Cookie>?.containsCookie(name: String, domain: String): Boolean {
        return this
            ?.firstOrNull { it.name == name && it.domain == domain }
            ?.let { it.expiry == null || it.expiry > Date() }
            ?: false
    }

    override fun findElements(by: By?): MutableList<WebElement> {
        return driver.findElements(by)
    }

    override fun findElement(by: By?): WebElement {
        return driver.findElement(by)
    }

    override fun get(url: String?) {
        driver.get(url)
    }

    override fun getCurrentUrl(): String {
        return driver.currentUrl
    }

    override fun getTitle(): String {
        return driver.title
    }

    override fun getPageSource(): String {
        return driver.pageSource
    }

    override fun close() {
        driver.close()
    }

    override fun quit() {
        driver.quit()
    }

    override fun getWindowHandles(): MutableSet<String> {
        return driver.windowHandles
    }

    override fun getWindowHandle(): String {
        return driver.windowHandle
    }

    override fun switchTo(): WebDriver.TargetLocator {
        return driver.switchTo()
    }

    override fun navigate(): WebDriver.Navigation {
        return driver.navigate()
    }

    override fun manage(): WebDriver.Options {
        return driver.manage()
    }

    override fun executeScript(script: String?, vararg args: Any?): Any {
        return (driver as JavascriptExecutor).executeScript(script, *args)
    }

    override fun executeAsyncScript(script: String?, vararg args: Any?): Any {
        return (driver as JavascriptExecutor).executeAsyncScript(script, *args)
    }
}