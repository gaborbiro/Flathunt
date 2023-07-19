package app.gaborbiro.flathunt.repo

import app.gaborbiro.flathunt.data.domain.Store
import app.gaborbiro.flathunt.repo.domain.MaintenanceRepository
import org.koin.core.annotation.Singleton
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import java.io.PrintWriter

@Singleton
class MaintenanceRepositoryImpl : MaintenanceRepository, KoinComponent {

    private val store: Store by inject()

    override fun backup(path: String) {
        store.getJsonProperties()?.let { json ->
            try {
                PrintWriter(path).use { it.print(json) }
                println("${json.length} bytes backed up")
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        } ?: run {
            println("Nothing to back up")
        }
    }

    override fun restore(path: String): Int {
        val json = File(path).bufferedReader().use { it.readText() }
        store.overrideJsonProperties(json)
        return store.getProperties().size
    }

    override fun clearCookies() {
        store.clearCookies()
    }
}