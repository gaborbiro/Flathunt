package app.gaborbiro.flathunt.directions

import app.gaborbiro.flathunt.POI


interface DirectionsService {
    fun calculateRoutes(from: DirectionsLatLon, targets: Collection<POI>): List<Route>

    fun getRoutesToNearestStations(from: DirectionsLatLon): List<Route>
}
