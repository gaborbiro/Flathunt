package app.gaborbiro.flathunt.directions

import app.gaborbiro.flathunt.directions.model.Destination
import app.gaborbiro.flathunt.directions.model.DirectionsLatLon
import app.gaborbiro.flathunt.directions.model.Route
import app.gaborbiro.flathunt.directions.model.DirectionsTravelLimit

interface DirectionsService {
    fun route(from: DirectionsLatLon, to: Destination): Route?

    fun getRoutesToNearestStations(from: DirectionsLatLon, limit: DirectionsTravelLimit): List<Route>
}
