package app.gaborbiro.flathunt.google

class GoogleLatLon(
    val latitude: String,
    val longitude: String,
) {
    override fun toString(): String {
        return toURL()
    }

    fun toURL() = "http://www.google.com/maps/place/${toGoogleCoords()}"

    fun toGoogleCoords()  = "$latitude,$longitude"
}