package app.gaborbiro.flathunt.service.idealista

import app.gaborbiro.flathunt.data.Store
import app.gaborbiro.flathunt.data.model.Cookies
import app.gaborbiro.flathunt.data.model.Property
import app.gaborbiro.flathunt.service.BaseService
import app.gaborbiro.flathunt.service.Page
import org.openqa.selenium.By
import org.openqa.selenium.Cookie

class IdealistaService(private val store: Store) : BaseService(store) {

    override val sessionCookieNames = arrayOf("SESSION")
    override val sessionCookieDomain = ".www.idealista.pt"

    override fun login() {
        ensureBrowser()
        driver[RM_ROOT_URL]
        driver.manage().deleteAllCookies()
        with(driver.manage()) {
            addCookie(
                Cookie.Builder(
                    "datadome",
                    "1l1UMy2QthQJoq4zBvoYrRN6zxUU9hzE3sLFLt5CpGuKFL-67IRYJbYtq6H31Vc1FpYYRR~fnb1vlrmZfmmNCQZ7cB9bauuL8yx3hTcDHso359zU-qHFvp4xXBWx71pX"
                ).build()
            )
        }
        driver[RM_ROOT_URL]
        driver.findElement(By.className("icon-user-no-logged")).click()
        driver.findElement(By.className("js-email-field")).sendKeys(USERNAME)
        driver.findElement(By.className("js-password-field")).sendKeys(PASSWORD)
        driver.findElement(By.id("doLogin")).click()

        store.saveCookies(Cookies(driver.manage().cookies))
    }

    override fun fetchLinksFromSearch(searchUrl: String, propertiesRemoved: Int): Page {
        ensureTab(searchUrl)
        return Page(
            urls = emptyList(),
            page = 0,
            pageCount = 0,
            propertiesRemoved = 0,
            nextPage = {
                this
            }
        )
    }

    override fun fetchProperty(id: String): Property {
        ensureTab(getUrlFromId(id))
        with(driver) {
            return Property()
        }
    }

    override fun markAsUnsuitable(id: String, index: Int?, unsuitable: Boolean) {
        super.markAsUnsuitable(id, index, unsuitable)

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
        return url.startsWith("$RM_ROOT_URL/") && url.split("$RM_ROOT_URL/").size == 2
    }

    override fun cleanUrl(url: String): String {
        return url
    }

    override fun checkUrlOrId(arg: String): String? {
        var cleanUrl = cleanUrl(arg)
        return null
    }

    override fun getPhotoUrls(id: String): List<String> {
        ensureTab(getUrlFromId(id))
        return emptyList()
    }
}