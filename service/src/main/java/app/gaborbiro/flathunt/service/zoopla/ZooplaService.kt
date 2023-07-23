package app.gaborbiro.flathunt.service.zoopla

import app.gaborbiro.flathunt.LatLon
import app.gaborbiro.flathunt.compileTimeConstant.Constants
import app.gaborbiro.flathunt.data.domain.Store
import app.gaborbiro.flathunt.data.domain.model.Property
import app.gaborbiro.flathunt.matcher
import app.gaborbiro.flathunt.service.BaseService
import app.gaborbiro.flathunt.service.domain.model.Page
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
@Named(Constants.zoopla)
class ZooplaService : BaseService() {

    override val rootUrl = "https://www.zoopla.co.uk"
    override val sessionCookieName = "_cs_s"
    override val sessionCookieDomain = ".zoopla.co.uk"

    private val store: Store by inject<Store>()

    companion object {
        private const val USERNAME = "gabor.biro@yahoo.com"
        private const val PASSWORD = "PS3em?jyK6y&s\$\$"
    }

    override fun beforeSession(driver: WebDriver) {
        Thread.sleep(500)
        runCatching {
            driver.switchTo().frame("gdpr-consent-notice")
            driver.findElement(By.id("save")).click()
            driver.switchTo().parentFrame()
        }
    }

    override fun login(driver: WebDriver) {
        driver.findElement(By.className("css-1rwrl3a")).click()
        driver.findElement(By.id("input-email-address")).click()
        driver.findElement(By.id("input-email-address")).sendKeys(USERNAME)
        driver.findElement(By.id("input-password")).click()
        driver.findElement(By.id("input-password")).sendKeys(PASSWORD)
        driver.findElement(By.className("e1oiir0n4")).click()
    }

    override fun fetchLinksFromSearch(driver: WebDriver, searchUrl: String, propertiesRemoved: Int): Page {
        ensurePageWithSession(searchUrl)
        var page = splitQuery(searchUrl)["pn"]?.toInt() ?: 0
        val urls = driver.findElement(By.className("css-kdnpqc-ListingsContainer"))
            .findElements(By.className("e2uk8e3")).map { it.getAttribute("href") }
        val totalSize = driver.findElement(By.className("css-1kx8akd-Text-SearchResultsTotalText"))
            .text.split(" result")[0].toInt()
        val pageCount = ceil(totalSize.toDouble() / 25).toInt()
        page++
        return Page(
            urls = urls,
            page = page,
            pageCount = pageCount,
            nextPage = {
                if (page < pageCount) {
                    var searchUrl = searchUrl.replace(Regex("&pn=[\\d]+"), "")
                    searchUrl = searchUrl.replace(Regex("\\?pn=[\\d]+"), "")
                    "$searchUrl&pn=$page"
                } else {
                    null
                }
            }
        )
    }

    override fun fetchProperty(driver: WebDriver, id: String): Property {
        ensurePageWithSession(getUrlFromId(id))

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
        return Property(
            id = id,
            title = propertyData.title,
            comment = null,
            markedUnsuitable = false,
            isBuddyUp = false,
            senderName = null,
            messageUrl = null,
            prices = arrayOf(ensurePriceIsPerMonth(propertyData.pricing.label)),
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
                LatLon(
                    it.latitude.toString(),
                    it.longitude.toString()
                )
            },
            routes = null
        )
    }

    override fun getPropertyIdFromUrl(url: String): String {
        return if (isValidUrl(url)) {
            val matcher = url.matcher("(?<id>[\\d]+)")
            if (matcher.find()) {
                matcher.group("id")
            } else {
                throw IllegalArgumentException("Unable to get property id from $url (missing id)")
            }
        } else {
            throw IllegalArgumentException("Unable to get property id from $url (invalid url)")
        }
    }

    override fun getUrlFromId(id: String): String {
        return "$rootUrl/to-rent/details/$id/"
    }

    override fun isValidUrl(url: String): Boolean {
        return url.startsWith("$rootUrl/") && url.split("$rootUrl/").size == 2
    }

    override fun cleanUrl(url: String): String {
        return url
    }

    override fun checkUrlOrId(arg: String): String? {
        var cleanUrl = cleanUrl(arg)
        if (!isValidUrl(cleanUrl)) {
            val matcher = cleanUrl.matcher("^([\\d]+)$")
            if (matcher.find()) {
                cleanUrl = "$rootUrl/to-rent/details/$arg/"
            }
        }
        return if (isValidUrl(cleanUrl)) {
            cleanUrl
        } else {
            println("Invalid url: $cleanUrl")
            null
        }
    }

    override fun getPhotoUrls(driver: WebDriver, id: String): List<String> {
        ensurePageWithSession(getUrlFromId(id))
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

    override fun markAsUnsuitable(driver: WebDriver, id: String, unsuitable: Boolean, description: String) {
        throw NotImplementedError()
    }
}