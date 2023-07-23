package app.gaborbiro.flathunt.repo

import app.gaborbiro.flathunt.GlobalVariables
import app.gaborbiro.flathunt.data.domain.Store
import app.gaborbiro.flathunt.data.domain.model.Message
import app.gaborbiro.flathunt.data.domain.model.PersistedProperty
import app.gaborbiro.flathunt.data.domain.model.Property
import app.gaborbiro.flathunt.repo.domain.InboxRepository
import app.gaborbiro.flathunt.repo.domain.model.MessageTag
import app.gaborbiro.flathunt.service.domain.Service
import org.koin.core.annotation.Singleton
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@Singleton
class InboxRepositoryImpl : InboxRepository, KoinComponent {

    private val store: Store by inject()
    private val service: Service by inject()
    private val validator: PropertyValidator by inject()

    override fun fetchMessages(): List<Message> {
        return service.fetchMessages(GlobalVariables.safeMode)
    }

    override fun fetchPropertiesFromMessage(message: Message): List<Property> {
        val propertyUrl = message.propertyUrls
        val messageRejection = propertyUrl.associateWith { false }.toMutableMap()
        val newProperties = mutableListOf<Property>()
        val savedProperties = store.getProperties().associateBy { it.id }
        propertyUrl.forEach { propertyLink ->
            if (service.isValidUrl(propertyLink)) {
                val id = service.getPropertyIdFromUrl(propertyLink)
                val property = savedProperties[id] ?: run {
                    println("=======> Fetching property $propertyLink (sent by ${message.senderName})")
                    service.fetchProperty(id).clone(senderName = message.senderName, messageUrl = message.messageLink)
                }
                if (property.isBuddyUp) {
                    if (!GlobalVariables.safeMode) service.tagMessage(message.messageLink, MessageTag.BUDDY_UP)
                } else if (!validator.checkValid(property)) {
                    if (!property.markedUnsuitable) { // not yet marked as unsuitable
                        if (!GlobalVariables.safeMode) {
                            val index = (property as? PersistedProperty)?.index
                            val description = index?.let { "($it)" } ?: ""
                            service.markAsUnsuitable(id, unsuitable = true, description)
                        }
                    }
                    messageRejection[propertyLink] = true
                } else {
                    if (property.prices.isEmpty()) {
                        println("Price missing")
                        if (!GlobalVariables.safeMode) service.tagMessage(message.messageLink, MessageTag.PRICE_MISSING)
                    } else {
                        newProperties.add(property)
                    }
                }
            } else {
                println("Not a spareroom url: $propertyLink (${message.senderName})")
            }
        }
        if (messageRejection.values.any { it }) {
            if (messageRejection.values.all { it }) {
                if (!GlobalVariables.safeMode) service.tagMessage(message.messageLink, MessageTag.REJECTED)
            } else if (messageRejection.values.any { it }) {
                if (!GlobalVariables.safeMode) service.tagMessage(message.messageLink, MessageTag.PARTIALLY_REJECTED)
                println("Partial rejection:\n" + messageRejection.map { it.key + " -> " + if (it.value) "rejected" else "fine" }
                    .joinToString("\n"))
            }
        }
        return newProperties
    }

    override fun tagMessage(url: String, tag: MessageTag) {
        if (!GlobalVariables.safeMode) service.tagMessage(url, MessageTag.REJECTED)
    }
}