package app.gaborbiro.flathunt.directions.model

sealed class Destination(open val description: String, open val limits: List<DirectionsTravelLimit>) {

    open class Coordinate(
        override val description: String,
        open val location: DirectionsLatLon,
        override val limits: List<DirectionsTravelLimit>,
    ) : Destination(description, limits)

    data class Address(
        override val description: String,
        override val location: DirectionsLatLon,
        val address: String,
        override val limits: List<DirectionsTravelLimit>,
    ) : Coordinate(description, location, limits)

    data class NearestStation(
        override val description: String,
        val limit: DirectionsTravelLimit,
    ) : Destination(description, listOf(limit))
}
