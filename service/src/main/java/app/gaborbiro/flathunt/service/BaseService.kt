package app.gaborbiro.flathunt.service

import app.gaborbiro.flathunt.AVG_WEEKS_IN_MONTH
import app.gaborbiro.flathunt.data.Store
import app.gaborbiro.flathunt.data.model.Price
import app.gaborbiro.flathunt.data.model.Property
import app.gaborbiro.flathunt.matcher
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

    abstract val sessionCookieNames: Array<String>
    abstract val sessionCookieDomain: String

    protected open fun beforeSession() {

    }

    protected open fun afterSession() {

    }

    abstract override fun login()

    protected fun ensureBrowser() {
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

    override fun markAsUnsuitable(id: String, index: Int?, unsuitable: Boolean) {
        val blacklist = store.getBlacklist().toMutableList().also {
            it.add(id)
        }
        store.saveBlacklist(blacklist)
    }

    override fun fetchProperty(id: String, newTab: Boolean): Property {
        if (newTab && ::driver.isInitialized) {
            openNewTab()
            driver[getUrlFromId(id)]
        } else {
            ensureTab(getUrlFromId(id))
        }
        return fetchProperty(id)
    }

    abstract fun fetchProperty(id: String): Property

    override fun cleanup() {
        if (::driver.isInitialized) {
            driver.quit()
        }
    }

    fun ensureTab(vararg defaultUrls: String) {
        ensureBrowser()
        runCatching { driver.currentUrl }.exceptionOrNull()?.let {
            driver.switchTo().window("")
        }
        if (defaultUrls.isNotEmpty() && defaultUrls.all { url -> !driver.currentUrl.startsWith("$url/") }) {
            try {
                driver[defaultUrls[0]]
            } catch (e: NoSuchWindowException) {
                openNewTab()
            }
        }
        ensureSession(defaultUrls[0])
    }

    fun perWeekToPerMonth(price: String): Price {
        val pricePerMonth = if (price.contains("pw")) {
            val matcher = price.matcher("([^\\d])([\\d\\.,]+)[\\s]*pw")
            if (matcher.find()) {
                val perMonth: Double = matcher.group(2).replace(",", "").toFloat() * AVG_WEEKS_IN_MONTH
                "${matcher.group(1)}${perMonth.toInt()} pcm"
            } else {
                price
            }
        } else price
        val matcher = pricePerMonth.matcher("([^\\d])([\\d\\.,]+)[\\s]*pcm")
        return if (matcher.find()) {
            Price(price, pricePerMonth, matcher.group(2).replace(",", "").toInt())
        } else {
            println("Error parsing price $price")
            Price(price, pricePerMonth, -1)
        }
    }

    private fun ensureSession(url: String) {
        beforeSession()
        store.getCookies()?.let { cookies ->
            runCatching {
                if (driver.manage().cookies.find { it.name in sessionCookieNames && it.domain == sessionCookieDomain } == null) {
                    if (cookies.cookies.find { it.name in sessionCookieNames && it.domain == sessionCookieDomain } != null) {
                        driver.manage().deleteAllCookies()
                        cookies.cookies.forEach { driver.manage().addCookie(it) }
                        try {
                            driver[url]
                        } catch (e: NoSuchWindowException) {
                            openNewTab()
                        }
                        afterSession()
                    } else {
                        login()
                    }
                }
            }
        } ?: run {
            login()
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