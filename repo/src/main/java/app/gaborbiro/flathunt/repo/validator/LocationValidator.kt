package app.gaborbiro.flathunt.repo.validator

import app.gaborbiro.flathunt.console.ConsoleWriter
import app.gaborbiro.flathunt.directions.model.POIResult
import org.koin.core.annotation.Singleton
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@Singleton
internal class LocationValidator : KoinComponent {

    private val console: ConsoleWriter by inject()

    fun isValid(poiResults: Collection<POIResult>): Boolean {
        val errors = validate(poiResults)
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
    private fun validate(poiResults: Collection<POIResult>): List<String> {
        val missingDestinationErrors = poiResults
            .mapNotNull {
                if (it.resolvedDestination == null) {
                    it.originalDescription
                } else {
                    null
                }
            }
            .map {
                "missing destination to: $it"
            }
        val missingRouteErrors = poiResults
            .mapNotNull {
                if (it.route == null) {
                    it.resolvedDestination?.description
                } else {
                    null
                }
            }
            .map {
                "missing route to: $it"
            }
        val routeErrors = poiResults
            .mapNotNull { it.route }
            .mapNotNull { route ->
                val maxMinutes = route.limit.maxMinutes
                if (route.timeMinutes > maxMinutes) {
                    "${route.description} is too far: best time is ${route.timeMinutes} minutes ${route.limit.mode.description} (max is $maxMinutes minutes)"
                } else {
                    null
                }
            }
        return missingDestinationErrors + missingRouteErrors + routeErrors
    }
}