package app.gaborbiro.flathunt.service.spareroom.usecase

import app.gaborbiro.flathunt.repo.domain.InboxRepository
import app.gaborbiro.flathunt.repo.domain.model.MessageTag
import app.gaborbiro.flathunt.repo.domain.PropertyRepository
import app.gaborbiro.flathunt.repo.domain.RoutesRepository
import app.gaborbiro.flathunt.service.domain.Service
import app.gaborbiro.flathunt.usecase.base.BaseUseCase
import app.gaborbiro.flathunt.usecase.base.Command
import app.gaborbiro.flathunt.usecase.base.command
import org.koin.core.component.inject

class InboxUseCase : BaseUseCase() {

    private val service: Service by inject()
    private val routesRepository: RoutesRepository by inject()
    private val inboxRepository: InboxRepository by inject()
    private val propertyRepository: PropertyRepository by inject()

    override val commands: List<Command<*>>
        get() = listOf(
            command(
                command = "inbox",
                description = "Fetches properties from un-tagged messages in your inbox. Prioritizes properties mentioned explicitly. " +
                        "If none mentioned, checks in the main advert attached to the message " +
                        "(Note: only single-property adverts are supported for now). It will also calculate commute times to your points of interest and finally it will label/mark messages/properties as needed. Note: real time Google Maps API is used. Results are affected by time of day this command is run."
            ) {
                val messages = inboxRepository.fetchMessages()
                val properties = messages.map { inboxRepository.fetchPropertiesFromMessage(it) }.flatten()

                properties.forEach { property ->
                    propertyRepository.addOrUpdateProperty(property)
                }
                val (_, unsuitable) = routesRepository.validateByRoutes(properties)
                unsuitable.forEach { property ->
                    property.messageUrl?.let {
                        inboxRepository.tagMessage(it, MessageTag.REJECTED)
                    }
                }
            }
        )
}