package app.gaborbiro.flathunt.google

import app.gaborbiro.flathunt.*
import app.gaborbiro.flathunt.request.RequestCaller
import com.google.gson.Gson
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import kotlin.math.ceil

fun calculateRoutes(location: LatLon?, pois: Collection<POI>, requestCaller: RequestCaller): List<Route> {
    return location?.let {
        pois.mapNotNull { poi ->
            when (poi) {
                is POI.Destination -> {
                    getDirectionsTo(
                        from = location,
                        to = poi.coordinates,
                        travelLimits = poi.max,
                        requestCaller,
                    )?.let { (travelOption, direction) ->
                        Route(
                            description = poi.description,
                            travelLimit = travelOption,
                            transitCount = direction.transitCount,
                            timeMinutes = direction.timeMinutes,
                            distanceKm = direction.distanceKm,
                            replacementTransitData = direction.replacementTransitData,
                            name = poi.name,
                            coordinates = poi.coordinates,
                        )
                    }
                }

                is POI.NearestRailStation -> {
                    getRoutesToNearestStations(from = location, requestCaller).minByOrNull { it.timeMinutes }
                }
            }
        }
    } ?: emptyList()
}

private val gson = Gson()

private fun getDirectionsTo(
    from: LatLon,
    to: LatLon,
    travelLimits: Array<out TravelLimit>,
    requestCaller: RequestCaller,
): Pair<TravelLimit, DirectionsResult>? {
    val departureTime = LocalDateTime.of(LocalDate.now().plus(1L, ChronoUnit.DAYS), LocalTime.NOON)
        .atZone(ZoneId.of("GMT")).toInstant().epochSecond
    val directions: List<Pair<TravelLimit, DirectionsResult>> = travelLimits.mapNotNull { limit ->
        val response = fetchDirections(from, to, limit.mode, departureTime, alternatives = true, requestCaller)
        if (response.routes.isEmpty()) {
            null
        } else {
            // there might be multiple routes because the 'alternatives' field is set to true
            response.routes.filter {
                // there is only one leg, unless waypoints are specified in the request
                val leg = it.legs[0]
                leg.steps.count { it.travelMode == TravelMode.TRANSIT.value.uppercase() } <= 2 // max two changes
            }.map { route ->
                val leg = route.legs[0]
                val totalDurationSecs: Int = leg.duration.value
                val totalDurationMins = TimeUnit.SECONDS.toMinutes(totalDurationSecs.toLong()).toInt()
                val totalDistanceMeters: Int = leg.distance.value
                val totalDistanceKm = totalDistanceMeters / 1000f
                val replacementDirections = if (limit.mode == TravelMode.TRANSIT) {
                    val collapsedSteps = leg.steps.fold(mutableListOf<RouteStep>()) { steps, step ->
                        if (steps.isEmpty()) {
                            if (step.replaceable()) {
                                steps.add(step.copy(travelMode = TravelMode.CYCLING.value.uppercase()))
                            } else {
                                steps.add(step)
                            }
                        } else {
                            val last = steps.lastOrNull()!!
                            if (last.travelMode == TravelMode.CYCLING.value.uppercase()) {
                                if (step.replaceable()) { // fold it in
                                    steps[steps.size - 1] = last.copy(
                                        endLocation = step.endLocation,
                                        duration = LegDuration(step.duration.value + last.duration.value),
                                        distance = LegDistance(step.distance.value + last.distance.value)
                                    )
                                } else {
                                    steps.add(step)
                                }
                            } else {
                                if (step.replaceable()) {
                                    steps.add(step.copy(travelMode = TravelMode.CYCLING.value.uppercase()))
                                } else {
                                    steps.add(step)
                                }
                            }
                        }
                        steps
                    }
                    if (collapsedSteps.size == 3
                        && (collapsedSteps[0].travelMode == TravelMode.CYCLING.value.uppercase())
                        && collapsedSteps[1].travelMode != TravelMode.CYCLING.value.uppercase()
                        && collapsedSteps[2].travelMode == TravelMode.CYCLING.value.uppercase()
                    ) {
                        val oldStep1 = collapsedSteps[0]
                        val cyclingLeg1 = fetchDirections(
                            from = oldStep1.startLocation.let { LatLon(it.lat, it.lng) },
                            to = oldStep1.endLocation.let { LatLon(it.lat, it.lng) },
                            mode = TravelMode.CYCLING,
                            departureTime = departureTime,
                            alternatives = false,
                            requestCaller,
                        ).let {
                            if (it.routes.isNotEmpty()) {
                                it.routes[0].legs[0]
                            } else {
                                null
                            }
                        }
                        val oldStep2 = collapsedSteps[2]
                        val cyclingLeg2: RouteLeg? = fetchDirections(
                            from = oldStep2.startLocation.let { LatLon(it.lat, it.lng) },
                            to = oldStep2.endLocation.let { LatLon(it.lat, it.lng) },
                            mode = TravelMode.CYCLING,
                            departureTime = departureTime,
                            alternatives = false,
                            requestCaller,
                        ).let {
                            if (it.routes.isNotEmpty()) {
                                it.routes[0].legs[0]
                            } else {
                                null
                            }
                        }
                        // It is possible that google doesn't return a cycling alternative to the start/end walking leg,
                        // because it is too short of a distance. We can just fall back on the original walking step, as
                        // the time saving wouldn't be significant anyway with a bicycle.
                        val replacedTimeSeconds =
                            (cyclingLeg1?.duration ?: oldStep1.duration).value +
                                    (cyclingLeg2?.duration ?: oldStep2.duration).value +
                                    leg.steps[1].duration.value
                        val replacedTimeMinutes = TimeUnit.SECONDS.toMinutes(replacedTimeSeconds.toLong()).toInt()
                        if (replacedTimeMinutes < totalDurationMins) {
                            val replacedDistanceMeters =
                                (cyclingLeg1?.distance ?: oldStep1.distance).value +
                                        (cyclingLeg2?.distance ?: oldStep2.distance).value +
                                        leg.steps[1].distance.value
                            DirectionsResult(
                                transitCount = 1,
                                timeMinutes = replacedTimeMinutes,
                                distanceKm = replacedDistanceMeters / 1000f,
                                replacementTransitData = collapsedSteps[1].transitDetails!!.toString(),
                            )
                        } else {
                            null
                        }
                    } else {
                        null
                    }
                } else {
                    null
                }
                limit to (replacementDirections ?: DirectionsResult(
                    transitCount = leg.steps.count { it.travelMode == TravelMode.TRANSIT.value.uppercase() },
                    timeMinutes = totalDurationMins,
                    distanceKm = totalDistanceKm,
                    replacementTransitData = null,
                ))
            }.minByOrNull { it.second.timeMinutes }
        }
    }
    val validOnes = directions.filter { (limit, result) ->
        result.timeMinutes <= limit.maxMinutes
    }
    return if (validOnes.isNotEmpty()) {
        validOnes.minByOrNull { it.second.timeMinutes }
    } else {
        directions.firstOrNull()
    }
}

