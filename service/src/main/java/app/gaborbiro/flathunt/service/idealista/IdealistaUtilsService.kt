package app.gaborbiro.flathunt.service.idealista

import app.gaborbiro.flathunt.compileTimeConstant.Constants
import app.gaborbiro.flathunt.service.BaseUtilsService
import app.gaborbiro.flathunt.service.domain.UtilsService
import app.gaborbiro.flathunt.service.domain.model.PageInfo
import org.koin.core.annotation.Named
import org.koin.core.annotation.Singleton
import java.net.URI

@Singleton(binds = [UtilsService::class])
@Named(Constants.idealista + "_utils")
class IdealistaUtilsService : BaseUtilsService() {

    override val rootUrl = "https://www.idealista.pt/en"
    override val sessionCookieName = "cc"
    override val sessionCookieDomain = "www.idealista.pt"

    override fun getNextPageUrl(page: PageInfo, offset: Int): String? {
        return if (page.page < page.pageCount) {
            val uri = URI.create(page.pageUrl)
            val pathTokens = uri.path.split("/").filter { it.isNotBlank() }
            if (pathTokens.last().contains("pagina-")) {
                page.pageUrl.replace(Regex("pagina-([\\d]+)"), "pagina-${page.page + 1}")
            } else {
                page.pageUrl.replace("?", "pagina-${page.page + 1}?")
            }
        } else {
            null
        }
    }

    override fun getPropertyIdFromUrl(url: String): String {
        return if (isValidUrl(url)) {
            URI.create(url).path.split("/").last { it.isNotBlank() }
        } else {
            throw IllegalArgumentException("Unable to get property id from $url (invalid url)")
        }
    }

    override fun getUrlFromWebId(webId: String): String {
        return "${rootUrl}/imovel/$webId/"
    }
}