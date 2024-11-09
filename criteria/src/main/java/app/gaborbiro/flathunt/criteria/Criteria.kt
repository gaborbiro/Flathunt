package app.gaborbiro.flathunt.criteria

import app.gaborbiro.flathunt.criteria.POITravelMode.*

private val poiCW = POI.Coordinate(
    description = "Canada Water Station",
    location = POILocation(
        latitude = "51.4988863",
        longitude = "-0.1322229",
    ),
    max = 20 minutes CYCLING or 30 minutes TRANSIT,
)

private val poiSoho = POI.Coordinate(
    description = "Dean Street",
    location = POILocation(
        latitude = "51.5142306",
        longitude = "-0.1300049",
    ),
    max = 45 minutes TRANSIT,
)

private val poiBaixaChiado = POI.Address(
    description = "Baixa-Chiado",
    location = POILocation(
        latitude = "38.710879111221196",
        longitude = "-9.140458860931528",
    ),
    address = "Baixa-Chiado",
    max = 25 minutes TRANSIT or 10 minutes WALKING,
)

private val poiGabor = POI.Address(
    description = "Gabor",
    location = POILocation(
        latitude = "38.7244735",
        longitude = "-9.1318743",
    ),
    address = "Rua+de+Macau+30,+1170-065+Lisboa",
    max = 25 minutes TRANSIT or 10 minutes WALKING,
)

private val poiRnD = POI.Address(
    description = "Rahul and David",
    location = POILocation(
        latitude = "38.72430133625508",
        longitude = "-9.15469834724955",
    ),
    address = "R.+Rodrigo+da+Fonseca+97,+1250-190+Lisboa",
    max = 25 minutes TRANSIT or 15 minutes WALKING,
)

private val poiMartha = POI.Address(
    description = "Martha",
    location = POILocation(
        latitude = "38.73147350451234",
        longitude = "-9.132385600454764"
    ),
    address = "R.+Heróis+de+Quionga+44,+1170-088+Lisboa",
    max = 25 minutes TRANSIT or 15 minutes WALKING,
)

private val poiFitnessHut = POI.CoordinateSet(
    description = "Fitness Huts in Lisbon",
    locations = listOf(
        POI.Address(
            description = "Fitness Hut Santos",
            location = POILocation(
                latitude = "38.708253178623536",
                longitude = "-9.150803589204868",
            ),
            address = "Fitness+Hut+Santos",
        ),
        POI.Address(
            description = "Fitness Hut Amoreiras",
            location = POILocation(
                latitude = "38.72092357360911",
                longitude = "-9.15984010447187",
            ),
            address = "Fitness+Hut+Amoreiras",
        ),
        POI.Address(
            description = "Fitness Hut Marquês de Pombal",
            location = POILocation(
                latitude = "38.72479714580329",
                longitude = "-9.146356587794353",
            ),
            address = "Fitness+Hut+Marquês+de+Pombal",
        ),
        POI.Address(
            description = "Fitness Hut Almirante Reis",
            location = POILocation(
                latitude = "38.72597416226121",
                longitude = "-9.135204206173372",
            ),
            address = "Fitness+Hut+Almirante+Reis",
        ),
        POI.Address(
            description = "Fitness Hut Picoas",
            location = POILocation(
                latitude = "38.7311973146677",
                longitude = "-9.148109215058927",
            ),
            address = "Fitness+Hut+Picoas",
        ),
        POI.Address(
            description = "Fitness Hut Arco do Cego",
            location = POILocation(
                latitude = "38.73473340356933",
                longitude = "-9.139608916670428",
            ),
            address = "Fitness+Hut+Arco+do+Cego",
        ),
        POI.Address(
            description = "Fitness Hut Campo Pequeno",
            location = POILocation(
                latitude = "38.739304484387795",
                longitude = "-9.147036527976272",
            ),
            address = "Fitness+Hut+Campo+Pequeno",
        ),
        POI.Address(
            description = "Fitness Hut Avenida de Roma",
            location = POILocation(
                latitude = "38.74318477063692",
                longitude = "-9.139358131009955",
            ),
            address = "Fitness+Hut+Avenida+de+Roma",
        ),
        POI.Address(
            description = "Hyatt Regency",
            location = POILocation(
                latitude = "38.69854577397226",
                longitude = "-9.186718490548676"
            ),
            address = "Hyatt+Regency+Lisbon",
        )
    ),
    max = 10 minutes WALKING,
)

private val poiCrossFit = POI.CoordinateSet(
    description = "Cross Fits in Lisbon",
    locations = listOf(
        POI.Address(
            description = "The Bakery CrossFit",
            location = POILocation(latitude = "38.723636162765175", longitude = "-9.139565886508088"),
            address = "The Bakery CrossFit",
        ),
        POI.Address(
            description = "Off Limits CrossFit",
            location = POILocation(latitude = "38.72018149425185", longitude = "-9.159769497792343"),
            address = "Off Limits CrossFit",
        ),
        POI.Address(
            description = "CrossFit Alvalade IV",
            location = POILocation(latitude = "38.74061416724753", longitude = "-9.103452213491906"),
            address = "CrossFit Alvalade IV",
        ),
        POI.Address(
            description = "Matchbox CrossFit",
            location = POILocation(latitude = "38.73900802822198", longitude = "-9.156292700000002"),
            address = "Matchbox CrossFit",
        ),
        POI.Address(
            description = "Trend CrossFit & Pilates Studio",
            location = POILocation(latitude = "38.74656124727494", longitude = "-9.13888874232794"),
            address = "Trend CrossFit & Pilates Studio",
        ),
        POI.Address(
            description = "QF CrossFit",
            location = POILocation(latitude = "38.719586829028245", longitude = "-9.166317884655877"),
            address = "QF CrossFit",
        ),
        POI.Address(
            description = "CrossFit Park of Nations",
            location = POILocation(latitude = "38.78445569817578", longitude = "-9.13099638942564"),
            address = "CrossFit Parque das Nações",
        ),
        POI.Address(
            description = "Off Limits CrossFit",
            location = POILocation(latitude = "38.7201042747466", longitude = "-9.159802433608442"),
            address = "Off Limits CrossFit",
        ),
        POI.Address(
            description = "CrossFit Caravelas",
            location = POILocation(latitude = "38.70894482520217", longitude = "-9.1653065"),
            address = "CrossFit Caravelas",
        ),
    ),
    max = 10 minutes WALKING,
)

val EXP = ValidationCriteria(
    pointsOfInterest = listOf(
        poiBaixaChiado,
        poiRnD,
//        poiMartha,
        poiFitnessHut,
    ),
    maxPrice = 1400,
    minBedrooms = 1,
    maxBedrooms = 2,
    furnished = true,
    allowedEnergyCertification = listOf("a+", "a+", "a", "b", "b-", "c", "d", "In process", "Not indicated", "Unknown")
)

val TIAGO = ValidationCriteria(
    pointsOfInterest = listOf(
        poiGabor,
        poiCrossFit,
    ),
    maxPrice = 1000,
    minBedrooms = 0,
    maxBedrooms = 1,
    furnished = true,
    allowedEnergyCertification = listOf("a+", "a+", "a", "b", "b-", "c", "d", "In process", "Not indicated", "Unknown")
)