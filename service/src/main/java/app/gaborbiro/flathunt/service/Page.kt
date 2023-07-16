package app.gaborbiro.flathunt.service

class Page(
    val urls: List<String>,
    val page: Int,
    val pageCount: Int,
    var propertiesRemoved: Int = 0,
    val nextPage: Page.() -> Page?,
)