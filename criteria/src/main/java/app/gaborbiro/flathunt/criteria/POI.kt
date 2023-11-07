package app.gaborbiro.flathunt.criteria

import app.gaborbiro.flathunt.criteria.POITravelMode.WALKING

sealed class POI(open val description: String, open vararg val max: POITravelLimit) {

    open class CoordinateSet(
        override val description: String,
        val locations: List<Address>,
        override vararg val max: POITravelLimit,
    ) : POI(description, *max) {

        constructor(
            description: String,
            locations: List<Address>,
            max: POITravelLimit,
        ) : this(description, locations, *arrayOf(max))

        override fun toString(): String {
            return "$description, ${max.joinToString(", ")}"
        }
    }

    open class Coordinate(
        override val description: String,
        open val location: POILocation,
        override vararg val max: POITravelLimit,
    ) : POI(description, *max) {

        constructor(
            description: String,
            location: POILocation,
            max: POITravelLimit,
        ) : this(description, location, *arrayOf(max))

        override fun toString(): String {
            return "$description, ${max.joinToString(", ")}"
        }
    }

    class Address(
        override val description: String,
        override val location: POILocation,
        val address: String,
        override vararg val max: POITravelLimit
    ) : Coordinate(description, location, *max) {

        constructor(
            description: String,
            location: POILocation,
            address: String,
            max: POITravelLimit
        ) : this(description, location, address, *arrayOf(max))

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
