package app.gaborbiro.flathunt.data.domain.model

import app.gaborbiro.flathunt.*
import app.gaborbiro.flathunt.google.Route

open class Property(
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
    val location: LatLon? = null,
    val heating: Boolean? = null,
    val airConditioning: Boolean? = null,
    val routes: List<Route>? = null,
) : Comparable<Property> {

    constructor(property: PersistedProperty) : this(
        webId = property.webId,
        title = property.title,
        comment = property.comment,
        markedUnsuitable = property.markedUnsuitable,
        isBuddyUp = property.isBuddyUp,
        senderName = property.senderName,
        messageUrl = property.messageUrl,
        prices = property.prices,
        billsIncluded = property.billsIncluded,
        deposit = property.deposit,
        availableFrom = property.availableFrom,
        minTerm = property.minTerm,
        maxTerm = property.maxTerm,
        furnished = property.furnished,
        broadband = property.broadband,
        livingRoom = property.livingRoom,
        flatmates = property.flatmates,
        totalRooms = property.totalRooms,
        householdGender = property.householdGender,
        preferredGender = property.preferredGender,
        occupation = property.occupation,
        location = property.location,
        heating = property.heating,
        airConditioning = property.airConditioning,
        routes = property.routes,
    )

    override fun compareTo(other: Property): Int {
        if (webId == other.webId) return 0
        return averageScore().compareTo(other.averageScore())
    }

    /**
     * Score based on how close the property is to non-transit points of interests
     */
    private fun averageScore(): Int {
        return this.routes?.filter { it.travelLimit.mode != TravelMode.WALKING }?.map { it.timeMinutes }?.average()
            ?.toInt() ?: Int.MAX_VALUE
    }

    /**
     * Warning: [comment], [senderName], [messageUrl] and [location] cannot be deleted by passing null.
     * Use empty string or empty list.
     */
    fun clone(
        webId: String? = null,
        title: String? = null,
        comment: String? = null,
        markedUnsuitable: Boolean? = null,
        isBuddyUp: Boolean? = null,
        senderName: String? = null,
        messageUrl: String? = null,
        prices: Array<Price>? = null,
        billsIncluded: Boolean? = null,
        deposit: String? = null,
        availableFrom: Long? = null,
        minTerm: String? = null,
        maxTerm: String? = null,
        furnished: Boolean? = null,
        broadband: String? = null,
        livingRoom: Boolean? = null,
        flatmates: Int? = null,
        totalRooms: Int? = null,
        householdGender: String? = null,
        preferredGender: String? = null,
        occupation: String? = null,
        location: LatLon? = null,
        heating: Boolean? = null,
        airConditioning: Boolean? = null,
    ): Property {
        return Property(
            webId = webId ?: this.webId,
            title = title ?: this.title,
            comment = comment ?: this.comment,
            markedUnsuitable = markedUnsuitable ?: this.markedUnsuitable,
            isBuddyUp = isBuddyUp ?: this.isBuddyUp,
            senderName = senderName ?: this.senderName,
            messageUrl = messageUrl ?: this.messageUrl,
            prices = prices ?: this.prices,
            billsIncluded = billsIncluded ?: this.billsIncluded,
            deposit = deposit ?: this.deposit,
            availableFrom = availableFrom ?: this.availableFrom,
            minTerm = minTerm ?: this.minTerm,
            maxTerm = maxTerm ?: this.maxTerm,
            furnished = furnished ?: this.furnished,
            broadband = broadband ?: this.broadband,
            livingRoom = livingRoom ?: this.livingRoom,
            flatmates = flatmates ?: this.flatmates,
            totalRooms = totalRooms ?: this.totalRooms,
            householdGender = householdGender ?: this.householdGender,
            preferredGender = preferredGender ?: this.preferredGender,
            occupation = occupation ?: this.occupation,
            location = location ?: this.location,
            heating = heating ?: this.heating,
            airConditioning = airConditioning ?: this.airConditioning,
            routes = this.routes,
        )
    }

    open fun withRoutes(routes: List<Route>?): Property {
        return Property(
            webId = webId,
            title = title,
            comment = comment,
            markedUnsuitable = markedUnsuitable,
            isBuddyUp = isBuddyUp,
            senderName = senderName,
            messageUrl = messageUrl,
            prices = prices,
            billsIncluded = billsIncluded,
            deposit = deposit,
            availableFrom = availableFrom,
            minTerm = minTerm,
            maxTerm = maxTerm,
            furnished = furnished,
            broadband = broadband,
            livingRoom = livingRoom,
            flatmates = flatmates,
            totalRooms = totalRooms,
            householdGender = householdGender,
            preferredGender = preferredGender,
            occupation = occupation,
            location = location ?: this.location,
            heating = heating,
            airConditioning = airConditioning,
            routes = routes,
        )
    }
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

class PersistedProperty private constructor(
    val index: Int,
    webId: String,
    title: String,
    comment: String?,
    markedUnsuitable: Boolean,
    isBuddyUp: Boolean,
    senderName: String?,
    messageUrl: String?,
    prices: Array<Price>,
    billIncluded: Boolean?,
    deposit: String,
    availableFrom: Long?,
    minTerm: String,
    maxTerm: String,
    furnished: Boolean?,
    broadband: String,
    livingRoom: Boolean?,
    flatmates: Int?,
    totalRooms: Int?,
    householdGender: String,
    preferredGender: String,
    occupation: String,
    location: LatLon?,
    heating: Boolean?,
    airConditioning: Boolean?,
    routes: List<Route>?,
) : Property(
    webId,
    title,
    comment,
    markedUnsuitable,
    isBuddyUp,
    senderName,
    messageUrl,
    prices,
    billIncluded,
    deposit,
    availableFrom,
    minTerm,
    maxTerm,
    furnished,
    broadband,
    livingRoom,
    flatmates,
    totalRooms,
    householdGender,
    preferredGender,
    occupation,
    location,
    heating,
    airConditioning,
    routes,
) {
    constructor(property: Property, index: Int) : this(
        index = index,
        webId = property.webId,
        title = property.title,
        comment = property.comment,
        markedUnsuitable = property.markedUnsuitable,
        isBuddyUp = property.isBuddyUp,
        senderName = property.senderName,
        messageUrl = property.messageUrl,
        prices = property.prices,
        billIncluded = property.billsIncluded,
        deposit = property.deposit,
        availableFrom = property.availableFrom,
        minTerm = property.minTerm,
        maxTerm = property.maxTerm,
        furnished = property.furnished,
        broadband = property.broadband,
        livingRoom = property.livingRoom,
        flatmates = property.flatmates,
        totalRooms = property.totalRooms,
        householdGender = property.householdGender,
        preferredGender = property.preferredGender,
        occupation = property.occupation,
        location = property.location,
        heating = property.heating,
        airConditioning = property.airConditioning,
        routes = property.routes,
    )

    override fun withRoutes(routes: List<Route>?): PersistedProperty {
        return PersistedProperty(
            index = index,
            webId = webId,
            title = title,
            comment = comment,
            markedUnsuitable = markedUnsuitable,
            isBuddyUp = isBuddyUp,
            senderName = senderName,
            messageUrl = messageUrl,
            prices = prices,
            billIncluded = billsIncluded,
            deposit = deposit,
            availableFrom = availableFrom,
            minTerm = minTerm,
            maxTerm = maxTerm,
            furnished = furnished,
            broadband = broadband,
            livingRoom = livingRoom,
            flatmates = flatmates,
            totalRooms = totalRooms,
            householdGender = householdGender,
            preferredGender = preferredGender,
            occupation = occupation,
            location = location ?: this.location,
            heating = heating,
            airConditioning = airConditioning,
            routes = routes,
        )
    }
}
