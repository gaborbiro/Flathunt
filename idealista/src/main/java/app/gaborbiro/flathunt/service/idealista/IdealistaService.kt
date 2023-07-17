package app.gaborbiro.flathunt.service.idealista

import app.gaborbiro.flathunt.data.domain.Store
import app.gaborbiro.flathunt.data.domain.model.Property
import app.gaborbiro.flathunt.service.BaseService
import app.gaborbiro.flathunt.service.Page
import org.openqa.selenium.By
import org.openqa.selenium.Cookie
import org.openqa.selenium.WebDriver

class IdealistaService(private val store: Store) : BaseService(store) {

    override val rootUrl = "https://www.idealista.pt/en/"
    override val sessionCookieName = "SESSION"
    override val sessionCookieDomain = ".www.idealista.pt"

    companion object {
        private const val USERNAME = "gabor.biro@yahoo.com"
        private const val PASSWORD = "1qazse45rdxSW2"
    }

    override fun beforeSession(driver: WebDriver) {
        with(driver.manage()) {
            deleteAllCookies()
            addCookie(
                Cookie.Builder(
                    "datadome",
                    "6Nn6khyJkunzjlUhb2TGTuul6fB3QAfv8MBblFz6xDJ4Mf0eDylfJqJ3DtGRKU-GudD~6EBBEH2MVi8nF_HGry1tCccX-YBxmCVS2yberuVPJ0GPtahQKGJTFLUXZpQI"
                ).build()
            )
        }
        driver[rootUrl]
    }

    override fun login(driver: WebDriver) {
        driver.findElement(By.className("icon-user-no-logged")).click()
        driver.findElement(By.className("js-email-field")).sendKeys(USERNAME)
        driver.findElement(By.className("js-password-field")).sendKeys(PASSWORD)
        driver.findElement(By.id("doLogin")).click()
    }

    override fun fetchLinksFromSearch(driver: WebDriver, searchUrl: String, propertiesRemoved: Int): Page {
        ensurePageWithSession(searchUrl)
        return Page(
            urls = emptyList(),
            page = 0,
            pageCount = 0,
            propertiesRemoved = 0,
            nextPage = {
                null
            }
        )
    }

    override fun fetchProperty(driver: WebDriver, id: String): Property {
        ensurePageWithSession(getUrlFromId(id))
        return Property()
    }

    override fun markAsUnsuitable(driver: WebDriver, id: String, index: Int?, unsuitable: Boolean) {
        val blacklist = store.getBlacklist().toMutableList().also {
            it.add(id)
        }
        store.saveBlacklist(blacklist)
    }

    override fun getPropertyIdFromUrl(url: String): String {
        return if (isValidUrl(url)) {
            ""
        } else {
            throw IllegalArgumentException("Unable to get property id from $url (invalid url)")
        }
    }

    override fun getUrlFromId(id: String): String {
        return ""
    }

    override fun isValidUrl(url: String): Boolean {
        return url.startsWith("$rootUrl/") && url.split("$rootUrl/").size == 2
    }

    override fun cleanUrl(url: String): String {
        return url
    }

    override fun checkUrlOrId(arg: String): String? {
        var cleanUrl = cleanUrl(arg)
        return null
    }

    override fun getPhotoUrls(driver: WebDriver, id: String): List<String> {
        ensurePageWithSession(getUrlFromId(id))
        return emptyList()
    }
}