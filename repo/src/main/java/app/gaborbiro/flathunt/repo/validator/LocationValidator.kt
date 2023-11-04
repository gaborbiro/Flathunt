package app.gaborbiro.flathunt.repo.validator

import app.gaborbiro.flathunt.console.ConsoleWriter
import app.gaborbiro.flathunt.criteria.POI
import app.gaborbiro.flathunt.directions.model.Route
import app.gaborbiro.flathunt.repo.mapper.Mapper
import org.koin.core.annotation.Singleton
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@Singleton
internal class LocationValidator : KoinComponent {

    private val mapper: Mapper by inject()
    private val console: ConsoleWriter by inject()

    fun isValid(routes: Map<POI, Route?>): Boolean {
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
    private fun validate(routes: Map<POI, Route?>): List<String> {
        val errors = mutableListOf<String>()
        routes
            .filterValues { it == null }
            .toList()
            .map { it.first.description }
            .forEach {
                errors.add("missing route to: $it")
            }
        routes
            .forEach { (poi, route) ->
                if (route != null) {
                    val poiTravelMode = mapper.map(route.mode)

                    if (route.timeMinutes > poi.max.find { it.mode == poiTravelMode }!!.maxMinutes) {
                        errors.add("${poi.description} is too far: best time is ${route.timeMinutes} minutes ${route.mode.description}")
                    }
                }
            }
        return errors
    }
}