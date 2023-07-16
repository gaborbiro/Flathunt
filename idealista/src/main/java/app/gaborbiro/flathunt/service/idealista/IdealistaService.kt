package app.gaborbiro.flathunt.service.idealista

import app.gaborbiro.flathunt.data.Store
import app.gaborbiro.flathunt.data.model.Property
import app.gaborbiro.flathunt.service.BaseService
import app.gaborbiro.flathunt.service.Page

class IdealistaService(private val store: Store) : BaseService(store) {

    override val cookieSessionName = ""
    override val cookieSessionDomain = ""

    override fun login() {

    }

    override fun fetchLinksFromSearch(searchUrl: String, propertiesRemoved: Int): Page {
        ensureTab(searchUrl)

    }

    override fun fetchProperty(id: String): Property {
        ensureTab(getUrlFromId(id))
        with(driver) {

        }
    }

    override fun markAsUnsuitable(id: String, index: Int?, unsuitable: Boolean) {
        super.markAsUnsuitable(id, index, unsuitable)

    }

    override fun getPropertyIdFromUrl(url: String): String {
        return if (isValidUrl(url)) {

        } else {
            throw IllegalArgumentException("Unable to get property id from $url (invalid url)")
        }
    }

    override fun getUrlFromId(id: String): String {
        return ""
    }

    override fun isValidUrl(url: String): Boolean {
        return url.startsWith("$RM_ROOT_URL/") && url.split("$RM_ROOT_URL/").size == 2
    }

    override fun cleanUrl(url: String): String {
        return url
    }

    override fun checkUrlOrId(arg: String): String? {
        var cleanUrl = cleanUrl(arg)

    }

    override fun getPhotoUrls(id: String): List<String> {
        ensureTab(getUrlFromId(id))

    }
}