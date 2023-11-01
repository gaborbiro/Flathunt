package app.gaborbiro.flathunt.directions.model

class DirectionsTravelLimit(
    val mode: DirectionsTravelMode,
    val maxMinutes: Int,
) {
    override fun toString(): String {
        return "max $maxMinutes minutes of ${mode.description}"
    }
}
