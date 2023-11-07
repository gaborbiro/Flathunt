package app.gaborbiro.flathunt.repo.domain

import app.gaborbiro.flathunt.data.domain.model.Property

interface DirectionsRepository {

    fun validateDirections(property: Property): Property?
}