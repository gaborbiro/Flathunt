package app.gaborbiro.flathunt.service.spareroom

import app.gaborbiro.flathunt.compileTimeConstant.Constants
import app.gaborbiro.flathunt.matcher
import app.gaborbiro.flathunt.service.BaseUtilsService
import app.gaborbiro.flathunt.service.domain.model.PageInfo
import org.koin.core.annotation.Named
import org.koin.core.annotation.Singleton
import org.openqa.selenium.By
import org.openqa.selenium.WebElement

@Singleton
//@Named(Constants.spareroom + "_utils")
class SpareRoomUtilsService : BaseUtilsService() {

    override val rootUrl = "https://www.spareroom.co.uk"
    override val sessionCookieName = "session_id"
    override val sessionCookieDomain = ".spareroom.co.uk"

    private fun getLinksFromMessage(element: WebElement, output: MutableList<String>) {
        element.findElements(By.xpath("*")).forEach {
            val href = it.getAttribute("href")
            if (href.isNullOrBlank().not()) {
                output.add(cleanUrl(href))
            }
            getLinksFromMessage(it, output)
        }
    }

    override fun getNextPageUrl(page: PageInfo, offset: Int): String? {
        return if (page.page < page.pageCount) {
            var searchUrl = page.pageUrl.replace(Regex("&offset=[\\d]+"), "")
            searchUrl = searchUrl.replace(Regex("\\?offset=[\\d]+"), "")
            searchUrl + "&offset=${page.page * 10 - offset}"
        } else {
            null
        }
    }

    override fun getPropertyIdFromUrl(url: String): String {
        return if (isValidUrl(url)) {
            var matcher = url.matcher("flatshare_id=(?<id>[\\d]+)\\&")
            if (matcher.find()) {
                matcher.group("id")
            } else {
                matcher = url.matcher("$rootUrl/(?<id>[\\d]+)")
                if (matcher.find()) {
                    matcher.group("id")
                } else {
                    matcher = url.matcher("fad_id=(?<id>[\\d]+)\\&")
                    if (matcher.find()) {
                        matcher.group("id")
                    } else {
                        throw IllegalArgumentException("Unable to get property id from $url (missing id)")
                    }
                }
            }
        } else {
            throw IllegalArgumentException("Unable to get property id from $url (invalid url)")
        }
    }

    override fun getUrlFromWebId(webId: String) = "$rootUrl/$webId"

    override fun isValidUrl(url: String) = url.startsWith(rootUrl) && url.split(rootUrl).size == 2

    override fun cleanUrl(url: String): String {
        val ROOT_URL_2 = "www.spareroom.co.uk"
        val ROOT_URL_3 = "http://www.spareroom.co.uk"
        val ROOT_URL_MOBILE = "https://m.spareroom.co.uk"
        val ROOT_URL_MOBILE_2 = "m.spareroom.co.uk"
        val ROOT_URL_MOBILE_3 = "http://m.spareroom.co.uk"

        var cleanUrl = url.trim()
        if (cleanUrl.startsWith(ROOT_URL_2)) { // www.spareroom.co.uk
            cleanUrl = cleanUrl.replace(ROOT_URL_2, rootUrl)
        }
        if (cleanUrl.startsWith(ROOT_URL_3)) { // http://www.spareroom.co.uk
            cleanUrl = cleanUrl.replace(ROOT_URL_3, rootUrl)
        }
        if (cleanUrl.startsWith(ROOT_URL_MOBILE_2)) { // m.spareroom.co.uk
            cleanUrl = cleanUrl.replace(ROOT_URL_MOBILE_2, rootUrl)
        }
        if (cleanUrl.startsWith(ROOT_URL_MOBILE_3)) { // http://m.spareroom.co.uk
            cleanUrl = cleanUrl.replace(ROOT_URL_MOBILE_3, rootUrl)
        }
        if (cleanUrl.startsWith(ROOT_URL_MOBILE)) { // https://m.spareroom.co.uk
            cleanUrl = cleanUrl.replace(ROOT_URL_MOBILE, rootUrl)
        }
        return cleanUrl
    }

}