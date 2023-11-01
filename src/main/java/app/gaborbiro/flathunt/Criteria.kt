package app.gaborbiro.flathunt

import app.gaborbiro.flathunt.criteria.POI
import app.gaborbiro.flathunt.criteria.POITravelMode.*
import app.gaborbiro.flathunt.criteria.ValidationCriteria

val poiCW = POI.Destination(
    description = "Canada Water Station",
    latitude = "51.4988863",
    longitude = "-0.1322229",
    max = 20 minutes CYCLING or 30 minutes TRANSIT,
)

val poiSoho = POI.Destination(
    description = "Dean Street",
    latitude = "51.5142306",
    longitude = "-0.1300049",
    max = 45 minutes TRANSIT,
)

val poiBaixaChiado = POI.Destination(
    description = "Baixa-Chiado Metro Station",
    latitude = "38.7108472",
    longitude = "-9.1430462",
    max = 20 minutes TRANSIT or 10 minutes WALKING
)

val poiRnD = POI.Destination(
    description = "Rahul and David",
    latitude = "38.7246084",
    longitude = "-9.140747",
    max = 30 minutes TRANSIT or 15 minutes WALKING
)

val EXP = ValidationCriteria(
    pointsOfInterest = listOf(
        poiBaixaChiado,
        poiRnD
    ),
    maxPrice = 1400,
    minBedrooms = 1,
    maxBedrooms = 2,
    furnished = true,
)