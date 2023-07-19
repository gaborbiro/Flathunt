package app.gaborbiro.flathunt.usecase.base

import app.gaborbiro.flathunt.*
import app.gaborbiro.flathunt.data.domain.Store
import app.gaborbiro.flathunt.data.domain.model.PersistedProperty
import app.gaborbiro.flathunt.data.domain.model.Property
import app.gaborbiro.flathunt.data.domain.model.checkValid
import app.gaborbiro.flathunt.google.calculateRoutes
import app.gaborbiro.flathunt.repo.domain.PropertyRepository
import app.gaborbiro.flathunt.service.domain.Service
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

abstract class BaseUseCase : UseCase, KoinComponent {

    protected fun String.checkLastUsedIndexOrId(): String {
        return GlobalVariables.lastUsedIndexOrId?.let {
            if (this == "$") {
                GlobalVariables.lastUsedIndexOrId
            } else {
                this
            }
        } ?: this
    }

    private fun openUrl(url: String) {
        Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler $url").waitFor()
    }
}