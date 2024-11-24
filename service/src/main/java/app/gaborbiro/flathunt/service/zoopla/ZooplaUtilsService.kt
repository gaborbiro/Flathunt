package app.gaborbiro.flathunt.service.zoopla

import app.gaborbiro.flathunt.compileTimeConstant.Constants
import app.gaborbiro.flathunt.matcher
import app.gaborbiro.flathunt.service.BaseUtilsService
import app.gaborbiro.flathunt.service.domain.model.PageInfo
import org.koin.core.annotation.Named
import org.koin.core.annotation.Singleton

@Singleton
//@Named(Constants.zoopla + "_utils")
class ZooplaUtilsService : BaseUtilsService() {

    override val rootUrl = "https://www.zoopla.co.uk"
    override val sessionCookieName = "_cs_s"
    override val sessionCookieDomain = ".zoopla.co.uk"

    override fun getNextPageUrl(page: PageInfo, offset: Int): String? {
        return if (page.page < page.pageCount) {
            var searchUrl = page.pageUrl.replace(Regex("&pn=[\\d]+"), "")
            searchUrl = searchUrl.replace(Regex("\\?pn=[\\d]+"), "")
            "$searchUrl&pn=$page"
        } else {
            null
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

    override fun getUrlFromWebId(webId: String): String {
        return "$rootUrl/to-rent/details/$webId/"
    }
}