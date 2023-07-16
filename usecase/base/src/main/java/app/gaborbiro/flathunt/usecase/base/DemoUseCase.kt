package app.gaborbiro.flathunt.usecase.base

import app.gaborbiro.flathunt.data.Store
import app.gaborbiro.flathunt.service.Service

class DemoUseCase(private val service: Service, private val store: Store, criteria: ValidationCriteria) :
    BaseUseCase(service, store, criteria) {

    override val commands
        get() = listOf(demo)

    private val demo = command(
        command = "demo",
        description = "demo",
    )
    {
        service.login()
    }
}