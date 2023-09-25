package app.gaborbiro.flathunt.usecase

import app.gaborbiro.flathunt.console.ConsoleWriter
import app.gaborbiro.flathunt.repo.domain.RoutesRepository
import app.gaborbiro.flathunt.usecase.base.BaseUseCase
import app.gaborbiro.flathunt.usecase.base.Command
import app.gaborbiro.flathunt.usecase.base.command
import org.koin.core.component.inject

class RoutesUseCase : BaseUseCase() {

    private val routesRepository: RoutesRepository by inject()
    private val console: ConsoleWriter by inject()

    override val commands: List<Command<*>>
        get() = listOf(routes)

    private val routes = command(
        command = "routes",
        description = "(Re)calculates all routes for all properties in the database. " +
                "Feel free to tweak any of your validation criteria in Setup.kt before running this command."
    )
    {
        routesRepository.validateByRoutes()
        console.d("Finished calculating routes")
    }
}