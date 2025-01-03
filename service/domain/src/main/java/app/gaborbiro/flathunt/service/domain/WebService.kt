package app.gaborbiro.flathunt.service.domain

import app.gaborbiro.flathunt.data.domain.model.Message
import app.gaborbiro.flathunt.data.domain.model.Property
import app.gaborbiro.flathunt.repo.domain.model.MessageTag
import app.gaborbiro.flathunt.service.domain.model.PageInfo

/**
 * All calls in this interface will try to acquire session
 */
interface WebService {

    fun getPageInfo(searchUrl: String, propertiesRemoved: Int = 0): PageInfo

    fun fetchProperty(webId: String): Property

    fun updateSuitability(webId: String, suitable: Boolean)

    fun getPhotoUrls(webId: String): List<String>

    fun fetchMessages(safeMode: Boolean): List<Message>

    fun tagMessage(messageUrl: String, vararg tags: MessageTag)

    fun login()
}
