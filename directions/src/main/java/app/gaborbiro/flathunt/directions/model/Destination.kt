package app.gaborbiro.flathunt.directions.model

sealed class Destination(open val limits: List<DirectionsTravelLimit>) {

    open class Coordinate(
        open val location: DirectionsLatLon,
        override val limits: List<DirectionsTravelLimit>,
    ) : Destination(limits)

    data class Address(
        override val location: DirectionsLatLon,
        val address: String,
        override val limits: List<DirectionsTravelLimit>,
    ) : Coordinate(location, limits)

    data class NearestStation(
        val maxMinutes: Int,
    ) : Destination(listOf(DirectionsTravelLimit(DirectionsTravelMode.WALKING, maxMinutes)))
}
