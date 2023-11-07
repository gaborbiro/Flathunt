package app.gaborbiro.flathunt.directions.model

data class DirectionsResult(
    val route: Route,
    val mode: DirectionsTravelMode,
    val discoveredName: String? = null,
    val destination: Destination,
    val to: DirectionsLatLon,
)

data class Route(
    val transitCount: Int,
    val timeMinutes: Int,
    val distanceKm: Float,
    val replacementTransitData: String?,
)