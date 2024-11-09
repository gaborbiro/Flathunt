package app.gaborbiro.flathunt.repo

import app.gaborbiro.flathunt.console.ConsoleWriter
import app.gaborbiro.flathunt.data.domain.Store
import app.gaborbiro.flathunt.data.domain.model.CookieSet
import app.gaborbiro.flathunt.repo.domain.MaintenanceRepository
import app.gaborbiro.flathunt.service.domain.Browser
import app.gaborbiro.flathunt.service.domain.UtilsService
import app.gaborbiro.flathunt.service.domain.WebService
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.koin.core.annotation.Singleton
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.openqa.selenium.Cookie
import java.io.*

@Singleton
class MaintenanceRepositoryImpl : MaintenanceRepository, KoinComponent {

    private val webService: WebService by inject()
    private val store: Store by inject()
    private val utilsService: UtilsService by inject()
    private val browser: Browser by inject()
    private val console: ConsoleWriter by inject()

    override fun backup(filepath: String) {
        store.getJsonProperties()?.let { json ->
            try {
                PrintWriter(filepath).use { it.print(json) }
                console.d("${json.length} bytes backed up")
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        } ?: run {
            console.d("Nothing to back up")
        }
    }

    override fun restore(filepath: String): Int {
        val json = File(filepath).bufferedReader().use { it.readText() }
        store.overrideJsonProperties(json)
        return store.getProperties().size
    }

    override fun clearBrowserCookies() {
        browser.clearCookies()
    }

    override fun clearStoredCookies() {
        store.clearCookies()
    }

    override fun importCookies(filepath: String) {
        val cookieFile = File(filepath)
        if (cookieFile.exists() && cookieFile.isFile) {
            val reader = BufferedReader(InputStreamReader(FileInputStream(cookieFile)))
//            val cookies: MutableList<Cookie> = reader.lines()
//                .filter { it.startsWith("//").not() }
//                .flatMap {
//                    Stream.of(*it.split(";").toTypedArray())
//                }
//                .map { it.trim() }
//                .filter { it.isNotBlank() }
//                .map { it.split("=") }
//                .filter { it.isNotEmpty() && it[0].isNotBlank() }
//                .map {
//                    if (it.size < 2) {
//                        listOf(it[0], "")
//                    } else {
//                        it
//                    }
//                }
//                .map { (key, value) ->
//                    Cookie.Builder(key, value)
//                        .domain(utilsService.domain())
//                        .build()
//                }
//                .collect(Collectors.toUnmodifiableList())

            val typeToken = object : TypeToken<List<Cookie>>() {}.type
            val cookies = Gson().fromJson<List<Cookie>>(reader.readText().replace(Regex("\\u202f"), " "), typeToken)

            if (cookies.isNotEmpty()) {
                val cookieSet = CookieSet(cookies.toSet())
                store.setCookies(cookieSet)
                browser.addOrUpdateCookies(cookieSet)
            }
        }
    }

    override fun saveCookies() {
        store.setCookies(browser.getCookies())
    }

    override fun loadCookies() {
        store.getCookies()
            ?.let {
                browser.addOrUpdateCookies(it)
            }
    }

    override fun openRoot() {
        webService.openRoot()
    }
}