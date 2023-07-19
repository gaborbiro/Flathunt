package app.gaborbiro.flathunt.repo.domain

import app.gaborbiro.flathunt.data.domain.model.Message
import app.gaborbiro.flathunt.data.domain.model.Property
import app.gaborbiro.flathunt.repo.domain.model.MessageTag

interface InboxRepository {

    fun fetchMessages(): List<Message>

    fun fetchPropertiesFromMessage(message: Message): List<Property>

    fun tagMessage(url: String, tag: MessageTag)
}