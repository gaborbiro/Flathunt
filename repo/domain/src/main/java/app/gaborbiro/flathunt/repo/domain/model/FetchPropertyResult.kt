package app.gaborbiro.flathunt.repo.domain.model

sealed class FetchPropertyResult {
    data class Property(val property: app.gaborbiro.flathunt.data.domain.model.Property) : FetchPropertyResult()
    object Failed : FetchPropertyResult()
    object Unsuitable : FetchPropertyResult()

}