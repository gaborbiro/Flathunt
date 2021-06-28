package app.gaborbiro.flathunt.google

class DirectionsResult(
    val transitCount: Int,
    val timeMinutes: Int,
    val distanceKm: Float,
    val replacementTransitData: String?,
)