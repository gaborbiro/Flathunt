package app.gaborbiro.flathunt.service

import app.gaborbiro.flathunt.data.domain.model.CookieSet
import app.gaborbiro.flathunt.service.domain.Browser
import org.koin.core.annotation.Singleton
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.openqa.selenium.Cookie
import org.openqa.selenium.NoSuchWindowException
import org.openqa.selenium.WebDriver
import org.openqa.selenium.remote.RemoteWebDriver
import java.util.*

@Singleton
class BrowserImpl : Browser, KoinComponent {

    private val tabHandleStack = Stack<Set<String>>()

    private var browserLaunched: Boolean = false

    private val driver: WebDriver by lazy {
        browserLaunched = true
        get()
    }

    fun ensureSession(
        sessionCookieName: String,
        sessionCookieDomain: String,
        cookieSet: CookieSet?
    ): Pair<Boolean, Boolean> {
        var needsRefresh = false
        var sessionAvailable = false
        val cookies = cookieSet?.cookies
        if (cookies != null) {
            runCatching {
                if (driver.manage().cookies.hasSession(sessionCookieName, sessionCookieDomain)) {
                    // browser already has valid session cookies, nothing to do
                    sessionAvailable = true
                } else {
                    // browser has no session cookies
                    if (cookies.hasSession(sessionCookieName, sessionCookieDomain)) {
                        // we have session cookies stored
                        val options = driver.manage()
                        options.deleteAllCookies()
                        cookies.forEach { options.addCookie(it) }
                        sessionAvailable = true
                        needsRefresh = true
                    }
                }
            }
        }

        return sessionAvailable to needsRefresh
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

    override fun cleanup() {
        if (browserLaunched) {
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

    override fun initialised(): Boolean {
        return browserLaunched
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

    private fun Set<Cookie>?.hasSession(sessionCookieName: String, sessionCookieDomain: String): Boolean {
        return this
            ?.firstOrNull { it.name == sessionCookieName && it.domain == sessionCookieDomain }
            ?.let { it.expiry == null || it.expiry > Date() }
            ?: false
    }
}