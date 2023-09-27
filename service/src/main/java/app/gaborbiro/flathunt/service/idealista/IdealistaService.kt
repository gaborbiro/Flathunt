package app.gaborbiro.flathunt.service.idealista

import app.gaborbiro.flathunt.LatLon
import app.gaborbiro.flathunt.compileTimeConstant.Constants
import app.gaborbiro.flathunt.data.domain.model.Price
import app.gaborbiro.flathunt.data.domain.model.Property
import app.gaborbiro.flathunt.service.BaseService
import app.gaborbiro.flathunt.service.domain.Service
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

@Singleton(binds = [Service::class])
@Named(Constants.idealista)
class IdealistaService : BaseService() {

    override val rootUrl = "https://www.idealista.pt/en"
    override val sessionCookieName = "cc"
    override val sessionCookieDomain = "www.idealista.pt"

    companion object {
        private const val USERNAME = "gabor.biro@yahoo.com"
        private const val PASSWORD = "1qazse45rdxSW2"
    }

    ///// Functions that require an open webpage

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
            propertyWebIds = urls.map { getPropertyIdFromUrl(it) },
            page = page,
            pageCount = pageCount,
        )
    }

    override fun fetchProperty(driver: WebDriver, webId: String): Property {
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
            webId = webId,
            title = driver.findElement(By.className("main-info__title-main")).text,
            prices = arrayOf(Price(priceStr, priceStr, price)),
            furnished = equipped,
            heating = heating,
            airConditioning = airConditioning,
            location = location,
        )
    }

    override fun markAsUnsuitable(driver: WebDriver, webId: String, unsuitable: Boolean, description: String) {
        ensurePageWithSession(getUrlFromWebId(webId))

    }

    override fun getPhotoUrls(driver: WebDriver, webId: String): List<String> {
        return driver.findElements(By.className("detail-image-gallery")).map { it.getAttribute("data-ondemand-img") }
    }

    override fun getNextPageUrl(page: PageInfo, markedAsUnsuitableCount: Int): String? {
        return if (page.page < page.pageCount) {
            val uri = URI.create(page.pageUrl)
            val pathTokens = uri.path.split("/").filter { it.isNotBlank() }
            if (pathTokens.last().contains("pagina-")) {
                page.pageUrl.replace(Regex("pagina-([\\d]+)"), "pagina-${page.page + 1}")
            } else {
                page.pageUrl.replace("?", "pagina-${page.page + 1}?")
            }
        } else {
            null
        }
    }

    ///// Functions that are service dependent, but do not require a browser instance

    override fun getPropertyIdFromUrl(url: String): String {
        return if (isValidUrl(url)) {
            URI.create(url).path.split("/").last { it.isNotBlank() }
        } else {
            throw IllegalArgumentException("Unable to get property id from $url (invalid url)")
        }
    }

    override fun getUrlFromWebId(webId: String): String {
        return "${rootUrl}/imovel/$webId/"
    }

    override fun login(driver: WebDriver): Boolean {
        return false
    }
}