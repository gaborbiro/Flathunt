package app.gaborbiro.flathunt.repo

import app.gaborbiro.flathunt.console.ConsoleWriter
import app.gaborbiro.flathunt.data.domain.Store
import app.gaborbiro.flathunt.repo.domain.FetchPropertyRepository
import app.gaborbiro.flathunt.repo.domain.SearchRepository
import app.gaborbiro.flathunt.repo.domain.model.FetchPropertyResult
import app.gaborbiro.flathunt.repo.domain.model.SaveType
import app.gaborbiro.flathunt.service.domain.UtilsService
import app.gaborbiro.flathunt.service.domain.WebService
import app.gaborbiro.flathunt.service.domain.model.PageInfo
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
        val addedIds = mutableListOf<String>()
        val unsuitableIds = mutableListOf<String>()
        val failedIds = mutableListOf<String>()
        var currentSearchUrl: String? = url
        var failedCounter = 0
        do {
            val pageInfo: PageInfo = webService.getPageInfo(currentSearchUrl!!)
            console.d("Fetching page ${pageInfo.page}/${pageInfo.pageCount}")
            val storedIds = store.getProperties().map { it.webId }.toSet()
            val blacklist = store.getBlacklistWebIds().toSet()
            val newIds: List<String> = pageInfo.propertyWebIds - blacklist - storedIds
            newIds.forEach { webId ->
                console.d(
                    "\n=======> Fetching $webId (${newIds.indexOf(webId) + 1}/${newIds.size}, page ${pageInfo.page}/${pageInfo.pageCount}): ",
                    newLine = false
                )

                try {
                    val result = fetchPropertyRepository.fetchProperty(webId, SaveType.SAVE)
                    when(result) {
                        is FetchPropertyResult.Property -> {
                            addedIds.add(result.property.webId)
                        }
                        is FetchPropertyResult.Unsuitable -> {
                            unsuitableIds.add(webId)
                            failedCounter++
                        }
                        is FetchPropertyResult.Failed -> {
                            failedIds.add(webId)
                            failedCounter++
                        }
                    }
                } catch (t: Throwable) {
                    t.printStackTrace()
                    if (t is IOException) {
                        throw t
                    }
                    failedIds.add(webId)
                    failedCounter++
                }
            }
            currentSearchUrl = utilsService.getNextPageUrl(pageInfo, failedCounter)
        } while (currentSearchUrl != null)

        console.d("\nFinished")
        if (addedIds.isNotEmpty()) {
            console.i("New ids: ${addedIds.joinToString(",")}")
        }
        if (unsuitableIds.isNotEmpty()) {
            console.i("Unsuitable ids: ${unsuitableIds.joinToString(",")}")
        }
        if (failedIds.isNotEmpty()) {
            console.i("Failed ids: ${failedIds.joinToString(",")}")
        }
    }
}