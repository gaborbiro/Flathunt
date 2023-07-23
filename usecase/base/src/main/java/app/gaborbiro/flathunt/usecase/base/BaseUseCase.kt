package app.gaborbiro.flathunt.usecase.base

import app.gaborbiro.flathunt.GlobalVariables
import org.koin.core.component.KoinComponent

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

//    private fun openUrl(url: String) {
//        Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler $url").waitFor()
//    }
}