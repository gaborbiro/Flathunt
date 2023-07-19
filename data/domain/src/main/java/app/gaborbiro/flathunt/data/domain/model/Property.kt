package app.gaborbiro.flathunt.data.domain.model

import app.gaborbiro.flathunt.*
import app.gaborbiro.flathunt.google.Route
import java.time.LocalDate

open class Property(
    val id: String = "",
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
    val routes: List<Route>? = null,
) : Comparable<Property> {

    constructor(property: PersistedProperty) : this(
        id = property.id,
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
        routes = property.routes,
    )

    override fun compareTo(other: Property): Int {
        if (id == other.id) return 0
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
        id: String? = null,
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
    ): Property {
        return Property(
            id = id ?: this.id,
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
            routes = this.routes,
        )
    }

    open fun withRoutes(routes: List<Route>?): Property {
        return Property(
            id = id,
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
    id: String,
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
    routes: List<Route>?,
) : Property(
    id,
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
    routes,
) {
    constructor(property: Property, index: Int) : this(
        index,
        property.id,
        property.title,
        property.comment,
        property.markedUnsuitable,
        property.isBuddyUp,
        property.senderName,
        property.messageUrl,
        property.prices,
        property.billsIncluded,
        property.deposit,
        property.availableFrom,
        property.minTerm,
        property.maxTerm,
        property.furnished,
        property.broadband,
        property.livingRoom,
        property.flatmates,
        property.totalRooms,
        property.householdGender,
        property.preferredGender,
        property.occupation,
        property.location,
        property.routes,
    )

    override fun withRoutes(routes: List<Route>?): PersistedProperty {
        return PersistedProperty(
            index = index,
            id = id,
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
            routes = routes,
        )
    }
}

fun Property.checkValid(criteria: ValidationCriteria): Boolean {
    val errors = validate(criteria)
    return if (errors.isEmpty()) {
        true
    } else {
        println("\nRejected: ${errors.joinToString()}")
        false
    }
}

/**
 * Validation is lenient, in that if a property attribute is missing, it won't fail the respective validation rule
 */
fun Property.validate(criteria: ValidationCriteria): List<String> {
    val errors = mutableListOf<String>()

    this.routes?.let {
        errors.addAll(it.validate(*criteria.pointsOfInterest.toTypedArray()))
    }

    if (criteria.maxPrice != null && this.prices.all { it.pricePerMonthInt > criteria.maxPrice!! }) {
        errors.add("too exp (${this.prices.joinToString(", ")})")
    }
    if (this.furnished != null) {
        if (criteria.furnished == true && (this.furnished == true).not()) {
            errors.add("unfurnished")
        }
        if (criteria.furnished == false && this.furnished == true) {
            errors.add("furnished")
        }
    }
    if (this.livingRoom != null) {
        if (criteria.sharedLivingRoom == true && (this.livingRoom == true).not()) {
            errors.add("no living room")
        }
        if (criteria.sharedLivingRoom == false && this.livingRoom == true) {
            errors.add("has living room")
        }
    }
    if (criteria.maxFlatmates != null && this.flatmates?.let { it > criteria.maxFlatmates!! } == true) {
        errors.add("more than ${criteria.maxFlatmates} flatmates")
    }
    if (criteria.maxBedrooms != null && this.totalRooms?.let { it > criteria.maxBedrooms!! } == true) {
        errors.add("more than ${criteria.maxBedrooms} bedrooms")
    }
    if (criteria.minBedrooms != null && this.totalRooms?.let { it < criteria.minBedrooms!! } == true) {
        errors.add("less than ${criteria.maxBedrooms} bedrooms")
    }
    if (this.minTerm == "Short term") {
        errors.add("short term let")
    }
    val availableFrom = this.availableFrom?.let { LocalDate.ofEpochDay(it) }
    availableFrom?.let {
        criteria.canMoveEarliest?.let {
            if (availableFrom < it) {
                errors.add("unavailable ($availableFrom)")
            }
        }
        criteria.canMoveLatest?.let {
            if (availableFrom > it) {
                errors.add("unavailable ($availableFrom)")
            }
        }
    }
    val maxTermMonths = if (this.maxTerm.isNotEmpty()) {
        val matcher = this.maxTerm.matcher("([\\d]+) month[s]?")
        if (matcher.find()) {
            matcher.group(1).replace(",", "").toInt()
        } else {
            if (this.maxTerm == "None") {
                Int.MAX_VALUE
            } else {
                null
            }
        }
    } else null
    criteria.minRequiredMonths?.let { minTerm ->
        maxTermMonths?.let {
            if (maxTermMonths < minTerm) {
                errors.add("max term too short: $minTerm")
            }
        }
    }
    criteria.noBedsit?.let {
        if (it && this.title.contains("bedsit", ignoreCase = true)) {
            errors.add("bedsit")
        } else if (!it && !this.title.contains("bedsit", ignoreCase = true)) {
            errors.add("not bedsit")
        } else {
        }
    }
    return errors
}

/**
 * The specified routes must satisfy all the specified points of interest
 */
fun List<Route>?.validate(vararg pois: POI): List<String> {
    val errors = mutableListOf<String>()
    if (this != null && pois.isNotEmpty()) {
        // In the current implementation the routes are generated based on the POIs so they have the same travelOptions.
        // Otherwise we'd have to come up with some way to find the original POI. It's a bit hacky but good enough for now.
        if (this.any { it.timeMinutes > it.travelLimit.maxMinutes }) {
            errors.add("too far")
        }
    }
    val missingRoutes =
        pois.map { it.description } - (this?.map { it.description } ?: emptyList())
    if (missingRoutes.isNotEmpty()) {
        errors.add("missing routes to: ${missingRoutes.joinToString(", ")}")
    }
    return errors
}