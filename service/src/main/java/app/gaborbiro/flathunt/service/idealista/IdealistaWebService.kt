package app.gaborbiro.flathunt.service.idealista

import app.gaborbiro.flathunt.compileTimeConstant.Constants
import app.gaborbiro.flathunt.data.domain.model.Price
import app.gaborbiro.flathunt.data.domain.model.Property
import app.gaborbiro.flathunt.data.domain.model.PropertyLatLon
import app.gaborbiro.flathunt.or
import app.gaborbiro.flathunt.service.BaseWebService
import app.gaborbiro.flathunt.service.domain.WebService
import app.gaborbiro.flathunt.service.domain.model.PageInfo
import org.koin.core.annotation.Named
import org.koin.core.annotation.Singleton
import org.openqa.selenium.By
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.WebDriver
import org.openqa.selenium.remote.RemoteWebElement
import java.net.URI
import java.text.DecimalFormat
import java.util.regex.Pattern

@Singleton(binds = [WebService::class])
@Named(Constants.idealista + "_web")
class IdealistaWebService : BaseWebService() {

    override val rootUrl = "https://www.idealista.pt/en"
    override val overrideCookies = listOf("datadome" to ".idealista.pt")
    override val importantCookies = listOf("datadome" to ".idealista.pt")

    companion object {
        private const val USERNAME = "gabor.biro@yahoo.com"
        private const val PASSWORD = "1qazse45rdxSW2"
    }

    override fun afterEnsureSession(driver: WebDriver) {
        driver.findElements(By.id("didomi-notice-agree-button")).firstOrNull()?.click()
    }

    override fun getPageInfo(driver: WebDriver, searchUrl: String): PageInfo {
        val pagerRegex = Pattern.compile("pagina-([\\d]+)")
        val matcher = pagerRegex.matcher(searchUrl)
        val page = if (matcher.find()) {
            matcher.group(1).toInt()
        } else {
            1
        }
        val urls = driver.findElements(By.className("item-link")).map { it.getAttribute("href") }
        var pageCount = if (urls.isNotEmpty()) 1 else 0
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

        return PageInfo(
            pageUrl = searchUrl,
            propertyWebIds = urls.map { utilsService.getPropertyIdFromUrl(it) },
            page = page,
            pageCount = pageCount,
        )
    }

    override fun fetchProperty(driver: WebDriver, webId: String): Property {
        val priceStr = driver.findElements(By.className("info-data-price"))[0].text
        val priceRegex = Pattern.compile("([\\d,\\.]+)\\s€/m")
        val matcher = priceRegex.matcher(priceStr)
        if (matcher.find().not()) throw IllegalArgumentException("Cannot parse price: $priceStr")
        val price = DecimalFormat("#,###").parse(matcher.group(1)).toInt()

        val features = driver.findElements(By.className("details-property_features"))
            .map { it.findElements(By.tagName("li")).map { it.text } }.flatten().distinct()
        var equipped = false
        if (features.any { it.contains("unfurnished", ignoreCase = true) }.not()) {
            if (features.any { it.contains("equipped kitchen", ignoreCase = true) }) {
                equipped = true
            }
        }
        val tRegex = Regex("T[\\d]+")
        val rooms = features.firstOrNull { it.matches(tRegex) }?.let {
            it.substring(1).toInt()
        }

        val heating = features.any { it.contains("heating", ignoreCase = true) }
        val airConditioning = features.any { it.contains("Air conditioning", ignoreCase = true) }
        val energyCertificationElement =
            driver.findElements(By.xpath("//*[contains(text(), \"Energy performance certificate\")]/following-sibling::*/ul/li/*[last()]"))
                .lastOrNull()
        val energyCertification = energyCertificationElement
            ?.let { it.getAttribute("title").or() ?: it.text }
            ?: "Unknown"
        val mapURL = (driver as JavascriptExecutor)
            .executeScript("return config['multimediaCarrousel']['map']['src'];") as String
        val center: List<String> = URI.create(mapURL).query.split("&")
            .filter { it.isNotBlank() }
            .map { it.split("=") }.associateBy { it[0] }
            .get("center")!!
            .get(1)
            .split(",")
        val location = PropertyLatLon(center[0], center[1])
        return Property(
            webId = webId,
            totalRooms = rooms,
            title = driver.findElement(By.className("main-info__title-main")).text,
            prices = arrayOf(Price(priceStr, priceStr, price)),
            furnished = equipped,
            heating = heating,
            airConditioning = airConditioning,
            location = location,
            energyCertification = energyCertification,
        )
    }

    override fun updateSuitability(driver: WebDriver, webId: String, suitable: Boolean, description: String) {
        ensurePageWithSession(utilsService.getUrlFromWebId(webId))
        driver.findElements(By.className("discard-btn"))[1].click()
        if (suitable) {
            assert(driver.findElements(By.className("icon-delete")).isNotEmpty())
        } else {
            assert(driver.findElements(By.className("icon-recover")).isNotEmpty())
        }
    }

    override fun getPhotoUrls(driver: WebDriver, webId: String): List<String> {
        return driver.findElements(By.tagName("img")).mapNotNull { it.getAttribute("src") }
    }

    override fun login(driver: WebDriver): Boolean {
        driver.findElements(By.id("didomi-notice-agree-button")).firstOrNull()?.click()
        if (driver.findElements(By.className("icon-user-no-logged-text")).isNotEmpty()) {
            driver.findElement(By.className("icon-user-no-logged-text")).click()
            driver.findElement(By.className("js-email-field")).click()
            driver.findElement(By.className("js-email-field")).sendKeys(USERNAME)
            driver.findElement(By.className("js-password-field")).click()
            driver.findElement(By.className("js-password-field")).sendKeys(PASSWORD)
            driver.findElement(By.id("doLogin")).click()
            return true
        }
        return false
    }
}