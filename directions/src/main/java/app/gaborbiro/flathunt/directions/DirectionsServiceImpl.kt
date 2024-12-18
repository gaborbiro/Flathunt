package app.gaborbiro.flathunt.directions

import app.gaborbiro.flathunt.LocalProperties
import app.gaborbiro.flathunt.Quad
import app.gaborbiro.flathunt.console.ConsoleWriter
import app.gaborbiro.flathunt.directions.model.*
import app.gaborbiro.flathunt.request.RequestCaller
import com.google.gson.Gson
import org.koin.core.annotation.Singleton
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.IOException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import kotlin.math.ceil

@Singleton
class DirectionsServiceImpl : DirectionsService, KoinComponent {

    private val requestCaller: RequestCaller by inject()
    private val console: ConsoleWriter by inject()
    private val gson = Gson()

    /**
     * @return best route if there are more than one valid routes, first available route if none of them are valid,
     * first available destination if no routes are available at all, null if no destinations are available at all.
     */
    override fun directions(from: DirectionsLatLon, to: Destination): POIResult {
        return when (to) {
            is Destination.Coordinate -> {
                val destinationStr = when (to) {
                    is Destination.Address -> to.address.replace(" ",  "+")
                    else -> to.location.toGoogleCoords()
                }
                val (limit, route) = directions(
                    description = to.description,
                    from = from,
                    destinationStr = destinationStr,
                    limits = to.limits,
                )
                POIResult(
                    limit = limit,
                    originalDescription = to.description,
                    resolvedDestination = POIDestination(
                        isNearestStation = false,
                        description = to.description,
                        address = destinationStr,
                        location = to.location,
                        limit = limit
                    ),
                    route = route,
                )
            }

            is Destination.NearestStation -> {
                nearestStation(from = from, to = to)
            }
        }
    }

    /**
     * Generate route to all the nearby public transport stations and pick the closest one.
     * If no routes are available to any of them, still return some info about the first station.
     * If no stations are available at all, return null.
     */
    private fun nearestStation(
        from: DirectionsLatLon,
        to: Destination.NearestStation,
    ): POIResult {
        val radius = 5000f / (60f / to.limit.maxMinutes)
        val url = "https://api.tfl.gov.uk/Stoppoint?" +
                "lat=${from.latitude}" +
                "&lon=${from.longitude}" +
                "&radius=${ceil(radius).toInt()}" +
                "&stoptypes=NaptanMetroStation,NaptanRailStation" +
                "&modes=dlr,overground,tube,tram,national-rail"
        val json = requestCaller.get(url)
        val stops = gson.fromJson(json, TflStopsResponse::class.java).stopPoints
        val route = stops.mapIndexed { index, stop ->
            val resolvedLocation = DirectionsLatLon(stop.lat, stop.lon)
            val (_, route) = directions(
                description = stop.commonName,
                from = from,
                destinationStr = resolvedLocation.toGoogleCoords(),
                limits = listOf(to.limit),
            )
            Quad(route, stop.commonName, resolvedLocation, index)
        }.minByOrNull { it.first?.timeMinutes ?: (it.fourth * 10000) }

        return route
            ?.let { (route, name, location) ->
                POIResult(
                    originalDescription = to.description,
                    limit = to.limit,
                    resolvedDestination = POIDestination(
                        isNearestStation = true,
                        description = name,
                        address = location.toGoogleCoords(),
                        location = location,
                        limit = to.limit,
                    ),
                    route = route,
                )
            }
            ?: POIResult(
                originalDescription = to.description,
                limit = to.limit,
                resolvedDestination = null,
                route = null,
            )
    }