private fun RouteStep.replaceable() = travelMode == TravelMode.WALKING.value.uppercase() ||
        travelMode == TravelMode.TRANSIT.value.uppercase() &&
        transitDetails?.line?.vehicle?.name == "Bus"

private fun fetchDirections(
    from: LatLon,
    to: LatLon,
    mode: TravelMode,
    departureTime: Long,
    alternatives: Boolean,
    requestCaller: RequestCaller,
): DirectionsResponse {
    val url = "https://maps.googleapis.com/maps/api/directions/json?" +
            "origin=${from.toGoogleCoords()}" +
            "&" +
            "destination=${to.toGoogleCoords()}" +
            "&" +
            "mode=${mode.value}" +
            "&" +
            "departure_time=$departureTime" +
            "&" +
//            "transit_mode=rail" +
//            "&" +
            (if (alternatives) {
                "alternatives=true" +
                        "&"
            } else "") +
            "key=${LocalProperties.googleApiKey}"
//    if (GlobalVariables.debug) {
//        println(); println(url)
//    }
    val json = requestCaller.get(url)
    return gson.fromJson(json, DirectionsResponse::class.java)
}

fun getRoutesToNearestStations(from: LatLon, requestCaller: RequestCaller): List<Route> {
    val radius = 5000f / (60f / POI.NearestRailStation.max[0].maxMinutes)
    val url = "https://api.tfl.gov.uk/Stoppoint?" +
            "lat=${from.latitude}" +
            "&lon=${from.longitude}" +
            "&radius=${ceil(radius).toInt()}" +
            "&stoptypes=NaptanMetroStation,NaptanRailStation" +
            "&modes=dlr,overground,tube,tram,national-rail"
    val json = requestCaller.get(url)
    val stops = gson.fromJson(json, TflStopsResponse::class.java).stopPoints
    return stops.mapNotNull {
        val location = LatLon(it.lat, it.lon)
        getDirectionsTo(
            from = from,
            to = location,
            travelLimits = POI.NearestRailStation.max,
            requestCaller,
        )?.let { (travelOption, direction) ->
            Route(
                description = POI.NearestRailStation.description,
                travelLimit = travelOption,
                transitCount = direction.transitCount,
                timeMinutes = direction.timeMinutes,
                distanceKm = direction.distanceKm,
                replacementTransitData = null,
                name = it.commonName,
                coordinates = location,
            )
        }
    }
}
