package app.gaborbiro.flathunt.usecase

import app.gaborbiro.flathunt.repo.domain.SearchRepository
import app.gaborbiro.flathunt.usecase.base.BaseUseCase
import app.gaborbiro.flathunt.usecase.base.command
import org.koin.core.component.inject

class SearchUseCase : BaseUseCase() {

    private val searchRepository: SearchRepository by inject()

    override val commands
        get() = listOf(
            search
        )

    private val search = command<String>(
        command = "search",
        description = "Read new properties from the search url. Saves and prints properties that are valid. " +
                "Marks them as unsuitable if needed",
        argumentName1 = "url",
    )
    { (url) ->
        searchRepository.fetchPropertiesFromAllPages(url)
    }
}
