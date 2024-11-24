package app.gaborbiro.flathunt.service.zoopla

import app.gaborbiro.flathunt.compileTimeConstant.Constants
import app.gaborbiro.flathunt.console.ConsoleWriter
import app.gaborbiro.flathunt.data.domain.model.Price
import app.gaborbiro.flathunt.data.domain.model.Property
import app.gaborbiro.flathunt.data.domain.model.PropertyLatLon
import app.gaborbiro.flathunt.service.BaseWebService
import app.gaborbiro.flathunt.service.PriceParseResult
import app.gaborbiro.flathunt.service.domain.model.PageInfo
import app.gaborbiro.flathunt.service.ensurePriceIsPerMonth
import app.gaborbiro.flathunt.splitQuery
import com.google.gson.Gson
import org.koin.core.annotation.Named
import org.koin.core.annotation.Singleton
import org.koin.core.component.inject
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.ceil

@Singleton
//@Named(Constants.zoopla + "_web")
class ZooplaWebService : BaseWebService() {

    override val rootUrl = "https://www.zoopla.co.uk"
    override val sessionCookieName = "_cs_s"
    override val sessionCookieDomain = ".zoopla.co.uk"

    private val console: ConsoleWriter by inject()

    companion object {
        private const val USERNAME = "gabor.biro@yahoo.com"
        private const val PASSWORD = "PS3em?jyK6y&s\$\$"
    }

    override fun beforeEnsureSession(driver: WebDriver) {
        Thread.sleep(500)
        runCatching {
            driver.switchTo().frame("gdpr-consent-notice")
            driver.findElement(By.id("save")).click()
            driver.switchTo().parentFrame()
        }
    }

    override fun login(driver: WebDriver): Boolean {
        driver.findElement(By.className("css-1rwrl3a")).click()
        driver.findElement(By.id("input-email-address")).click()
        driver.findElement(By.id("input-email-address")).sendKeys(USERNAME)
        driver.findElement(By.id("input-password")).click()
        driver.findElement(By.id("input-password")).sendKeys(PASSWORD)
        driver.findElement(By.className("e1oiir0n4")).click()
        return true
    }

    override fun getPageInfo(driver: WebDriver, searchUrl: String): PageInfo {
        var page = splitQuery(searchUrl)["pn"]?.toInt() ?: 0
        val urls = driver.findElement(By.className("css-kdnpqc-ListingsContainer"))
            .findElements(By.className("e2uk8e3")).map { it.getAttribute("href") }
        val totalSize = driver.findElement(By.className("css-1kx8akd-Text-SearchResultsTotalText"))
            .text.split(" result")[0].toInt()
        val pageCount = ceil(totalSize.toDouble() / 25).toInt()
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

        val json = driver.findElement(By.id("__NEXT_DATA__")).getAttribute("innerHTML")
        val propertyData = Gson().fromJson(json, ZooplaResponse::class.java)
            .props
            .initialProps
            .pageProps
            .data
            .listing

        val features = propertyData.features.bullets

        val availableFrom = runCatching {
            val availableFromText = driver.findElement(By.className("css-1f6ruxg-AvailableFrom")).text.trim()
            if (availableFromText == "Available immediately") {
                LocalDate.now()
            } else {
                val dateStr = availableFromText.removePrefix("Available from ")
                LocalDate.parse(
                    dateStr,
                    DateTimeFormatter.ofPattern("dd MMMM yyyy")
                )
            }
        }.getOrNull() ?: run {
            if (features?.contains("Available now") == true) {
                LocalDate.now()
            } else {
                null
            }
        }
        val rawPrice = propertyData.pricing.label
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
            title = propertyData.title,
            comment = null,
            markedUnsuitable = false,
            isBuddyUp = false,
            senderName = null,
            messageUrl = null,
            prices = arrayOf(price),
            billsIncluded = features?.contains("Bills Included"),
            deposit = "",
            availableFrom = availableFrom?.toEpochDay(),
            minTerm = "",
            maxTerm = "",
            furnished = propertyData.features.flags.furnishedState.name.let { it == "furnished" || it == "furnished_or_unfurnished" },
            broadband = "",
            livingRoom = propertyData.counts.numLivingRooms?.let { it > 0 },
            flatmates = null,
            totalRooms = propertyData.counts.numBedrooms ?: 0,
            householdGender = "",
            preferredGender = "",
            occupation = "",
            location = propertyData.location.coordinates.let {
                PropertyLatLon(
                    it.latitude.toString(),
                    it.longitude.toString()
                )
            },
        )
    }

    override fun getPhotoUrls(driver: WebDriver, webId: String): List<String> {
        val json = driver.findElement(By.id("__NEXT_DATA__")).getAttribute("innerHTML")
        val propertyData = Gson().fromJson(json, ZooplaResponse::class.java).props.initialProps.pageProps.data.listing
        val images = mutableListOf<String>()
        propertyData.propertyImage.forEach {
            it.filename?.let {
                images.add("https://lid.zoocdn.com/u/2400/1800/$it")
            }
        }
        propertyData.content?.floorPlan?.forEach {
            it.filename?.let {
                images.add("https://lid.zoocdn.com/u/2400/1800/$it")
            }
            it.original?.let { images.add(it) }
        }
        return images
    }

    override fun updateSuitability(driver: WebDriver, webId: String, suitable: Boolean, description: String) {
        throw NotImplementedError()
    }
}