package app.gaborbiro.flathunt.service

import app.gaborbiro.flathunt.matcher
import app.gaborbiro.flathunt.service.domain.UtilsService
import org.koin.core.component.KoinComponent

abstract class BaseUtilsService : UtilsService, KoinComponent {

    protected abstract val rootUrl: String
    protected abstract val sessionCookieName: String
    protected abstract val sessionCookieDomain: String

    ///// Functions that are service dependent, but do not require a browser instance

    override fun isValidUrl(url: String): Boolean {
        return url.startsWith("$rootUrl/") && url.split("$rootUrl/").size == 2
    }

    override fun cleanUrl(url: String): String {
        return url
    }

    override fun parseWebIdOrUrl(idu: String): String? {
        var cleanUrl = cleanUrl(idu)
        if (!isValidUrl(cleanUrl)) {
            val matcher = cleanUrl.matcher("^([\\d]+)$")
            if (matcher.find()) {
                cleanUrl = getUrlFromWebId(idu)
            }
        }
        return if (isValidUrl(cleanUrl)) {
            cleanUrl
        } else {
            null
        }
    }

    override fun domain() = sessionCookieDomain
}