package app.gaborbiro.flathunt.directions.model

data class POIResult(
    val originalDescription: String,
    val limit: DirectionsTravelLimit,
    val resolvedDestination: POIDestination?,
    val route: Route?,
)

data class POIDestination(
    val isNearestStation: Boolean,
    val description: String,
    val address: String,
    val location: DirectionsLatLon,
    val limit: DirectionsTravelLimit,
)

data class Route(
    val description: String,
    val transitCount: Int,
    val timeMinutes: Int,
    val distanceKm: Float,
    val replacementTransitData: String?,
    val limit: DirectionsTravelLimit,
)