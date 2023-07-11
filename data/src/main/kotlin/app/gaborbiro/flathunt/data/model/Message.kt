package app.gaborbiro.flathunt.data.model

class Message(
    val senderName: String,
    val messageLink: String,
    val propertyUrls: List<String>
) {
    override fun toString(): String {
        return "Message(senderName='$senderName', propertyLinks=${propertyUrls.size}, messageLink='$messageLink')"
    }
}

