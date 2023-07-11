package app.gaborbiro.flathunt.usecases

import app.gaborbiro.flathunt.data.model.Property
import app.gaborbiro.flathunt.google.Route
import app.gaborbiro.flathunt.google.POI
import app.gaborbiro.flathunt.matcher
import java.time.LocalDate

class ValidationCriteria(
    /**
     * Note: Feel free to add or remove points of interest.
     * Note: commute-time calculation uses real-time Google Maps API and the time of day (at which you are running commands, like 'inbox' or 'search') affects the results.
     * Note: commute estimates seem to be on the cautious side. Add 5 extra minutes to your max travel time to get a more reliable validation.
     */
    val pointsOfInterest: List<POI>,
    val maxPrice: Int? = null,
    val maxFlatmates: Int? = null,
    val minBedrooms: Int? = null,
    val maxBedrooms: Int? = null,
    val furnished: Boolean? = null,
    val sharedLivingRoom: Boolean? = null,
    val canMoveEarliest: LocalDate? = null,
    val canMoveLatest: LocalDate? = null,
    /**
     * Number of months I want to stay ad minimum
     */
    val minRequiredMonths: Int? = null,
    val noBedsit: Boolean? = null,
)

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

    if (criteria.maxPrice != null && this.prices.all { it.pricePerMonthInt > criteria.maxPrice }) {
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
    if (criteria.maxFlatmates != null && this.flatmates?.let { it > criteria.maxFlatmates } == true) {
        errors.add("more than ${criteria.maxFlatmates} flatmates")
    }
    if (criteria.maxBedrooms != null && this.totalRooms?.let { it > criteria.maxBedrooms } == true) {
        errors.add("more than ${criteria.maxBedrooms} bedrooms")
    }
    if (criteria.minBedrooms != null && this.totalRooms?.let { it < criteria.minBedrooms } == true) {
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