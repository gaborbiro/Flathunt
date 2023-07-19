package app.gaborbiro.flathunt.repo.domain

import app.gaborbiro.flathunt.data.domain.model.Property
import app.gaborbiro.flathunt.repo.domain.model.SaveType

interface FetchPropertyRepository {

    fun fetchProperty(arg: String, save: SaveType, safeMode: Boolean): Property?
}