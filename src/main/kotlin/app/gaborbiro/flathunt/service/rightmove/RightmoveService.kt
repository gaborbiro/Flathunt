package app.gaborbiro.flathunt.service.rightmove

import app.gaborbiro.flathunt.*
import app.gaborbiro.flathunt.data.Store
import app.gaborbiro.flathunt.data.model.Cookies
import app.gaborbiro.flathunt.data.model.LatLon
import app.gaborbiro.flathunt.data.model.Property
import app.gaborbiro.flathunt.service.BaseService
import app.gaborbiro.flathunt.service.Page
import org.openqa.selenium.By
import org.openqa.selenium.remote.RemoteWebElement
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.ceil

class RightmoveService(private val store: Store) : BaseService(store) {

    override val cookieSessionName = "rmsessionid"
    override val cookieSessionDomain = ".rightmove.co.uk"

    override fun login() {
        driver.findElement(By.className("sign-in-link")).click()
        driver.findElement(By.id("email-input")).click()
        driver.findElement(By.id("email-input")).sendKeys(USERNAME)
        driver.findElement(By.id("password-input")).click()
        driver.findElement(By.id("password-input")).sendKeys(PASSWORD)
        driver.findElement(By.id("submit")).click()

        store.saveCookies(Cookies(driver.manage().cookies))
    }

    override fun fetchLinksFromSearch(searchUrl: String, propertiesRemoved: Int): Page {
        ensureTab(searchUrl)
        var page = splitQuery(searchUrl)["index"]?.let { it.toInt() / 24 } ?: 0
        val totalSize = driver.findElement(By.className("searchHeader-resultCount")).text.toInt()
        val listItems = driver.findElements(By.className("is-list"))
        val pageCount = ceil(totalSize.toDouble() / 24).toInt()
        val urls = listItems.mapNotNull {
            if (it.findElements(By.className("property-hidden-container")).isEmpty()) {
                val id = (it as RemoteWebElement).getAttribute("id")
                getUrlFromId(id)
            } else {
                null
            }
        }
        page++
        return Page(
            urls = urls,
            page = page,
            pageCount = pageCount,
            nextPage = {
                if (page < pageCount) {
                    var searchUrl = searchUrl.replace(Regex("&index=[\\d]+"), "")
                    searchUrl = searchUrl.replace(Regex("\\?index=[\\d]+"), "")
                    fetchLinksFromSearch(searchUrl + "&index=${page * 24}")
                } else {
                    null
                }
            }
        )
    }

    override fun fetchProperty(id: String): Property {
        ensureTab(getUrlFromId(id))
        with(driver) {
            val location = findElement(By.className("_1kck3jRw2PGQSOEy3Lihgp"))
                .findElement(By.tagName("img"))
                .getAttribute("src")
                .let { splitQuery(it) }
                .let {
                    LatLon(it["latitude"]!!, it["longitude"]!!)
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
            return Property(
                id = id,
                title = findElement(By.xpath("/html/body/div[4]/div/div[3]/main/div[1]/div[1]/div/h1")).text,
                comment = null,
                markedUnsuitable = false,
                isBuddyUp = false,
                senderName = null,
                messageUrl = null,
                prices = arrayOf(
                    perWeekToPerMonth(
                        findElement(By.className("_1gfnqJ3Vtd1z40MlC0MzXu")).findElement(
                            By.tagName(
                                "span"
                            )
                        ).text
                    )
                ),
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
                routes = null,
            )
        }
    }

    override fun markAsUnsuitable(id: String, index: Int?, unsuitable: Boolean) {
        super.markAsUnsuitable(id, index, unsuitable)
        store.getCookies()?.let { cookies ->
            if (GlobalVariables.safeMode || callPost(
                    "https://my.rightmove.co.uk/property/status",
                    "[{\"id\": \"$id\", \"action\": \"${if (unsuitable) "HIDE" else "UNHIDE"}\"}]",
                    cookies
                )
            ) {
                println("$id marked${index?.let { " ($it)" } ?: ""}")
            }
        } ?: run {
            println("Unable to mark property. We don't have any cookies.")
        }
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
        return "$RM_ROOT_URL/properties/$id#/"
    }

    override fun isValidUrl(url: String): Boolean {
        return url.startsWith("$RM_ROOT_URL/") && url.split("$RM_ROOT_URL/").size == 2
    }

    override fun cleanUrl(url: String): String {
        return url
    }

    override fun checkUrlOrId(arg: String): String? {
        var cleanUrl = cleanUrl(arg)
        if (!isValidUrl(cleanUrl)) {
            val matcher = cleanUrl.matcher("^([\\d]+)$")
            if (matcher.find()) {
                cleanUrl = "$RM_ROOT_URL/properties/$arg"
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
        return driver.findElements(By.className("_2zqynvtIxFMCq18pu-g8d_"))
            .mapNotNull {
                kotlin.runCatching { (it.findElement(By.tagName("meta")) as RemoteWebElement).getAttribute("content") }
                    .getOrNull()
            }
    }
}