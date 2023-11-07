package app.gaborbiro.flathunt.directions

import app.gaborbiro.flathunt.directions.model.Destination
import app.gaborbiro.flathunt.directions.model.DirectionsLatLon
import app.gaborbiro.flathunt.directions.model.DirectionsResult
import app.gaborbiro.flathunt.directions.model.DirectionsTravelLimit

interface DirectionsService {
    fun route(from: DirectionsLatLon, to: Destination): DirectionsResult?

    fun getRoutesToNearestStations(
        from: DirectionsLatLon,
        limit: DirectionsTravelLimit,
        description: String,
    ): List<DirectionsResult>
}
