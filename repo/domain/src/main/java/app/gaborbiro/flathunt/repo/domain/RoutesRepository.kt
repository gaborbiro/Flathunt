package app.gaborbiro.flathunt.repo.domain

import app.gaborbiro.flathunt.data.domain.model.Property

interface RoutesRepository {

    fun revalidateRoutes(properties: List<Property>): Pair<List<Property>, List<Property>>

    fun revalidateRoutes(): Pair<List<Property>, List<Property>>
}