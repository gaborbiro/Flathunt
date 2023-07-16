package app.gaborbiro.flathunt.google

class TflStopsResponse(
    val stopPoints: List<StopPoint>
)

class StopPoint(
    val modes: Array<String>,
    val commonName: String,
    val lat: String,
    val lon: String,
)