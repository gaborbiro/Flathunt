package app.gaborbiro.flathunt.service.domain

import org.openqa.selenium.Cookie

interface Browser {

    fun openNewTab(retry: Boolean = true): String?

    fun openTabs(vararg urls: String): List<String>

    fun openHTML(html: String)

    fun cleanup()

    fun pinCurrentTabs()

    fun closeUnpinnedTabs()

    fun clearCookies()

    fun addOrUpdateCookies(cookies: List<Cookie>)

    fun saveCookies()
}