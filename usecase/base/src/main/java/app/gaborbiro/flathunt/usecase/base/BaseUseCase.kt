package app.gaborbiro.flathunt.usecase.base

import app.gaborbiro.flathunt.GlobalVariables
import org.koin.core.component.KoinComponent

abstract class BaseUseCase : UseCase, KoinComponent {

    protected fun String.checkLastUsedIndexOrWebId(): String {
        return GlobalVariables.lastUsedIndexOrWebId?.let {
            if (this == "$") {
                GlobalVariables.lastUsedIndexOrWebId
            } else {
                this
            }
        } ?: this
    }

//    private fun openUrl(url: String) {
//        Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler $url").waitFor()
//    }
}