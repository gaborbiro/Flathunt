package app.gaborbiro.flathunt.directions.model

class DirectionsLatLon(
    val latitude: String,
    val longitude: String,
) {
    override fun toString(): String {
        return toURL()
    }

    fun toURL() = "http://www.google.com/maps/place/${toGoogleCoords()}"

    fun toGoogleCoords()  = "$latitude,$longitude"
}