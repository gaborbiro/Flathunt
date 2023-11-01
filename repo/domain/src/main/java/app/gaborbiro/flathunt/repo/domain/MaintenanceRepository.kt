package app.gaborbiro.flathunt.repo.domain

interface MaintenanceRepository {

    fun backup(path: String)

    fun restore(path: String): Int

    fun clearBrowserCookies()

    fun clearStoredCookies()

    fun importCookiesToBrowser(path: String)

    fun saveCookies()
}