package app.gaborbiro.flathunt.service.rightmove

import app.gaborbiro.flathunt.GlobalVariables
import app.gaborbiro.flathunt.compileTimeConstant.Constants
import app.gaborbiro.flathunt.console.ConsoleWriter
import app.gaborbiro.flathunt.data.domain.Store
import app.gaborbiro.flathunt.data.domain.model.Price
import app.gaborbiro.flathunt.data.domain.model.Property
import app.gaborbiro.flathunt.data.domain.model.PropertyLatLon
import app.gaborbiro.flathunt.request.RequestCaller
import app.gaborbiro.flathunt.service.BaseWebService
import app.gaborbiro.flathunt.service.PriceParseResult
import app.gaborbiro.flathunt.service.domain.model.PageInfo
import app.gaborbiro.flathunt.service.ensurePriceIsPerMonth
import app.gaborbiro.flathunt.splitQuery
import org.koin.core.annotation.Named
import org.koin.core.annotation.Singleton
import org.koin.core.component.inject
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.remote.RemoteWebElement
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.ceil

@Singleton
@Named(Constants.rightmove + "_web")
class RightmoveWebService : BaseWebService() {

    override val rootUrl = "https://www.rightmove.co.uk"
    override val sessionCookieName = "rmsessionid"
    override val sessionCookieDomain = ".rightmove.co.uk"

    private val store: Store by inject()
    private val requestCaller: RequestCaller by inject()
    private val console: ConsoleWriter by inject()

    companion object {
        private const val USERNAME = "gabor.biro@yahoo.com"
        private const val PASSWORD = "FA2x2+zh+/zD9Gc"
    }

    override fun login(driver: WebDriver): Boolean {
        driver.findElement(By.className("sign-in-link")).click()
        driver.findElement(By.id("email-input")).click()
        driver.findElement(By.id("email-input")).sendKeys(USERNAME)
        driver.findElement(By.id("password-input")).click()
        driver.findElement(By.id("password-input")).sendKeys(PASSWORD)
        driver.findElement(By.id("submit")).click()
        return true
    }

    override fun getPageInfo(driver: WebDriver, searchUrl: String): PageInfo {
        var page = splitQuery(searchUrl)["index"]?.let { it.toInt() / 24 } ?: 0
        val totalSize = driver.findElement(By.className("searchHeader-resultCount")).text.toInt()
        val listItems = driver.findElements(By.className("is-list"))
        val pageCount = ceil(totalSize.toDouble() / 24).toInt()
        val urls = listItems.mapNotNull {
            if (it.findElements(By.className("property-hidden-container")).isEmpty()) {
                val id = (it as RemoteWebElement).getAttribute("id")
                utilsService.getUrlFromWebId(id)
            } else {
                null
            }
        }
        page++
        return PageInfo(
            pageUrl = searchUrl,
            propertyWebIds = urls.map { utilsService.getPropertyIdFromUrl(it) },
            page = page,
            pageCount = pageCount,
        )
    }

    override fun fetchProperty(driver: WebDriver, webId: String): Property {
        ensurePageWithSession(utilsService.getUrlFromWebId(webId))
        with(driver) {
            val location = findElement(By.className("_1kck3jRw2PGQSOEy3Lihgp"))
                .findElement(By.tagName("img"))
                .getAttribute("src")
                .let { splitQuery(it) }
                .let {
                    PropertyLatLon(it["latitude"]!!, it["longitude"]!!)
                }
            val lettingDetailsMap = runCatching { findElement(By.className("_2E1qBJkWUYMJYHfYJzUb_r")) }.getOrNull()
                ?.findElements(By.tagName("div"))
                ?.map { it.findElement(By.tagName("dt")).text to it.findElement(By.tagName("dd")).text }
                ?.associate { it } ?: emptyMap()
            val infoReel = runCatching { findElement(By.className("_4hBezflLdgDMdFtURKTWh")) }.getOrNull()
                ?.findElements(By.className("_1u12RxIYGx3c84eaGxI6_b"))
                ?.map { it.findElement(By.className("tmJOVKTrHAB4bLpcMjzQ")).text to it.findElement(By.className("_1fcftXUEbWfJOJzIUeIHKt")).text }
                ?.associate { it } ?: emptyMap()
            val availableFromText = lettingDetailsMap["Let available date:"]
            val availableFrom = availableFromText?.let {
                if (it == "Now") {
                    LocalDate.now()
                } else {
                    LocalDate.parse(
                        it,
                        DateTimeFormatter.ofPattern("dd/MM/yyyy")
                    )
                }
            }
            val rawPrice = findElement(By.className("_1gfnqJ3Vtd1z40MlC0MzXu")).findElement(
                By.tagName(
                    "span"
                )
            ).text
            val price = when (val result = ensurePriceIsPerMonth(rawPrice)) {
                is PriceParseResult.Price -> Price(
                    rawPrice,
                    result.pricePerMonth,
                    result.pricePerMonthInt
                )

                is PriceParseResult.ParseError -> {
                    console.e("Error parsing price $rawPrice")
                    Price(
                        rawPrice,
                        result.pricePerMonth,
                        -1
                    )
                }
            }
            return Property(
                webId = webId,
                title = findElement(By.xpath("/html/body/div[4]/div/div[3]/main/div[1]/div[1]/div/h1")).text,
                comment = null,
                markedUnsuitable = false,
                isBuddyUp = false,
                senderName = null,
                messageUrl = null,
                prices = arrayOf(price),
                location = location,
                billsIncluded = null,
                deposit = "",
                availableFrom = availableFrom?.toEpochDay(),
                minTerm = lettingDetailsMap["Let type:"] ?: "",
                maxTerm = "",
                furnished = lettingDetailsMap["Furnish type:"] == "Furnished",
                broadband = "",
                livingRoom = null,
                flatmates = null,
                totalRooms = infoReel["BEDROOMS"]?.replace("x", "")?.toInt(),
                householdGender = "",
                preferredGender = "",
                occupation = "",
            )
        }
    }

    override fun updateSuitability(driver: WebDriver, webId: String, suitable: Boolean, description: String) {
        store.getCookies()?.let { cookies ->
            if (GlobalVariables.safeMode || requestCaller.post(
                    url = "https://my.rightmove.co.uk/property/status",
                    payload = "[{\"id\": \"$webId\", \"action\": \"${if (suitable.not()) "HIDE" else "UNHIDE"}\"}]",
                    cookies = cookies.cookies.joinToString("; ")
                )
            ) {
                console.d("$webId updated suitable=$suitable $description")
            }
        } ?: run {
            console.e("Unable to mark property. We don't have any cookies.")
        }
        // TODO assert correct outcome
    }

    override fun getPhotoUrls(driver: WebDriver, webId: String): List<String> {
        return driver.findElements(By.className("_2zqynvtIxFMCq18pu-g8d_"))
            .mapNotNull {
                kotlin.runCatching { (it.findElement(By.tagName("meta")) as RemoteWebElement).getAttribute("content") }
                    .getOrNull()
            }
    }
}