package app.gaborbiro.flathunt.criteria

import app.gaborbiro.flathunt.criteria.POITravelMode.WALKING
import app.gaborbiro.flathunt.minutes

sealed class POI(open val description: String, open vararg val max: POITravelLimit) {

    class Destination(
        override val description: String,
        val latitude: String,
        val longitude: String,
        override vararg val max: POITravelLimit
    ) : POI(description, *max) {

        constructor(
            description: String,
            latitude: String,
            longitude: String,
            max: POITravelLimit
        ) : this(description, latitude, longitude, *arrayOf(max))

        override fun toString(): String {
            return "$description, ${max.joinToString(", ")}"
        }
    }

    data object NearestRailStation : POI("Nearest station", 10 minutes WALKING)
}

class POITravelLimit(
    val mode: POITravelMode,
    val maxMinutes: Int,
) {
    override fun toString(): String {
        return "max $maxMinutes minutes of ${mode.description}"
    }
}

enum class POITravelMode(val value: String, val description: String) {
    TRANSIT("transit", "commute"),
    CYCLING("bicycling", "cycling"),
    WALKING("walking", "walk")
}
