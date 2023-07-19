package app.gaborbiro.flathunt

import app.gaborbiro.flathunt.POI
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
