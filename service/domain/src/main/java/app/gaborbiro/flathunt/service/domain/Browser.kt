package app.gaborbiro.flathunt.service.domain

import app.gaborbiro.flathunt.data.domain.model.CookieSet
import org.openqa.selenium.WebDriver

interface Browser : WebDriver {

    fun openNewTab(retry: Boolean = true): String?

    fun openTabs(urls: List<String>): List<String>

    fun openHTML(html: String)

    fun savePositionAndSize()

    fun cleanup()

    fun pinTabs()

    fun closeUnpinnedTabs()

    fun clearCookies()

    fun addOrUpdateCookies(cookies: CookieSet)

    fun getCookies(): CookieSet
}