package app.gaborbiro.flathunt.service.idealista

import app.gaborbiro.flathunt.LatLon
import app.gaborbiro.flathunt.compileTimeConstant.Constants
import app.gaborbiro.flathunt.data.domain.Store
import app.gaborbiro.flathunt.data.domain.model.Price
import app.gaborbiro.flathunt.data.domain.model.Property
import app.gaborbiro.flathunt.service.BaseService
import app.gaborbiro.flathunt.service.domain.Service
import app.gaborbiro.flathunt.service.domain.model.Page
import org.koin.core.annotation.Named
import org.koin.core.annotation.Singleton
import org.koin.core.component.inject
import org.openqa.selenium.By
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.WebDriver
import org.openqa.selenium.remote.RemoteWebElement
import java.net.URI
import java.text.DecimalFormat
import java.util.regex.Pattern

@Singleton(binds = [Service::class])
@Named(Constants.idealista)
class IdealistaService : BaseService() {

    override val rootUrl = "https://www.idealista.pt/en"
    override val sessionCookieName = "cc"
    override val sessionCookieDomain = "www.idealista.pt"

    private val store: Store by inject()

    companion object {
        private const val USERNAME = "gabor.biro@yahoo.com"
        private const val PASSWORD = "1qazse45rdxSW2"
    }

    override fun login(driver: WebDriver) {
        driver.findElement(By.className("icon-user-no-logged")).click()
        driver.findElement(By.className("js-email-field")).sendKeys(USERNAME)
        driver.findElement(By.className("js-password-field")).sendKeys(PASSWORD)
        driver.findElement(By.id("doLogin")).click()
    }

    override fun fetchLinksFromSearch(driver: WebDriver, searchUrl: String, propertiesRemoved: Int): Page {
        ensurePageWithSession(searchUrl)
        val pagerRegex = Pattern.compile("pagina-([\\d]+)")
        val matcher = pagerRegex.matcher(searchUrl)
        val page = if (matcher.find()) {
            matcher.group(1).toInt()
        } else {
            1
        }
        val urls = driver.findElements(By.className("item-link")).map { it.getAttribute("href") }
        var pageCount = if(urls.isNotEmpty()) 1 else 0
        do {
            val pagination = driver.findElements(By.className("pagination"))
            if (pagination.isEmpty()) {
                break
            }
            val pagingButtons = pagination[0].findElements(By.tagName("li"))
            val lastVisiblePageBtn = pagingButtons.last { (it as RemoteWebElement).getAttribute("class") != "next" }
            val r = lastVisiblePageBtn.findElements(By.tagName("a"))
            if (r.isNotEmpty()) {
                val maxPage = (lastVisiblePageBtn.findElement(By.tagName("a")) as RemoteWebElement).text.toInt()
                if (maxPage > pageCount) {
                    pageCount = maxPage
                    lastVisiblePageBtn.click()
                } else {
                    break
                }
            } else {
                break
            }
        } while (true)

        return Page(
            urls = urls,
            page = page,
            pageCount = pageCount,
            nextPage = {
                val uri = URI.create(searchUrl)
                val pathTokens = uri.path.split("/").filter { it.isNotBlank() }
                if (pathTokens.last().contains("pagina-")) {
                    searchUrl.replace(Regex("pagina-([\\d]+)"), "pagina-${this.page + 1}")
                } else {
                    searchUrl.replace("?", "pagina-${this.page + 1}?")
                }
            }
        )
    }

    override fun fetchProperty(driver: WebDriver, id: String): Property {
        val priceStr = driver.findElements(By.className("info-data-price"))[0].text
        val priceRegex = Pattern.compile("([\\d,\\.]+)\\s€/month")
        val matcher = priceRegex.matcher(priceStr)
        if (!matcher.find()) throw IllegalArgumentException("Cannot parse price: $priceStr")
        val price = DecimalFormat("#,###").parse(matcher.group(1)).toInt()
        val features = driver.findElements(By.className("details-property_features"))
            .map { it.findElements(By.tagName("li")).map { it.text } }.flatten().distinct()
        var equipped = false
        if (features.any { it.contains("unfurnished", ignoreCase = true) }.not()) {
            if (features.any { it.contains("equipped kitchen", ignoreCase = true) }) {
                equipped = true
            }
        }
        val heating = features.any { it.contains("heating", ignoreCase = true) }
        val airConditioning = features.any { it.contains("Air conditioning", ignoreCase = true) }
        val mapURL = (driver as JavascriptExecutor)
            .executeScript("return config['multimediaCarrousel']['map']['src'];") as String
        val center: List<String> = URI.create(mapURL).query.split("&")
            .filter { it.isNotBlank() }
            .map { it.split("=") }.associateBy { it[0] }
            .get("center")!!
            .get(1)
            .split(",")
        val location = LatLon(center[0], center[1])
        return Property(
            id = id,
            title = driver.findElement(By.className("main-info__title-main")).text,
            prices = arrayOf(Price(priceStr, priceStr, price)),
            furnished = equipped,
            heating = heating,
            airConditioning = airConditioning,
            location = location,
        )
    }

    override fun markAsUnsuitable(driver: WebDriver, id: String, unsuitable: Boolean, description: String) {
        ensurePageWithSession(getUrlFromId(id))
        runCatching {
            if (unsuitable) {
                driver.findElement(By.className("icon-delete")).click()
            } else {
                driver.findElement(By.className("icon-recover")).click()
            }
        }
    }

    override fun getPropertyIdFromUrl(url: String): String {
        return if (isValidUrl(url)) {
            URI.create(url).path.split("/").filter { it.isNotBlank() }.last()
        } else {
            throw IllegalArgumentException("Unable to get property id from $url (invalid url)")
        }
    }

    override fun getUrlFromId(id: String): String {
        return "${rootUrl}/imovel/$id/"
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