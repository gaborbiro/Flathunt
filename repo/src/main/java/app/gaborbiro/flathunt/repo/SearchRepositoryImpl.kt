package app.gaborbiro.flathunt.repo

import app.gaborbiro.flathunt.console.ConsoleWriter
import app.gaborbiro.flathunt.data.domain.Store
import app.gaborbiro.flathunt.prettyPrint
import app.gaborbiro.flathunt.repo.domain.FetchPropertyRepository
import app.gaborbiro.flathunt.repo.domain.SearchRepository
import app.gaborbiro.flathunt.repo.domain.model.SaveType
import app.gaborbiro.flathunt.service.domain.UtilsService
import app.gaborbiro.flathunt.service.domain.WebService
import org.koin.core.annotation.Singleton
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.IOException

@Singleton
class SearchRepositoryImpl : SearchRepository, KoinComponent {

    private val store: Store by inject()
    private val webService: WebService by inject()
    private val utilsService: UtilsService by inject()
    private val fetchPropertyRepository: FetchPropertyRepository by inject()
    private val console: ConsoleWriter by inject()

    override fun fetchPropertiesFromAllPages(url: String) {
        val storedIds = store.getProperties().map { it.webId }.toSet()
        val addedIds = mutableListOf<String>()
        val failedIds = mutableListOf<String>()
        var currentSearchUrl: String? = url
        var markedAsUnsuitableCount = 0
        do {
            val pageInfo = webService.getPageInfo(currentSearchUrl!!)
            console.d("Fetching page ${pageInfo.page}/${pageInfo.pageCount}")
            val blacklist = store.getBlacklistWebIds().toSet()
            val newIds: List<String> = pageInfo.propertyWebIds - blacklist - storedIds
            val (suitable, unsuitable) = newIds.partition { webId ->
                console.d(
                    "\n=======> Fetching $webId (${newIds.indexOf(webId) + 1}/${newIds.size}, page ${pageInfo.page}/${pageInfo.pageCount}): ",
                    newLine = false
                )

                try {
                    val property = fetchPropertyRepository.fetchProperty(webId, SaveType.SAVE)
                    property != null
                } catch (t: Throwable) {
                    t.printStackTrace()
                    if (t is IOException) {
                        throw t
                    }
                    false
                }
            }
            addedIds.addAll(suitable)
            failedIds.addAll(unsuitable)
            markedAsUnsuitableCount += unsuitable.size
            currentSearchUrl = utilsService.getNextPageUrl(pageInfo, markedAsUnsuitableCount)
        } while (currentSearchUrl != null)

        console.d("\nFinished")
        if (addedIds.isNotEmpty()) {
            console.i("New ids: ${addedIds.joinToString(",")}")
        }
        if (failedIds.isNotEmpty()) {
            console.i("Rejected ids: ${failedIds.joinToString(",")}")
        }
    }
}