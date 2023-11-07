package app.gaborbiro.flathunt.directions.model

data class Route(
    val transitCount: Int,
    val timeMinutes: Int,
    val mode: DirectionsTravelMode,
    val distanceKm: Float,
    val replacementTransitData: String?,
    val destinationName: String? = null,
    val destination: Destination,
    val to: DirectionsLatLon,
)