package app.gaborbiro.flathunt.data.model

import app.gaborbiro.flathunt.google.TravelMode
import app.gaborbiro.flathunt.google.TravelLimit
import app.gaborbiro.flathunt.addPadding
import app.gaborbiro.flathunt.decimals

class Route(
    val description: String,
    val travelLimit: TravelLimit,
    val transitCount: Int,
    val timeMinutes: Int,
    val distanceKm: Float,
    val replacementTransitData: String?,
    val name: String?,
    val coordinates: LatLon,
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