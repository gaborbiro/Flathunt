import app.gaborbiro.flathunt.google.POI
import app.gaborbiro.flathunt.google.TravelMode.CYCLING
import app.gaborbiro.flathunt.google.TravelMode.TRANSIT
import app.gaborbiro.flathunt.usecases.ValidationCriteria
import app.gaborbiro.flathunt.LatLon
import app.gaborbiro.flathunt.google.minutes
import app.gaborbiro.flathunt.google.or
import java.time.LocalDate

val poiCW = POI.Destination(
    description = "Schwann",
    name = "Canada Water Station",
    coordinates = LatLon(latitude = "51.4988863", longitude = "-0.1322229"),
    max = 20 minutes CYCLING or 30 minutes TRANSIT,
)

val poiMia = POI.Destination(
    description = "Mia's WP",
    name = "Westminster Station",
    coordinates = LatLon(latitude = "51.4988524", longitude = "-0.1292879"),
    max = 40 minutes TRANSIT,
)

val poiSoho = POI.Destination(
    description = "Soho",
    name = "Dean Street",
    coordinates = LatLon(latitude = "51.5142306", longitude = "-0.1300049"),
    max = 45 minutes TRANSIT,
)

val EXP = ValidationCriteria(
    pointsOfInterest = listOf(
        poiSoho,
        poiCW,
        POI.NearestRailStation,
    ),
    maxPrice = 1300,
    maxBedrooms = 2,
    furnished = true,
    canMoveEarliest = LocalDate.of(2021, 7, 1),
    canMoveLatest = LocalDate.of(2021, 8, 20),
    minRequiredMonths = 12,
    noBedsit = true,
)

val WITH_FLATMATE = ValidationCriteria(
    pointsOfInterest = listOf(
        poiSoho,
        poiCW,
        poiMia,
        POI.NearestRailStation,
    ),
    maxPrice = 2300,
    minBedrooms = 2,
    maxBedrooms = 2,
    furnished = true,
    canMoveEarliest = LocalDate.of(2021, 8, 1),
    canMoveLatest = LocalDate.of(2021, 8, 20),
    minRequiredMonths = 12,
    noBedsit = true,
    sharedLivingRoom = true,
    maxFlatmates = 2,
)