package app.gaborbiro.flathunt.directions.model

sealed class Destination {
    data class Specific(
        val location: DirectionsLatLon,
        val limits: List<DirectionsTravelLimit>,
    ) : Destination()

    data class NearestStation(
        val limit: DirectionsTravelLimit,
    ) : Destination()
}
