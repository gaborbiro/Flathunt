package app.gaborbiro.flathunt.directions.model

enum class DirectionsTravelMode(val value: String, val description: String) {
    TRANSIT("transit", "commute"),
    CYCLING("bicycling", "cycling"),
    WALKING("walking", "walk")
}