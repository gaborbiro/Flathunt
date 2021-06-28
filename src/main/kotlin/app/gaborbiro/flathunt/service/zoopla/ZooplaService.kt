package app.gaborbiro.flathunt.service.zoopla

import app.gaborbiro.flathunt.service.Page
import app.gaborbiro.flathunt.data.Store
import app.gaborbiro.flathunt.data.model.Cookies
import app.gaborbiro.flathunt.data.model.LatLon
import app.gaborbiro.flathunt.data.model.Property
import app.gaborbiro.flathunt.matcher
import app.gaborbiro.flathunt.service.BaseService
import app.gaborbiro.flathunt.splitQuery
import com.google.gson.Gson
import org.openqa.selenium.By
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.ceil

class ZooplaService(private val store: Store) : BaseService(store) {

    override val cookieSessionName = "_cs_s"
    override val cookieSessionDomain = ".zoopla.co.uk"

    override fun beforeSession() {
        Thread.sleep(500)
        runCatching {
            driver.switchTo().frame("gdpr-consent-notice")
            driver.findElement(By.id("save")).click()
            driver.switchTo().parentFrame()
        }
    }

    override fun login() {
        driver.findElement(By.className("css-1rwrl3a")).click()
        driver.findElement(By.id("input-email-address")).click()
        driver.findElement(By.id("input-email-address")).sendKeys(USERNAME)
        driver.findElement(By.id("input-password")).click()
        driver.findElement(By.id("input-password")).sendKeys(PASSWORD)
        driver.findElement(By.className("e1oiir0n4")).click()

        store.saveCookies(Cookies(driver.manage().cookies))
    }

    override fun fetchLinksFromSearch(searchUrl: String, propertiesRemoved: Int): Page {
        ensureTab(searchUrl)
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
                    fetchLinksFromSearch("$searchUrl&pn=$page")
                } else {
                    null
                }
            }
        )
    }

    override fun fetchProperty(id: String): Property {
        ensureTab(getUrlFromId(id))

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
            prices = arrayOf(perWeekToPerMonth(propertyData.pricing.label)),
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
        return "$ZOOPLA_ROOT_URL/to-rent/details/$id/"
    }

    override fun isValidUrl(url: String): Boolean {
        return url.startsWith("$ZOOPLA_ROOT_URL/") && url.split("$ZOOPLA_ROOT_URL/").size == 2
    }

    override fun cleanUrl(url: String): String {
        return url
    }

    override fun checkUrlOrId(arg: String): String? {
        var cleanUrl = cleanUrl(arg)
        if (!isValidUrl(cleanUrl)) {
            val matcher = cleanUrl.matcher("^([\\d]+)$")
            if (matcher.find()) {
                cleanUrl = "$ZOOPLA_ROOT_URL/to-rent/details/$arg/"
            }
        }
        return if (isValidUrl(cleanUrl)) {
            cleanUrl
        } else {
            println("Invalid url: $cleanUrl")
            null
        }
    }

    override fun getPhotoUrls(id: String): List<String> {
        ensureTab(getUrlFromId(id))
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
}