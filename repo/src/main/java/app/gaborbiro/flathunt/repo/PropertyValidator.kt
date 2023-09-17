package app.gaborbiro.flathunt.repo

import app.gaborbiro.flathunt.POI
import app.gaborbiro.flathunt.ValidationCriteria
import app.gaborbiro.flathunt.data.domain.model.Property
import app.gaborbiro.flathunt.google.Route
import app.gaborbiro.flathunt.matcher
import org.koin.core.annotation.Singleton
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.LocalDate

@Singleton
class PropertyValidator : KoinComponent {

    private val criteria: ValidationCriteria by inject()

    fun checkValid(property: Property): Boolean {
        val errors = validate(property)
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
    fun validate(property: Property): List<String> {
        val errors = mutableListOf<String>()

        property.routes?.let {
            errors.addAll(it.validate(*criteria.pointsOfInterest.toTypedArray()))
        }

        if (criteria.maxPrice != null && property.prices.all { it.pricePerMonthInt > criteria.maxPrice!! }) {
            errors.add("too exp (${property.prices.joinToString(", ")})")
        }
        if (property.furnished != null) {
            if (criteria.furnished == true && (property.furnished == true).not()) {
                errors.add("unfurnished")
            }
            if (criteria.furnished == false && property.furnished == true) {
                errors.add("furnished")
            }
        }
        if (property.livingRoom != null) {
            if (criteria.sharedLivingRoom == true && (property.livingRoom == true).not()) {
                errors.add("no living room")
            }
            if (criteria.sharedLivingRoom == false && property.livingRoom == true) {
                errors.add("has living room")
            }
        }
        if (criteria.maxFlatmates != null && property.flatmates?.let { it > criteria.maxFlatmates!! } == true) {
            errors.add("more than ${criteria.maxFlatmates} flatmates")
        }
        if (criteria.maxBedrooms != null && property.totalRooms?.let { it > criteria.maxBedrooms!! } == true) {
            errors.add("more than ${criteria.maxBedrooms} bedrooms")
        }
        if (criteria.minBedrooms != null && property.totalRooms?.let { it < criteria.minBedrooms!! } == true) {
            errors.add("less than ${criteria.maxBedrooms} bedrooms")
        }
        if (property.minTerm == "Short term") {
            errors.add("short term let")
        }
        val availableFrom = property.availableFrom?.let { LocalDate.ofEpochDay(it) }
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
        val maxTermMonths = if (property.maxTerm.isNotEmpty()) {
            val matcher = property.maxTerm.matcher("([\\d]+) month[s]?")
            if (matcher.find()) {
                matcher.group(1).replace(",", "").toInt()
            } else {
                if (property.maxTerm == "None") {
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
        criteria.noBedsit?.also {
            if (it && property.title.contains("bedsit", ignoreCase = true)) {
                errors.add("bedsit")
            } else if (!it && !property.title.contains("bedsit", ignoreCase = true)) {
                errors.add("need bedsit")
            }
        }
        criteria.airConditioning?.also {
            if (it && property.airConditioning != true) {
                errors.add("no A/C")
            } else if (it.not() && property.airConditioning == true) {
                errors.add("no need for A/C")
            }
        }
        criteria.heating?.also {
            if (it && property.heating != true) {
                errors.add("no heating")
            } else if (it.not() && property.heating == true) {
                errors.add("no need for heating")
            }
        }
        return errors
    }

    /**
     * The specified routes must satisfy all the specified points of interest
     */
    private fun List<Route>?.validate(vararg pois: POI): List<String> {
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
}