package app.gaborbiro.flathunt.repo.domain

import app.gaborbiro.flathunt.data.domain.model.Property

interface DirectionsRepository {

    fun revalidateDirections(properties: List<Property>): Pair<List<Property>, List<Property>>

    fun revalidateDirections(): Pair<List<Property>, List<Property>>
}