    /**
     * Try to get a route for each of the specified limits and return the best one. If nothing meets the limit,
     * return the first route anyway (corresponding to the first specified limit) so that the user can follow up.
     */
    private fun directions(
        description: String,
        from: DirectionsLatLon,
        destinationStr: String,
        limits: List<DirectionsTravelLimit>,
    ): Pair<DirectionsTravelLimit, Route?> {
        if (limits.isEmpty()) throw IllegalArgumentException("Must specify at least one limit")

        val resultMap: List<Pair<DirectionsTravelLimit, Route?>> = limits.map { limit ->
            limit to directions(
                from = from,
                description = description,
                destinationStr = destinationStr,
                limit = limit
            )
        }
        val validOnes: List<Pair<DirectionsTravelLimit, Route?>> = resultMap
            .filter { (limit, route) ->
                route != null && route.timeMinutes <= limit.maxMinutes
            }

        return if (validOnes.isNotEmpty()) {
            validOnes.minBy { (_, route) ->
                route!!.timeMinutes
            }
        } else {
            resultMap.find { it.first == limits[0] }!!
        }
    }

    /**
     * @return DirectionsResult with the shortest non-null route if the Google API returned any solution,
     * null otherwise
     */
    private fun directions(
        from: DirectionsLatLon,
        description: String,
        destinationStr: String,
        limit: DirectionsTravelLimit,
    ): Route? {
        val departureTime = LocalDateTime.of(LocalDate.now().plus(1L, ChronoUnit.DAYS), LocalTime.NOON)
            .atZone(ZoneId.systemDefault()).toInstant().epochSecond

        fun isReplaceable(step: RouteStep): Boolean {
            return step.travelMode == DirectionsTravelMode.WALKING.value.uppercase() ||
                    step.travelMode == DirectionsTravelMode.TRANSIT.value.uppercase() &&
                    step.transitDetails?.line?.vehicle?.name == "Bus"
        }

        val response = fetchGoogleDirections(from, destinationStr, limit.mode, departureTime, alternatives = true)
        return if (response.routes.isEmpty()) {
            if (response.errorMessage != null) {
                console.e(response.errorMessage)
            }
            throw IOException("Google API responded with ${response.status}")
        } else {
            // there might be multiple routes because the 'alternatives' field is set to true
            response.routes.filter {
                // there is only one leg, unless waypoints are specified in the request
                val leg = it.legs[0]
                leg.steps.count { it.travelMode == DirectionsTravelMode.TRANSIT.value.uppercase() } <= 2 // max one change
            }.map { directionsRoute ->
                val leg = directionsRoute.legs[0]
                val totalDurationSecs: Int = leg.duration.value
                val totalDurationMins = TimeUnit.SECONDS.toMinutes(totalDurationSecs.toLong()).toInt()
                val totalDistanceMeters: Int = leg.distance.value
                val totalDistanceKm = totalDistanceMeters / 1000f
//                val replacementDirections = if (limit.mode == DirectionsTravelMode.TRANSIT) {
//                    val collapsedSteps = leg.steps.fold(mutableListOf<RouteStep>()) { steps, step ->
//                        if (steps.isEmpty()) {
//                            if (isReplaceable(step)) {
//                                steps.add(step.copy(travelMode = DirectionsTravelMode.CYCLING.value.uppercase()))
//                            } else {
//                                steps.add(step)
//                            }
//                        } else {
//                            val last = steps.lastOrNull()!!
//                            if (last.travelMode == DirectionsTravelMode.CYCLING.value.uppercase()) {
//                                if (isReplaceable(step)) { // fold it in
//                                    steps[steps.size - 1] = last.copy(
//                                        endLocation = step.endLocation,
//                                        duration = LegDuration(step.duration.value + last.duration.value),
//                                        distance = LegDistance(step.distance.value + last.distance.value)
//                                    )
//                                } else {
//                                    steps.add(step)
//                                }
//                            } else {
//                                if (isReplaceable(step)) {
//                                    steps.add(step.copy(travelMode = DirectionsTravelMode.CYCLING.value.uppercase()))
//                                } else {
//                                    steps.add(step)
//                                }
//                            }
//                        }
//                        steps
//                    }
//                    if (collapsedSteps.size == 3
//                        && (collapsedSteps[0].travelMode == DirectionsTravelMode.CYCLING.value.uppercase())
//                        && collapsedSteps[1].travelMode != DirectionsTravelMode.CYCLING.value.uppercase()
//                        && collapsedSteps[2].travelMode == DirectionsTravelMode.CYCLING.value.uppercase()
//                    ) {
//                        val oldStep1 = collapsedSteps[0]
//                        val cyclingLeg1 = fetchDirections(
//                            from = oldStep1.startLocation.let { DirectionsLatLon(it.lat, it.lng) },
//                            to = oldStep1.endLocation.let { DirectionsLatLon(it.lat, it.lng) },
//                            mode = DirectionsTravelMode.CYCLING,
//                            departureTime = departureTime,
//                            alternatives = false,
//                        ).let {
//                            if (it.routes.isNotEmpty()) {
//                                it.routes[0].legs[0]
//                            } else {
//                                null
//                            }
//                        }
//                        val oldStep2 = collapsedSteps[2]
//                        val cyclingLeg2: RouteLeg? = fetchDirections(
//                            from = oldStep2.startLocation.let { DirectionsLatLon(it.lat, it.lng) },
//                            to = oldStep2.endLocation.let { DirectionsLatLon(it.lat, it.lng) },
//                            mode = DirectionsTravelMode.CYCLING,
//                            departureTime = departureTime,
//                            alternatives = false,
//                        ).let {
//                            if (it.routes.isNotEmpty()) {
//                                it.routes[0].legs[0]
//                            } else {
//                                null
//                            }
//                        }
//                        // It is possible that google doesn't return a cycling alternative to the start/end walking leg,
//                        // because it is too short of a distance. We can just fall back on the original walking step, as
//                        // the time saving wouldn't be significant anyway with a bicycle.
//                        val replacedTimeSeconds =
//                            (cyclingLeg1?.duration ?: oldStep1.duration).value +
//                                    (cyclingLeg2?.duration ?: oldStep2.duration).value +
//                                    leg.steps[1].duration.value
//                        val replacedTimeMinutes = TimeUnit.SECONDS.toMinutes(replacedTimeSeconds.toLong()).toInt()
//                        if (replacedTimeMinutes < totalDurationMins) {
//                            val replacedDistanceMeters =
//                                (cyclingLeg1?.distance ?: oldStep1.distance).value +
//                                        (cyclingLeg2?.distance ?: oldStep2.distance).value +
//                                        leg.steps[1].distance.value
//                            Route(
//                                transitCount = 1,
//                                timeMinutes = replacedTimeMinutes,
//                                distanceKm = replacedDistanceMeters / 1000f,
//                                replacementTransitData = collapsedSteps[1].transitDetails!!.toString(),
//                                mode = DirectionsTravelMode.CYCLING,
//                                destination = to,
//                            )
//                        } else {
//                            null
//                        }
//                    } else {
//                        null
//                    }
//                } else {
//                    null
//                }
//                replacementDirections ?:
                Route(
                    description = description,
                    transitCount = leg.steps.count { it.travelMode == DirectionsTravelMode.TRANSIT.value.uppercase() },
                    timeMinutes = totalDurationMins,
                    distanceKm = totalDistanceKm,
                    replacementTransitData = null,
                    limit = limit,
                )
            }.minByOrNull { it.timeMinutes }
        }
    }

    private fun fetchGoogleDirections(
        from: DirectionsLatLon,
        destinationStr: String,
        mode: DirectionsTravelMode,
        departureTime: Long,
        alternatives: Boolean,
    ): DirectionsResponse {
        val url = "https://maps.googleapis.com/maps/api/directions/json?" +
                "origin=${from.toGoogleCoords()}" +
                "&" +
                "destination=$destinationStr" +
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
}