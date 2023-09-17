package app.gaborbiro.flathunt.repo.domain

interface MaintenanceRepository {

    fun backup(path: String)

    fun restore(path: String): Int

    fun clearCookies()

    fun importCookiesToBrowser(path: String)
}