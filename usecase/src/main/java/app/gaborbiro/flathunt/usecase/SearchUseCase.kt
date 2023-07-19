package app.gaborbiro.flathunt.usecase

import app.gaborbiro.flathunt.repo.domain.SearchRepository
import app.gaborbiro.flathunt.usecase.base.BaseUseCase
import app.gaborbiro.flathunt.usecase.base.command
import org.koin.core.component.inject

class SearchUseCase : BaseUseCase() {

    private val searchRepository: SearchRepository by inject()

    override val commands
        get() = listOf(search)

    private val search = command<String>(
        command = "search",
        description = "Reads new properties from a search url. Marks, saves and prints found properties that are valid",
        argumentName = "search url",
    )
    { (searchUrl) ->
        searchRepository.fetchSearchResults(searchUrl)
    }
}
