package app.gaborbiro.flathunt.repo.domain

import app.gaborbiro.flathunt.data.domain.model.Property

interface DirectionsRepository {

    fun validateDirections(properties: List<Property>): Pair<List<Property>, List<Property>>

    fun validateDirections(): Pair<List<Property>, List<Property>>
}