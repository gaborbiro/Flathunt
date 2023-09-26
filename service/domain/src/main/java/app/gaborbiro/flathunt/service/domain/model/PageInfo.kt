package app.gaborbiro.flathunt.service.domain.model

class PageInfo(
    val pageUrl: String,
    val propertyWebIds: List<String>,
    val page: Int,
    val pageCount: Int,
)