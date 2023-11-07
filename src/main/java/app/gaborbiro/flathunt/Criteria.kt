package app.gaborbiro.flathunt

import app.gaborbiro.flathunt.criteria.POI
import app.gaborbiro.flathunt.criteria.POITravelMode.*
import app.gaborbiro.flathunt.criteria.ValidationCriteria

val poiCW = POI.Coordinate(
    description = "Canada Water Station",
    latitude = "51.4988863",
    longitude = "-0.1322229",
    max = 20 minutes CYCLING or 30 minutes TRANSIT,
)

val poiSoho = POI.Coordinate(
    description = "Dean Street",
    latitude = "51.5142306",
    longitude = "-0.1300049",
    max = 45 minutes TRANSIT,
)

val poiBaixaChiado = POI.Address(
    description = "Baixa-Chiado",
    latitude = "38.710879111221196",
    longitude = "-9.140458860931528",
    address = "Baixa-Chiado",
    max = 25 minutes TRANSIT or 10 minutes WALKING
)

val poiRnD = POI.Address(
    description = "Rahul and David",
    latitude = "38.72430133625508",
    longitude = "-9.15469834724955",
    address = "R.+Rodrigo+da+Fonseca+97,+1250-190+Lisboa",
    max = 25 minutes TRANSIT or 15 minutes WALKING
)

val poiMartha = POI.Address(
    description = "Martha",
    latitude = "38.73147350451234",
    longitude = "-9.132385600454764",
    address = "R.+Heróis+de+Quionga+44,+1170-088+Lisboa",
    max = 25 minutes TRANSIT or 15 minutes WALKING
)

val EXP = ValidationCriteria(
    pointsOfInterest = listOf(
        poiBaixaChiado,
        poiRnD,
        poiMartha,
    ),
    maxPrice = 1400,
    minBedrooms = 1,
    maxBedrooms = 2,
    furnished = true,
    allowedEnergyCertification = listOf("a+", "a+", "a", "b", "b-", "c", "d", "In process", "Not indicated", "Unknown")
)