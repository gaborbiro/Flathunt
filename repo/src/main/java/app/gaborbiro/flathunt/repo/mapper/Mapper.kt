package app.gaborbiro.flathunt.repo.mapper

import app.gaborbiro.flathunt.LocalProperties
import app.gaborbiro.flathunt.criteria.POI
import app.gaborbiro.flathunt.criteria.POITravelLimit
import app.gaborbiro.flathunt.criteria.POITravelMode
import app.gaborbiro.flathunt.data.domain.model.Property
import app.gaborbiro.flathunt.data.domain.model.PropertyLatLon
import app.gaborbiro.flathunt.directions.model.*
import org.koin.core.annotation.Singleton
import org.koin.core.component.KoinComponent

@Singleton
internal class Mapper : KoinComponent {

    fun map(poi: POI): Destination {
        return when (poi) {
            is POI.Destination -> {
                Destination.Specific(
                    location = DirectionsLatLon(poi.latitude, poi.longitude),
                    limits = poi.max.map(::map)
                )
            }

            is POI.NearestRailStation -> {
                Destination.NearestStation(map(poi.max[0]))
            }
        }
    }

    fun map(propertyLocation: PropertyLatLon): DirectionsLatLon {
        return DirectionsLatLon(
            latitude = propertyLocation.latitude,
            longitude = propertyLocation.longitude,
        )
    }

    fun map(limit: POITravelLimit): DirectionsTravelLimit {
        return DirectionsTravelLimit(
            mode = when (limit.mode) {
                POITravelMode.TRANSIT -> DirectionsTravelMode.TRANSIT
                POITravelMode.CYCLING -> DirectionsTravelMode.CYCLING
                POITravelMode.WALKING -> DirectionsTravelMode.WALKING
            },
            maxMinutes = limit.maxMinutes
        )
    }

    fun map(mode: DirectionsTravelMode): POITravelMode {
        return when (mode) {
            DirectionsTravelMode.TRANSIT -> POITravelMode.TRANSIT
            DirectionsTravelMode.CYCLING -> POITravelMode.CYCLING
            DirectionsTravelMode.WALKING -> POITravelMode.WALKING
        }
    }

    fun mapLinks(property: Property, routes: Map<POI, Route?>): List<String> {
        val urls = mutableListOf<String>()

        property.messageUrl?.let(urls::add)

        property.location?.toURL()?.let(urls::add)

        routes.forEach { (poi, route) ->
            if (route != null) {
                val destination = when (poi) {
                    is POI.Destination -> route.destination.toGoogleCoords()
                    is POI.NearestRailStation -> escapeHTML(route.destinationName!!)
                }
                val url = "https://www.google.com/maps/dir/?api=1" +
                        "&origin=${property.location?.toGoogleCoords()}" +
                        "&destination=$destination" +
//                    (it.placeId?.let { "&destination_place_id=$it" } ?: "") +
                        "&travelmode=${route.mode.value}"
                urls.add(url)
            }
        }

        property.location?.let {
            urls.add("https://www.google.com/maps/@?api=1&map_action=pano&viewpoint=${it.toGoogleCoords()}&heading=13&pitch=0&fov=80")
        }
        return urls
    }

    fun mapStaticMap(propertyLocation: PropertyLatLon, routes: Map<POI, Route?>): String {
        val nearestStations =
            routes.filter { it.key is POI.NearestRailStation }.mapNotNull { (_, route) ->
                if (route != null) {
                    "&markers=size:mid|color:green|${route.destination.toGoogleCoords()}"
                } else null
            }
        val pois = routes.map { it.value }.filterIsInstance<POI.Destination>().map {
            val coords = DirectionsLatLon(it.latitude, it.longitude).toGoogleCoords()
            "&markers=size:mid|color:blue|$coords"
        }
        return "http://maps.googleapis.com/maps/api/staticmap?" +
                "&size=600x400" +
                "&markers=size:mid|color:red|${propertyLocation.toGoogleCoords()}" +
                (nearestStations.joinToString("")) +
                pois.joinToString("") +
                "&key=${LocalProperties.googleApiKey}".replace(",", "%2C")
    }

    fun mapCommuteScore(routes: Map<POI, Route?>): Int {
        val eligibleRoutes: List<Route> =
            routes.mapNotNull { it.value }.filter { it.mode != DirectionsTravelMode.WALKING }
        return if (eligibleRoutes.isNotEmpty()) eligibleRoutes.map { it.timeMinutes }.average()
            .toInt() else Int.MAX_VALUE
    }

    private fun escapeHTML(s: String): String {
        val out = StringBuilder(16.coerceAtLeast(s.length))
        for (element in s) {
            if (element.code > 127 || element == '"' || element == '\'' || element == '<' || element == '>' || element == '&') {
                out.append("&#")
                out.append(element.code)
                out.append(';')
            } else {
                out.append(element)
            }
        }
        return out.toString()
    }

}