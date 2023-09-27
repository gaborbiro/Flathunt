package app.gaborbiro.flathunt.service.spareroom

import app.gaborbiro.flathunt.*
import app.gaborbiro.flathunt.compileTimeConstant.Constants
import app.gaborbiro.flathunt.console.ConsoleWriter
import app.gaborbiro.flathunt.data.domain.model.Message
import app.gaborbiro.flathunt.data.domain.model.Price
import app.gaborbiro.flathunt.data.domain.model.Property
import app.gaborbiro.flathunt.repo.domain.model.MessageTag
import app.gaborbiro.flathunt.service.BaseWebService
import app.gaborbiro.flathunt.service.PriceParseResult
import app.gaborbiro.flathunt.service.domain.model.PageInfo
import app.gaborbiro.flathunt.service.ensurePriceIsPerMonth
import org.koin.core.annotation.Named
import org.koin.core.annotation.Singleton
import org.koin.core.component.inject
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.ceil

@Singleton
@Named(Constants.spareroom + "_web")
class SpareRoomWebService : BaseWebService() {

    companion object {
        const val MISSING_VALUE = "missing"

        private const val USERNAME = "gabor.biro@yahoo.com"
        private const val PASSWORD = "6euBDNW9JUssLwy"
    }

    override val rootUrl = "https://www.spareroom.co.uk"
    override val sessionCookieName = "session_id"
    override val sessionCookieDomain = ".spareroom.co.uk"

    private val console: ConsoleWriter by inject()

    override fun afterEnsureSession(driver: WebDriver) {
        if (driver.findElements(By.className("show-user-auth-popup")).isNotEmpty()) {
            // old session
            login(driver)
        }
    }

    override fun login(driver: WebDriver): Boolean {
        driver.findElement(By.className("show-user-auth-popup")).click()
        driver.findElement(By.id("loginemail")).click()
        driver.findElement(By.id("loginemail")).sendKeys(USERNAME)
        driver.findElement(By.id("loginpass")).click()
        driver.findElement(By.id("loginpass")).sendKeys(PASSWORD)
        driver.findElement(By.id("sign-in-button")).click()
        return true
    }

    /**
     * Assumes message is open
     */
    private fun fetchMessage(driver: WebDriver): Message {
        val senderName = driver.findElement(By.className("message_in__name")).text
        val messageBody = driver.findElement(By.xpath("//dd[@class='message_body']"))
        val links = mutableListOf<String>().apply {
            getLinksFromMessage(messageBody, output = this)
        }
        if (links.isEmpty()) {
            runCatching {
                val propertyId =
                    driver.findElement(By.linkText("View ad")).getAttribute("href")
                        .substringAfter("flatshare_id=", "")
                        .substringBefore("&", "")
                if (propertyId.isNotBlank()) {
                    links.add("$rootUrl/$propertyId")
                }
            }
        }
        if (links.isEmpty()) { // try the top "view their ad" link
            val currentAdLink = kotlin.runCatching { driver.findElement(By.linkText("view their ad")) }.getOrNull()
                ?.getAttribute("href")
            currentAdLink?.let { links.add(it) }
        }
        console.d(senderName + " (${links.size} links)")
        return Message(senderName, driver.currentUrl, links)
    }

    private fun getLinksFromMessage(element: WebElement, output: MutableList<String>) {
        element.findElements(By.xpath("*")).forEach {
            val href = it.getAttribute("href")
            if (!href.isNullOrBlank()) {
                output.add(utilsService.cleanUrl(href))
            }
            getLinksFromMessage(it, output)
        }
    }

