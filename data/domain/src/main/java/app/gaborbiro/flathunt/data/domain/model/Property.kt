package app.gaborbiro.flathunt.data.domain.model


data class Property(
    val index: Int? = null,
    val webId: String,
    val title: String = "",
    val comment: String? = null,
    val markedUnsuitable: Boolean = false,
    val isBuddyUp: Boolean = false,
    val senderName: String? = null,
    val messageUrl: String? = null,
    val prices: Array<Price> = emptyArray(),
    val billsIncluded: Boolean? = null,
    val deposit: String = "",
    val availableFrom: Long? = null,
    val minTerm: String = "",
    val maxTerm: String = "",
    val furnished: Boolean? = null,
    val broadband: String = "",
    val livingRoom: Boolean? = null,
    val flatmates: Int? = null,
    val totalRooms: Int? = null,
    val householdGender: String = "",
    val preferredGender: String = "",
    val occupation: String = "",
    val location: PropertyLatLon? = null,
    val heating: Boolean? = null,
    val airConditioning: Boolean? = null,
    val commuteScore: Int? = null,
    val links: List<String> = emptyList(),
    val staticMapUrl: String? = null,
) : Comparable<Property> {

    override fun compareTo(other: Property): Int {
        if (webId == other.webId) return 0
        return (commuteScore ?: Int.MAX_VALUE).compareTo(other.commuteScore ?: Int.MAX_VALUE)
    }

//    /**
//     * Score based on how close the property is to non-transit points of interests
//     */
//    private fun averageScore(): Int {
//        return this.routes?.filter { it.travelMode != DirectionsTravelMode.WALKING }?.map { it.timeMinutes }?.average()
//            ?.toInt() ?: Int.MAX_VALUE
//    }
}

class Price(
    val price: String,
    val pricePerMonth: String,
    val pricePerMonthInt: Int
) {
    override fun toString(): String {
        return pricePerMonth + price.let { if (it != pricePerMonth) " ($price)" else "" }
    }
}
