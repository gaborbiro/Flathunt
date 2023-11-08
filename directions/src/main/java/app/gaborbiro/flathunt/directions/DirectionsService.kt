package app.gaborbiro.flathunt.directions

import app.gaborbiro.flathunt.directions.model.Destination
import app.gaborbiro.flathunt.directions.model.DirectionsLatLon
import app.gaborbiro.flathunt.directions.model.POIResult

interface DirectionsService {
    fun directions(from: DirectionsLatLon, to: Destination): POIResult
}
