package app.gaborbiro.flathunt.service

import app.gaborbiro.flathunt.matcher
import app.gaborbiro.flathunt.service.domain.UtilsService
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.openqa.selenium.NoSuchWindowException
import org.openqa.selenium.WebDriver

abstract class BaseUtilsService : UtilsService, KoinComponent {

    protected abstract val rootUrl: String
    protected abstract val sessionCookieName: String
    protected abstract val sessionCookieDomain: String
    private val driver: WebDriver by inject()
    private val browser: BrowserImpl by inject()

    ///// Functions that are service dependent, but do not require a browser instance

    override fun isValidUrl(url: String): Boolean {
        return url.startsWith("$rootUrl/") && url.split("$rootUrl/").size == 2
    }

    override fun cleanUrl(url: String): String {
        return url
    }

    override fun parseUrlOrWebId(arg: String): String? {
        var cleanUrl = cleanUrl(arg)
        if (!isValidUrl(cleanUrl)) {
            val matcher = cleanUrl.matcher("^([\\d]+)$")
            if (matcher.find()) {
                cleanUrl = getUrlFromWebId(arg)
            }
        }
        return if (isValidUrl(cleanUrl)) {
            cleanUrl
        } else {
            null
        }
    }
}