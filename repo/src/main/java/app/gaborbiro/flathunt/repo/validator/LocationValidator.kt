package app.gaborbiro.flathunt.repo.validator

import app.gaborbiro.flathunt.console.ConsoleWriter
import app.gaborbiro.flathunt.criteria.POI
import app.gaborbiro.flathunt.directions.model.DirectionsResult
import org.koin.core.annotation.Singleton
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@Singleton
internal class LocationValidator : KoinComponent {

    private val console: ConsoleWriter by inject()

    fun isValid(routes: Map<POI, DirectionsResult?>): Boolean {
        val errors = validate(routes)
        return if (errors.isEmpty()) {
            true
        } else {
            console.d("\nRejected:\n${errors.joinToString("\n")}")
            false
        }
    }

    /**
     * All POI's must be satisfied
     */
    private fun validate(routes: Map<POI, DirectionsResult?>): List<String> {
        val errors = mutableListOf<String>()
        routes
            .filterValues { it == null }
            .toList()
            .map { it.first.description }
            .forEach {
                errors.add("missing route to: $it")
            }
        routes
            .forEach { (_, route) ->
                if (route != null) {
                    val maxMinutes = route.destination.limits.find { it.mode == route.mode }!!.maxMinutes
                    if (route.route.timeMinutes > maxMinutes) {
                        errors.add("${route.destination.description} is too far: best time is ${route.route.timeMinutes} minutes ${route.mode.description} (max is $maxMinutes minutes)")
                    }
                }
            }
        return errors
    }
}