package app.gaborbiro.flathunt.repo.mapper

import app.gaborbiro.flathunt.LocalProperties
import app.gaborbiro.flathunt.criteria.POI
import app.gaborbiro.flathunt.criteria.POILocation
import app.gaborbiro.flathunt.criteria.POITravelLimit
import app.gaborbiro.flathunt.criteria.POITravelMode
import app.gaborbiro.flathunt.data.domain.model.Property
import app.gaborbiro.flathunt.data.domain.model.PropertyLatLon
import app.gaborbiro.flathunt.directions.model.*
import org.koin.core.annotation.Singleton
import org.koin.core.component.KoinComponent

@Singleton
internal class Mapper : KoinComponent {

    fun map(poi: POI, propertyLocation: PropertyLatLon): Destination {
        return when (poi) {
            is POI.CoordinateSet -> {
                val closestLocation: POI.Address = closest(poi.locations, propertyLocation)
                Destination.Address(
                    description = closestLocation.description,
                    location = map(closestLocation.location),
                    address = closestLocation.address,
                    limits = poi.max.map(::map),
                )
            }

            is POI.Address -> Destination.Address(
                description = poi.description,
                location = map(poi.location),
                address = poi.address,
                limits = poi.max.map(::map),
            )

            is POI.Coordinate -> Destination.Coordinate(
                description = poi.description,
                location = map(poi.location),
                limits = poi.max.map(::map),
            )

            is POI.NearestRailStation -> {
                Destination.NearestStation(
                    description = poi.description,
                    limit = map(poi.max[0]),
                )
            }
        }
    }

    private fun closest(locations: List<POI.Address>, target: PropertyLatLon): POI.Address {
        if (locations.isEmpty()) throw IllegalAccessException("locations should not be empty")
        val targetLatitude = target.latitude.toDouble()
        val targetLongitude = target.longitude.toDouble()
        return locations.minBy {
            distance(
                latitude1 = it.location.latitude.toDouble(),
                longitude1 = it.location.longitude.toDouble(),
                latitude2 = targetLatitude,
                longitude2 = targetLongitude,
            )
        }
    }

    private fun map(location: POILocation): DirectionsLatLon {
        return DirectionsLatLon(
            latitude = location.latitude,
            longitude = location.longitude,
        )
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

    fun mapLinks(property: Property, poiResults: Collection<POIResult>): List<String> {
        val urls = mutableListOf<String>()

//        property.location?.toURL()?.let(urls::add)

        poiResults.mapNotNull { it.resolvedDestination }.forEach { destination ->
            val url = "https://www.google.com/maps/dir/?api=1" +
                    "&origin=${property.location?.toGoogleCoords()}" +
                    "&destination=${destination.address}" +
//                    (it.placeId?.let { "&destination_place_id=$it" } ?: "") +
                    "&travelmode=${destination.limit.mode.value}"
            urls.add(url)
        }

        property.location?.let {
            urls.add("https://www.google.com/maps/@?api=1&map_action=pano&viewpoint=${it.toGoogleCoords()}&heading=13&pitch=0&fov=80")
        }
        property.messageUrl?.let(urls::add)
        return urls
    }

    fun mapStaticMap(propertyLocation: PropertyLatLon, poiResults: Collection<POIResult>): String {
        val nearestStations = poiResults
            .mapNotNull { it.resolvedDestination }
            .filter { it.isNearestStation }
            .map { destination ->
                "&markers=size:mid|color:green|${destination.address}"
            }
        val pois = poiResults
            .mapNotNull { it.resolvedDestination }
            .filter { it.isNearestStation.not() }
            .map { destination ->
                "&markers=size:mid|color:blue|${destination.address}"
            }
        return "http://maps.googleapis.com/maps/api/staticmap?" +
                "&size=600x400" +
                "&markers=size:mid|color:red|${propertyLocation.toGoogleCoords()}" +
                (nearestStations.joinToString("")) +
                pois.joinToString("") +
                "&key=${LocalProperties.googleApiKey}".replace(",", "%2C")
    }

    fun mapCommuteScore(poiResults: Collection<POIResult>): Int {
        var score: Int? = null
        if (poiResults.all { it.route != null }) {
            score = poiResults.mapNotNull { it.route?.timeMinutes }.average().toInt()
        }
        if (score == null) {
            score = 1000 + (poiResults.size - poiResults.count { it.resolvedDestination != null })
        }
        return score
    }
}