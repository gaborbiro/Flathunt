package app.gaborbiro.flathunt.service.rightmove

import app.gaborbiro.flathunt.compileTimeConstant.Constants
import app.gaborbiro.flathunt.matcher
import app.gaborbiro.flathunt.service.BaseUtilsService
import app.gaborbiro.flathunt.service.domain.model.PageInfo
import org.koin.core.annotation.Named
import org.koin.core.annotation.Singleton

@Singleton
@Named(Constants.rightmove + "_utils")
class RightmoveUtilsService : BaseUtilsService() {

    override val rootUrl = "https://www.rightmove.co.uk"
    override val sessionCookieName = "rmsessionid"
    override val sessionCookieDomain = ".rightmove.co.uk"

    override fun getNextPageUrl(page: PageInfo, offset: Int): String? {
        return if (page.page < page.pageCount) {
            var searchUrl = page.pageUrl.replace(Regex("&index=[\\d]+"), "")
            searchUrl = searchUrl.replace(Regex("\\?index=[\\d]+"), "")
            searchUrl + "&index=${page.page * 24}"
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
        return "$rootUrl/properties/$webId#/"
    }
}