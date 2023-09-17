package app.gaborbiro.flathunt.repo.domain

import app.gaborbiro.flathunt.data.domain.model.Property

interface RoutesRepository {

    fun validateByRoutes(properties: List<Property>): Pair<List<Property>, List<Property>>

    fun validateByRoutes(): Pair<List<Property>, List<Property>>
}