package app.gaborbiro.flathunt

import app.gaborbiro.flathunt.TravelMode.WALKING

sealed class POI(open val description: String, open vararg val max: TravelLimit) {

    class Destination(
        override val description: String,
        val name: String,
        val coordinates: LatLon,
        override vararg val max: TravelLimit
    ) : POI(description, *max) {

        constructor(
            description: String,
            name: String,
            coordinates: LatLon,
            max: TravelLimit
        ) : this(description, name, coordinates, *arrayOf(max))

        override fun toString(): String {
            return "$description, ${max.joinToString(", ")}"
        }
    }

    object NearestRailStation : POI("Nearest station", 10 minutes WALKING)
}

class TravelLimit(
    val mode: TravelMode,
    val maxMinutes: Int,
) {
    override fun toString(): String {
        return "max $maxMinutes minutes of ${mode.description}"
    }
}

enum class TravelMode(val value: String, val description: String) {
    TRANSIT("transit", "commute"),
    CYCLING("bicycling", "cycling"),
    WALKING("walking", "walk")
}