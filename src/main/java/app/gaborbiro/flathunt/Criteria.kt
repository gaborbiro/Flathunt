package app.gaborbiro.flathunt

import app.gaborbiro.flathunt.TravelMode.CYCLING
import app.gaborbiro.flathunt.TravelMode.TRANSIT

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

val poiBaixaChiado = POI.Destination(
    description = "Baixa-Chiado Metro Station",
    name = "Baixa-Chiado",
    coordinates = LatLon(latitude = "38.7108472", longitude = "-9.1430462"),
    max = 20 minutes TRANSIT
)

val EXP = ValidationCriteria(
    pointsOfInterest = listOf(
        poiBaixaChiado,
    ),
    maxPrice = 1600,
//    maxBedrooms = 2,
//    furnished = true,
//    maxFlatmates = 0,
//    airConditioning = true,
//    heating = true,
)