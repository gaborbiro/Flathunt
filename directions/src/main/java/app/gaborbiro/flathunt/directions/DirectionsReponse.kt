package app.gaborbiro.flathunt.directions

import com.google.gson.annotations.SerializedName
import java.util.concurrent.TimeUnit

data class DirectionsResponse(
    @SerializedName("error_message")
    val errorMessage: String?,
    val routes: List<DirectionsRoute>, // only one route unless provideRouteAlternatives field is set to true
    @SerializedName("status")
    val status: String?,
)

data class DirectionsRoute(
    val legs: List<RouteLeg> // only one leg, unless waypoints are specified in the request
)

data class RouteLeg(
    val duration: LegDuration, // seconds
    val distance: LegDistance, // meters
    val steps: List<RouteStep>,
)

class LegDuration(val value: Int) {
    override fun toString(): String {
        return "${TimeUnit.SECONDS.toMinutes(value.toLong()).toInt()} minutes"
    }
}

data class LegDistance(val value: Int) {
    override fun toString(): String {
        return "${value / 1000.0} km"
    }
}

data class RouteStep(
    @SerializedName("travel_mode")
    val travelMode: String,
    @SerializedName("transit_details")
    val transitDetails: TransitDetails?,
    @SerializedName("start_location")
    val startLocation: LatLng,
    @SerializedName("end_location")
    val endLocation: LatLng,
    val duration: LegDuration, // seconds
    val distance: LegDistance, // meters
) {
    override fun toString(): String {
        return "RouteStep(travelMode='$travelMode', duration=$duration, distance=$distance, transitDetails=${transitDetails})"
    }
}

class TransitDetails(
    @SerializedName("departure_stop")
    val departureStop: Stop,
    @SerializedName("arrival_stop")
    val arrivalStop: Stop,
    val line: Line
) {
    override fun toString(): String {
        return "${departureStop.name} - ${arrivalStop.name} (${line.shortName ?: line.vehicle.name})"
    }
}

data class Stop(val name: String)

data class Line(
    @SerializedName("short_name")
    val shortName: String?,
    val vehicle: Vehicle,
)

data class Vehicle(val name: String)

data class LatLng(val lat: String, val lng: String)