package app.gaborbiro.flathunt.repo.domain

interface SearchRepository {

    fun fetchPropertiesFromAllPages(url: String)
}