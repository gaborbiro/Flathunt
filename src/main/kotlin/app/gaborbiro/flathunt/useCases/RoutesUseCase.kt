package app.gaborbiro.flathunt.useCases

import app.gaborbiro.flathunt.GlobalVariables
import app.gaborbiro.flathunt.ValidationCriteria
import app.gaborbiro.flathunt.data.Store
import app.gaborbiro.flathunt.service.Service

class RoutesUseCase(service: Service, private val store: Store, criteria: ValidationCriteria) :
    BaseUseCase(service, store, criteria) {

    override val commands: List<Command<*>>
        get() = listOf(routes)

    private val routes = command(
        command = "routes",
        description = "(Re)calculates all routes for all properties in the database. " +
                "Feel free to tweak any of your validation criteria in Setup.kt before running this command."
    )
    {
        fetchRoutes(store.getProperties(), GlobalVariables.safeMode)
        println("Finished")
    }
}