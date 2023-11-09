package app.gaborbiro.flathunt.repo.domain

import app.gaborbiro.flathunt.GlobalVariables
import app.gaborbiro.flathunt.repo.domain.model.FetchPropertyResult
import app.gaborbiro.flathunt.repo.domain.model.SaveType

interface FetchPropertyRepository {

    fun fetchProperty(idu: String, save: SaveType, safeMode: Boolean = GlobalVariables.safeMode): FetchPropertyResult
}