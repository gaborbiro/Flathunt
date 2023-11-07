package app.gaborbiro.flathunt.criteria

import app.gaborbiro.flathunt.criteria.POITravelMode.WALKING
import app.gaborbiro.flathunt.minutes

sealed class POI(open val description: String, open vararg val max: POITravelLimit) {

    open class Coordinate(
        override val description: String,
        open val latitude: String,
        open val longitude: String,
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

    class Address(
        override val description: String,
        override val latitude: String,
        override val longitude: String,
        val address: String,
        override vararg val max: POITravelLimit
    ) : Coordinate(description, latitude, longitude, *max) {

        constructor(
            description: String,
            latitude: String,
            longitude: String,
            address: String,
            max: POITravelLimit
        ) : this(description, longitude, latitude, address, *arrayOf(max))

        override fun toString(): String {
            return "$description, $address, ${max.joinToString(", ")}"
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
