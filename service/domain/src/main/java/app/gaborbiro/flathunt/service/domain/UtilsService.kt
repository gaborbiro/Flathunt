package app.gaborbiro.flathunt.service.domain

import app.gaborbiro.flathunt.service.domain.model.PageInfo

interface UtilsService {

    fun getNextPageUrl(page: PageInfo, markedAsUnsuitableCount: Int): String?

    fun getPropertyIdFromUrl(url: String): String

    fun getUrlFromWebId(webId: String): String

    fun isValidUrl(url: String): Boolean

    fun cleanUrl(url: String): String

    fun parseUrlOrWebId(arg: String): String?
}
