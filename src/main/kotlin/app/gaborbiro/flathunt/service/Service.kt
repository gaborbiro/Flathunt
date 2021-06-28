package app.gaborbiro.flathunt.service

import app.gaborbiro.flathunt.data.model.Property

interface Service {

    fun openTabs(vararg urls: String): List<String>

    /**
     * Starting from a page other than the first one is supported.
     */
    fun fetchLinksFromSearch(searchUrl: String, propertiesRemoved: Int = 0): Page

    fun fetchProperty(id: String, newTab: Boolean = false): Property

    fun markAsUnsuitable(id: String, index: Int?, unsuitable: Boolean)

    fun getPropertyIdFromUrl(url: String): String

    fun getUrlFromId(id: String): String

    fun isValidUrl(url: String): Boolean

    fun cleanUrl(url: String): String

    fun checkUrlOrId(arg: String): String?

    fun cleanup()

    fun getPhotoUrls(id: String): List<String>

    fun pushTabHandles()

    fun popTabHandles()
}