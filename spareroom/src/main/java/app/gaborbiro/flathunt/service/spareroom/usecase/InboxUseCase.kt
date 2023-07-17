package app.gaborbiro.flathunt.service.spareroom.usecase

import app.gaborbiro.flathunt.GlobalVariables
import app.gaborbiro.flathunt.data.domain.Store
import app.gaborbiro.flathunt.data.domain.model.Message
import app.gaborbiro.flathunt.data.domain.model.PersistedProperty
import app.gaborbiro.flathunt.data.domain.model.Property
import app.gaborbiro.flathunt.service.BaseService
import app.gaborbiro.flathunt.service.domain.model.MessageTag
import app.gaborbiro.flathunt.usecase.base.*

class InboxUseCase(private val service: BaseService, private val store: Store, criteria: ValidationCriteria) :
    BaseUseCase(service, store, criteria) {

    override val commands: List<Command<*>>
        get() = listOf(
            command(
                command = "inbox",
                description = "Fetches properties from un-tagged messages in your inbox. Prioritizes properties mentioned explicitly. " +
                        "If none mentioned, checks in the main advert attached to the message " +
                        "(Note: only single-property adverts are supported for now). It will also calculate commute times to your points of interest and finally it will label/mark messages/properties as needed. Note: real time Google Maps API is used. Results are affected by time of day this command is run."
            ) {
                val properties =
                    fetchPropertiesFromAllMessages(
                        service.fetchMessages(GlobalVariables.safeMode),
                        GlobalVariables.safeMode
                    )
                val (_, unsuitable) = fetchRoutes(properties, GlobalVariables.safeMode)
                unsuitable.forEach { property ->
                    property.messageUrl?.let {
                        if (!GlobalVariables.safeMode) service.tagMessage(it, MessageTag.REJECTED)
                    }
                }
            }
        )

    /**
     * Fetch properties from all messages. Marks messages as needed.
     */
    private fun fetchPropertiesFromAllMessages(messages: List<Message>, safeMode: Boolean): List<Property> {
        val newProperties = mutableListOf<Property>()
        val savedProperties = store.getProperties()
        messages.forEach { message ->
            fetchPropertiesFromMessage(
                message = message,
                savedProperties = savedProperties,
                safeMode = safeMode
            ).forEach { newProperty: Property ->
                newProperties.add(newProperty)
            }
        }
        val properties: MutableList<Property> = store.getProperties().toMutableList()
        newProperties.forEach { property ->
            properties.removeIf { it.id == property.id }
            properties.add(property)
        }
        store.saveProperties(properties)
        return newProperties
    }

    private fun fetchPropertiesFromMessage(
        message: Message,
        savedProperties: List<PersistedProperty>,
        safeMode: Boolean
    ): List<Property> {
        val propertyUrl = message.propertyUrls
        val rejectionMap = propertyUrl.associateWith { false }.toMutableMap()
        val savedPropertiesMap = savedProperties.associateBy { it.id }
        val newProperties = mutableListOf<Property>()
        propertyUrl.forEach { propertyLink ->
            if (service.isValidUrl(propertyLink)) {
                val id = service.getPropertyIdFromUrl(propertyLink)
                val property = savedPropertiesMap[id] ?: run {
                    println("=======> Fetching property $propertyLink (sent by ${message.senderName})")
                    service.fetchProperty(id).clone(senderName = message.senderName, messageUrl = message.messageLink)
                }
                if (property.isBuddyUp) {
                    if (!safeMode) service.tagMessage(message.messageLink, MessageTag.BUDDY_UP)
                } else if (!property.checkValid(criteria)) {
                    if (!property.markedUnsuitable) { // not yet marked as unsuitable
                        if (!safeMode) service.markAsUnsuitable(id, (property as? PersistedProperty)?.index, true)
                    }
                    rejectionMap[propertyLink] = true
                } else {
                    if (property.prices.isEmpty()) {
                        println("Price missing")
                        if (!safeMode) service.tagMessage(message.messageLink, MessageTag.PRICE_MISSING)
                    } else {
                        newProperties.add(property)
                    }
                }
            } else {
                println("Not a spareroom url: $propertyLink (${message.senderName})")
            }
        }
        if (rejectionMap.values.any { it }) {
            if (rejectionMap.values.all { it }) {
                if (!safeMode) service.tagMessage(message.messageLink, MessageTag.REJECTED)
            } else if (rejectionMap.values.any { it }) {
                if (!safeMode) service.tagMessage(message.messageLink, MessageTag.PARTIALLY_REJECTED)
                println("Partial rejection:\n" + rejectionMap.map { it.key + " -> " + if (it.value) "rejected" else "fine" }
                    .joinToString("\n"))
            }
        }
        return newProperties
    }
}