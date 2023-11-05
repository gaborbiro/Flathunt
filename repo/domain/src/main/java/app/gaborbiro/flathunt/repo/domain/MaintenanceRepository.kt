package app.gaborbiro.flathunt.repo.domain

interface MaintenanceRepository {

    fun backup(filepath: String)

    fun restore(filepath: String): Int

    fun clearBrowserCookies()

    fun clearStoredCookies()

    fun importCookiesToBrowser(filepath: String)

    fun saveCookies()
}