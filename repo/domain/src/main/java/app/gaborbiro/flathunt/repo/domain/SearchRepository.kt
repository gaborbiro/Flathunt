package app.gaborbiro.flathunt.repo.domain

interface SearchRepository {

    fun fetchSearchResults(searchUrl: String)
}