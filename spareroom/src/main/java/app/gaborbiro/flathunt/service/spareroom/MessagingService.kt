package app.gaborbiro.flathunt.service.spareroom

import app.gaborbiro.flathunt.data.model.Message
import app.gaborbiro.flathunt.service.Service
import org.openqa.selenium.WebDriver

interface MessagingService : Service {

    fun fetchMessages(safeMode: Boolean): List<Message>

    fun tagMessage(messageUrl: String, vararg tags: Tag)
}