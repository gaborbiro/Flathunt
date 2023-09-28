package app.gaborbiro.flathunt.google

internal class DirectionsResult(
    val transitCount: Int,
    val timeMinutes: Int,
    val distanceKm: Float,
    val replacementTransitData: String?,
)