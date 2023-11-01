package app.gaborbiro.flathunt.service.domain

import org.openqa.selenium.Cookie

interface Browser {

    fun openNewTab(retry: Boolean = true): String?

    fun openTabs(urls: List<String>): List<String>

    fun openHTML(html: String)

    fun cleanup()

    fun pinTabs()

    fun closeUnpinnedTabs()

    fun clearCookies()

    fun addOrUpdateCookies(cookies: List<Cookie>)

    fun saveCookies()
}