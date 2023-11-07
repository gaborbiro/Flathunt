package app.gaborbiro.flathunt.repo.mapper

import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin

fun distance(
    latitude1: Double,
    longitude1: Double,
    latitude2: Double,
    longitude2: Double,
    unit: Unit = Unit.Kilometers
): Double {
    val theta = longitude1 - longitude2
    var dist =
        sin(latitude1.deg2rad()) * sin(latitude2.deg2rad()) + cos(latitude1.deg2rad()) * cos(latitude2.deg2rad()) * cos(
            theta.deg2rad()
        )
    dist = acos(dist).rad2deg()
    return when (unit) {
        Unit.Kilometers -> dist * 1.609344
        Unit.NauticalMiles -> dist * 0.8684
        Unit.Miles -> dist
    } * 60.0 * 1.1515
}

fun Double.deg2rad() = this * Math.PI / 180.0
fun Double.rad2deg() = this * 180.0 / Math.PI

enum class Unit {
    Miles, Kilometers, NauticalMiles
}