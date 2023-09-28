package app.gaborbiro.flathunt.directions

internal class DirectionsResult(
    val transitCount: Int,
    val timeMinutes: Int,
    val distanceKm: Float,
    val replacementTransitData: String?,
)