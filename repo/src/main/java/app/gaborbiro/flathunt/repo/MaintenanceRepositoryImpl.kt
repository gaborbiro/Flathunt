package app.gaborbiro.flathunt.repo

import app.gaborbiro.flathunt.data.domain.Store
import app.gaborbiro.flathunt.repo.domain.MaintenanceRepository
import app.gaborbiro.flathunt.service.domain.Service
import org.koin.core.annotation.Singleton
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.openqa.selenium.Cookie
import java.io.*
import java.util.stream.Collectors
import java.util.stream.Stream

@Singleton
class MaintenanceRepositoryImpl : MaintenanceRepository, KoinComponent {

    private val store: Store by inject()
    private val service: Service by inject()

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
        service.clearCookies()
    }

    override fun importCookiesToBrowser(path: String) {
        val cookieFile = File(path)
        if (cookieFile.exists() && cookieFile.isFile) {
            val reader = BufferedReader(InputStreamReader(FileInputStream(cookieFile)))
            val cookies = reader.lines()
                .flatMap {
                    Stream.of(*it.split(";").toTypedArray())
                }
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .map { it.split("=") }
                .filter { it.isNotEmpty() && it[0].isNotBlank() }
                .map {
                    if (it.size < 2) {
                        listOf(it[0], "")
                    } else {
                        it
                    }
                }
                .map { (key, value) ->
                    Cookie.Builder(key, value).build()
                }
                .collect(Collectors.toList())
            if (cookies.isNotEmpty()) {
                service.addOrUpdateCookies(cookies)
            }
        }
    }
}