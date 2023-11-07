package app.gaborbiro.flathunt.criteria


infix fun Int.minutes(travelMode: POITravelMode) = POITravelLimit(travelMode, this)

infix fun POITravelLimit.or(travelLimit: POITravelLimit) = arrayOf(this, travelLimit)
infix fun Array<POITravelLimit>.or(travelLimit: POITravelLimit) = this + travelLimit

infix fun POITravelLimit.or(minutes: Int) = TravelLimitBuilder(arrayOf(this), minutes)
infix fun Array<POITravelLimit>.or(minutes: Int) = TravelLimitBuilder(this, minutes)

class TravelLimitBuilder(private val maxes: Array<POITravelLimit>, private val minutes: Int) {
    fun build(travelMode: POITravelMode) = maxes or (minutes minutes travelMode)
}

infix fun TravelLimitBuilder.minutes(travelMode: POITravelMode) = build(travelMode)