package app.gaborbiro.flathunt.directions

import app.gaborbiro.flathunt.LocalProperties
import app.gaborbiro.flathunt.directions.model.*
import app.gaborbiro.flathunt.request.RequestCaller
import com.google.gson.Gson
import org.koin.core.annotation.Singleton
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
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
    private val gson = Gson()

    override fun route(from: DirectionsLatLon, to: Destination): Route? {
        return when (to) {
            is Destination.Specific -> {
                route(
                    from = from,
                    to = to.location,
                    travelLimits = to.limits,
                )
            }

            is Destination.NearestStation -> {
                getRoutesToNearestStations(from = from, limit = to.limit)
                    .minByOrNull { it.timeMinutes }
            }
        }
    }

    override fun getRoutesToNearestStations(
        from: DirectionsLatLon,
        limit: DirectionsTravelLimit
    ): List<Route> {
        val radius = 5000f / (60f / limit.maxMinutes)
        val url = "https://api.tfl.gov.uk/Stoppoint?" +
                "lat=${from.latitude}" +
                "&lon=${from.longitude}" +
                "&radius=${ceil(radius).toInt()}" +
                "&stoptypes=NaptanMetroStation,NaptanRailStation" +
                "&modes=dlr,overground,tube,tram,national-rail"
        val json = requestCaller.get(url)
        val stops = gson.fromJson(json, TflStopsResponse::class.java).stopPoints
        return stops.mapNotNull {
            val location = DirectionsLatLon(it.lat, it.lon)
            route(
                from = from,
                to = location,
                travelLimits = listOf(limit),
            )
                ?.copy(destinationName = it.commonName)
        }
    }

    private fun route(
        from: DirectionsLatLon,
        to: DirectionsLatLon,
        travelLimits: List<DirectionsTravelLimit>,
    ): Route? {
        val directions: List<Pair<DirectionsTravelLimit, Route>> = travelLimits.mapNotNull { limit ->
            route(from, to, limit)?.let { limit to it }
        }
        val validOnes: List<Route> = directions.filter { (limit, result) ->
            result.timeMinutes <= limit.maxMinutes
        }.map { it.second }

        return if (validOnes.isNotEmpty()) {
            validOnes.minByOrNull { it.timeMinutes }
        } else {
            directions.firstOrNull()?.second
        }
    }

    private fun route(
        from: DirectionsLatLon,
        to: DirectionsLatLon,
        limit: DirectionsTravelLimit,
    ): Route? {
        val departureTime = LocalDateTime.of(LocalDate.now().plus(1L, ChronoUnit.DAYS), LocalTime.NOON)
            .atZone(ZoneId.of("GMT")).toInstant().epochSecond

        fun isReplaceable(step: RouteStep): Boolean {
            return step.travelMode == DirectionsTravelMode.WALKING.value.uppercase() ||
                    step.travelMode == DirectionsTravelMode.TRANSIT.value.uppercase() &&
                    step.transitDetails?.line?.vehicle?.name == "Bus"
        }

        val response = fetchDirections(from, to, limit.mode, departureTime, alternatives = true)
        return if (response.routes.isEmpty()) {
            null
        } else {
            // there might be multiple routes because the 'alternatives' field is set to true
            response.routes.filter {
                // there is only one leg, unless waypoints are specified in the request
                val leg = it.legs[0]
                leg.steps.count { it.travelMode == DirectionsTravelMode.TRANSIT.value.uppercase() } <= 2 // max two changes
            }.map { route ->
                val leg = route.legs[0]
                val totalDurationSecs: Int = leg.duration.value
                val totalDurationMins = TimeUnit.SECONDS.toMinutes(totalDurationSecs.toLong()).toInt()
                val totalDistanceMeters: Int = leg.distance.value
                val totalDistanceKm = totalDistanceMeters / 1000f
                val replacementDirections = if (limit.mode == DirectionsTravelMode.TRANSIT) {
                    val collapsedSteps = leg.steps.fold(mutableListOf<RouteStep>()) { steps, step ->
                        if (steps.isEmpty()) {
                            if (isReplaceable(step)) {
                                steps.add(step.copy(travelMode = DirectionsTravelMode.CYCLING.value.uppercase()))
                            } else {
                                steps.add(step)
                            }
                        } else {
                            val last = steps.lastOrNull()!!
                            if (last.travelMode == DirectionsTravelMode.CYCLING.value.uppercase()) {
                                if (isReplaceable(step)) { // fold it in
                                    steps[steps.size - 1] = last.copy(
                                        endLocation = step.endLocation,
                                        duration = LegDuration(step.duration.value + last.duration.value),
                                        distance = LegDistance(step.distance.value + last.distance.value)
                                    )
                                } else {
                                    steps.add(step)
                                }
                            } else {
                                if (isReplaceable(step)) {
                                    steps.add(step.copy(travelMode = DirectionsTravelMode.CYCLING.value.uppercase()))
                                } else {
                                    steps.add(step)
                                }
                            }
                        }
                        steps
                    }
                    if (collapsedSteps.size == 3
                        && (collapsedSteps[0].travelMode == DirectionsTravelMode.CYCLING.value.uppercase())
                        && collapsedSteps[1].travelMode != DirectionsTravelMode.CYCLING.value.uppercase()
                        && collapsedSteps[2].travelMode == DirectionsTravelMode.CYCLING.value.uppercase()
                    ) {
                        val oldStep1 = collapsedSteps[0]
                        val cyclingLeg1 = fetchDirections(
                            from = oldStep1.startLocation.let { DirectionsLatLon(it.lat, it.lng) },
                            to = oldStep1.endLocation.let { DirectionsLatLon(it.lat, it.lng) },
                            mode = DirectionsTravelMode.CYCLING,
                            departureTime = departureTime,
                            alternatives = false,
                        ).let {
                            if (it.routes.isNotEmpty()) {
                                it.routes[0].legs[0]
                            } else {
                                null
                            }
                        }
                        val oldStep2 = collapsedSteps[2]
                        val cyclingLeg2: RouteLeg? = fetchDirections(
                            from = oldStep2.startLocation.let { DirectionsLatLon(it.lat, it.lng) },
                            to = oldStep2.endLocation.let { DirectionsLatLon(it.lat, it.lng) },
                            mode = DirectionsTravelMode.CYCLING,
                            departureTime = departureTime,
                            alternatives = false,
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
                            Route(
                                transitCount = 1,
                                timeMinutes = replacedTimeMinutes,
                                distanceKm = replacedDistanceMeters / 1000f,
                                replacementTransitData = collapsedSteps[1].transitDetails!!.toString(),
                                mode = DirectionsTravelMode.CYCLING,
                                destination = to,
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
                (replacementDirections ?: Route(
                    transitCount = leg.steps.count { it.travelMode == DirectionsTravelMode.TRANSIT.value.uppercase() },
                    timeMinutes = totalDurationMins,
                    distanceKm = totalDistanceKm,
                    replacementTransitData = null,
                    mode = limit.mode,
                    destination = to,
                ))
            }.minByOrNull { it.timeMinutes }
        }
    }

    private fun fetchDirections(
        from: DirectionsLatLon,
        to: DirectionsLatLon,
        mode: DirectionsTravelMode,
        departureTime: Long,
        alternatives: Boolean,
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
}