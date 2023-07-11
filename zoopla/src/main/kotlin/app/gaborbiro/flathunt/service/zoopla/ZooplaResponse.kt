package app.gaborbiro.flathunt.service.zoopla

class ZooplaResponse(
    val props: Props
)

class Props(
    val initialProps: InitialProps
)

class InitialProps(
    val pageProps: PageProps
)

class PageProps(
    val data: Data
)

class Data(
    val listing: ZooplaProperty
)

class ZooplaProperty(
    val features: PropertyFeatures,
    val propertyImage: Array<Image>,
    val title: String,
    val location: PropertyLocation,
    val content: PropertyContent?,
    val counts: Counts,
    val pricing: Pricing
)

class Image(
    val filename: String?,
    val original: String?,
)

class PropertyFeatures(
    val bullets: Array<String>?,
    val flags: Flags
)

class Flags(
    val furnishedState: FurnishedState,
    val availableFromDate: String
)

class FurnishedState(val name: String)

class PropertyLocation(
    val coordinates: PropertyCoordinates
)


class PropertyCoordinates(
    val latitude: Double,
    val longitude: Double,
)

class PropertyContent(
    val floorPlan: Array<Image>?
)

class Counts(
    val numBedrooms: Int?,
    val numBathrooms: Int?,
    val numLivingRooms: Int?,
)

class Pricing(
    val label: String
)