    override fun getPageInfo(driver: WebDriver, searchUrl: String): PageInfo {
        var page = splitQuery(searchUrl)["offset"]?.let { it.toInt() / 10 } ?: 0
        val urls = driver.findElements(By.className("listing-result")).mapNotNull {
            if (runCatching { it.findElement(By.linkText("Unsuitable")) }.getOrNull() != null) {
                // Featured properties will appear in the search result even if marked as unsuitable
                null
            } else {
                it.findElement(By.linkText("More info")).getAttribute("href")
            }
        }
        val totalSize = driver.findElement(By.id("results_header")).text.split(" of ")[1].split(" ")[0].toInt()
        val pageCount = ceil(totalSize / 10.0).toInt()
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
            val roomOnlyPricesXpath =
                "//div[@class=\"property-details\"]/section[@class=\"feature feature--price_room_only\"]/ul/li"
            val wholePropertyPricesXpath =
                "//div[@class=\"property-details\"]/section[@class=\"feature feature--price-whole-property\"]/h3"
            val rawPrices = mutableListOf<String>()
            runCatching {
                findElements(By.xpath(roomOnlyPricesXpath))
            }.getOrNull()?.mapNotNull {
                val price = it.findElement(By.xpath("strong")).text
                val comment = it.findElement(By.xpath("small")).text
                if (!comment.contains("NOW LET")) price else null
            }?.apply {
                rawPrices.addAll(this)
            }

            if (rawPrices.isEmpty()) {
                runCatching {
                    findElements(By.xpath(wholePropertyPricesXpath))
                }.getOrNull()?.mapNotNull { it.text }?.toTypedArray()?.apply {
                    rawPrices.addAll(this)
                }
            }

            val availableFromText = findSimpleText(
                "//dt[@class=\"feature-list__key\" and text()=\"Available\"]/following-sibling::dd[1]"
            )
            val availableFrom = availableFromText?.let {
                if (it == "Now") {
                    LocalDate.now()
                } else {
                    LocalDate.parse(
                        it,
                        DateTimeFormatter.ofPattern("dd MMM yyyy")
                    )
                }
            }

            val prices = rawPrices.map { rawPrice ->
                when (val result = ensurePriceIsPerMonth(rawPrice)) {
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
            }
            return Property(
                webId = webId,
                title = findSimpleText("//h1[1]") ?: MISSING_VALUE,
                comment = null,
                markedUnsuitable = linkExists("Marked as unsuitable"),
                isBuddyUp = checkForBuddyUp(driver),
                senderName = null,
                messageUrl = null,
                prices = prices.toTypedArray(),
                location = findRegex("latitude: \"(.*?)\",longitude: \"(.*?)\"")?.let {
                    if (it[0].isNotEmpty() && it[1].isNotEmpty())
                        LatLon(it[0], it[1])
                    else null
                },
                billsIncluded = null,
                deposit = findSimpleText(
                    "//dt[@class=\"feature-list__key\" and text()=\"Deposit\"]/following-sibling::dd[1]"
                ) ?: MISSING_VALUE,
                availableFrom = availableFrom?.toEpochDay(),
                minTerm = findSimpleText(
                    "//dt[@class=\"feature-list__key\" and text()=\"Minimum term\"]/following-sibling::dd[1]"
                ) ?: MISSING_VALUE,
                maxTerm = findSimpleText(
                    "//dt[@class=\"feature-list__key\" and text()=\"Maximum term\"]/following-sibling::dd[1]"
                ) ?: MISSING_VALUE,
                furnished = findSimpleText(
                    "//dt[@class=\"feature-list__key\" and text()=\"Furnishings\"]/following-sibling::dd[1]"
                ) == "Furnished",
                broadband = findSimpleText(
                    "//dt[@class=\"feature-list__key\" and text()=\"Broadband\"]/following-sibling::dd[1]"
                ) ?: MISSING_VALUE,
                livingRoom = findSimpleText(
                    "//dt[@class=\"feature-list__key\" and text()=\"Living room\"]/following-sibling::dd[1]"
                ) == "Yes",
                flatmates = findSimpleText(
                    "//section[@class=\"feature feature--current-household\"]//dt[@class=\"feature-list__key\" and (contains(text(),\"flatmates\") or contains(text(),\"housemates\"))]/following-sibling::dd[1]"
                )?.toInt(),
                totalRooms = findSimpleText(
                    "//section[@class=\"feature feature--current-household\"]//dt[@class=\"feature-list__key\" and contains(text(),\"Total # rooms\")]/following-sibling::dd[1]"
                )?.toInt(),
                householdGender = findSimpleText(
                    "//section[@class=\"feature feature--current-household\"]//dt[@class=\"feature-list__key\" and contains(text(),\"Gender\")]/following-sibling::dd[1]"
                ) ?: MISSING_VALUE,
                preferredGender = findSimpleText(
                    "//section[@class=\"feature feature--household-preferences\"]//dt[@class=\"feature-list__key\" and contains(text(),\"Gender\")]/following-sibling::dd[1]"
                ) ?: MISSING_VALUE,
                occupation = findSimpleText(
                    "//section[@class=\"feature feature--household-preferences\"]//dt[@class=\"feature-list__key\" and contains(text(),\"Occupation\")]/following-sibling::dd[1]"
                ) ?: MISSING_VALUE,
                routes = null,
            )
        }
    }

    private fun checkForBuddyUp(driver: WebDriver): Boolean {
        return driver.findElements(By.className("key-features__feature")).any {
            it.text.contains("wanted")
        }
    }

