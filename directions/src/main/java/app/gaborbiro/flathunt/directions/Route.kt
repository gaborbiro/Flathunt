package app.gaborbiro.flathunt.directions

import app.gaborbiro.flathunt.*

class Route(
    val description: String,
    val travelLimit: TravelLimit,
    val transitCount: Int,
    val timeMinutes: Int,
    val distanceKm: Float,
    val replacementTransitData: String?,
    val name: String?,
    val coordinates: DirectionsLatLon,
) {
    override fun toString(): String {
        val description = "$timeMinutes minutes ${travelLimit.mode.description} ${name?.let { "to $it " } ?: ""}"
        val way = when (travelLimit.mode) {
            TravelMode.TRANSIT -> "$description($transitCount changes,${replacementTransitData?.let { " bicycle on $it" }})"
            TravelMode.CYCLING -> "$description(${distanceKm.decimals(0)} km)"
            TravelMode.WALKING -> "$description(${(distanceKm * 1000).decimals(0)} meters)"
        }
        return "\n${addPadding(this.description, 16)} => $way"
    }
}