    override fun markAsUnsuitable(driver: WebDriver, webId: String, unsuitable: Boolean, description: String) {
        ensurePageWithSession(utilsService.getUrlFromWebId(webId))
        if (unsuitable) {
            runCatching { driver.findElement(By.linkText("Mark as unsuitable")) }.getOrNull()
                ?.let {
                    it.click()
                    console.d("$webId marked $description")
                    driver.switchTo().alert().dismiss()
                    return
                }
            runCatching { driver.findElement(By.linkText("Saved - remove ad")) }.getOrNull()?.let {
                it.click()
                ensurePageWithSession(utilsService.getUrlFromWebId(webId))
                runCatching { driver.findElement(By.linkText("Mark as unsuitable")) }.getOrNull()
                    ?.let {
                        it.click()
                        console.d("$webId marked $description")
                        driver.switchTo().alert().dismiss()
                        return
                    }
            }
            runCatching { driver.findElement(By.linkText("Contacted")) }.getOrNull()?.let {
                it.click()
                runCatching { driver.findElement(By.xpath("//input[@value='unsuitable']")) }.getOrNull()?.click()
                runCatching { driver.findElement(By.className("submit")) }.getOrNull()?.click()
                console.d("$webId marked $description")
                return
            }
        } else {
            runCatching { driver.findElement(By.linkText("Marked as Unsuitable")) }.getOrNull()?.let {
                it.click()
                ensurePageWithSession(utilsService.getUrlFromWebId(webId))
                runCatching { driver.findElement(By.linkText("Remove from saved")) }.getOrNull()
                    ?.let {
                        it.click()
                        runCatching { driver.findElement(By.className("submit")) }.getOrNull()
                            ?.let {
                                it.click()
                                console.d("$webId marked $description")
                                return
                            }
                    }
            }
        }
        console.e("Failed to mark $webId $description")
    }

    override fun getPhotoUrls(driver: WebDriver, webId: String): List<String> {
        return driver.findElements(By.className("photoswipe_me")).map {
            it.findElement(By.tagName("img")).getAttribute("src").replace("square", "large")
        }
    }

    /**
     * Requires same webpage to stay open
     */
    override fun fetchMessages(driver: WebDriver, safeMode: Boolean): List<Message> {
        ensurePageWithSession("$rootUrl/flatshare/mythreads.pl")
        val messages = mutableListOf<Message>()

        runCatching {
            driver.findElement(By.linkText("Oldest first")).click()
            val rawMessages = driver.findElements(By.className("msg_row"))

            if (rawMessages.isEmpty()) {
                console.e("no messages found")
            } else {
                rawMessages[0].click()
                do {
                    if (getMessageTags(driver).isEmpty()) { // ignoring tagged messages
                        val message = fetchMessage(driver)
                        if (message.propertyUrls.isEmpty()) {
                            if (!safeMode) tagMessage(driver.currentUrl, MessageTag.NO_LINKS)
                        } else {
                            messages.add(message)
                        }
                    }
                } while (runCatching { driver.findElement(By.linkText("Next thread >>")) }.let {
                        it.getOrNull()?.click()
                        it.isSuccess
                    })
            }
        }.apply {
            if (isFailure) {
                this.exceptionOrNull()?.printStackTrace()
            }
        }
        return messages
    }

    /**
     * Assumes message is open
     */
    private fun getMessageTags(driver: WebDriver): List<String> {
        return runCatching { driver.findElements(By.className("add-label__attached-label")) }
            .getOrNull()?.map { it.text } ?: emptyList()
    }

    override fun tagMessage(driver: WebDriver, messageUrl: String, vararg tags: MessageTag) {
        ensurePageWithSession(messageUrl)
        tags.forEach { tag ->
            val success: Boolean =
                runCatching { driver.findElement(By.className("add-label__link")) }.getOrNull()?.let {
                    it.click() // open tag menu
                    val id = when (tag) {
                        MessageTag.REJECTED -> "add_label_354322"
                        MessageTag.PARTIALLY_REJECTED -> "add_label_1482847"
                        MessageTag.PRICE_MISSING -> "add_label_1481816"
                        MessageTag.NO_LINKS -> "add_label_1481817"
                        MessageTag.BUDDY_UP -> "add_label_1482896"
                    }
                    runCatching { driver.findElement(By.id(id)) }.getOrNull()
                        ?.let { it.click(); true }
                        ?: run { driver.findElement(By.className("add-label__close")).click(); true }
                } ?: false
            if (success) {
                console.d("Message tagged $tag")
            } else {
                console.e("Failed to tag message with $tag")
            }
        }
    }
}