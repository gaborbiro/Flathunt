package app.gaborbiro.flathunt


infix fun Int.minutes(travelMode: TravelMode) = TravelLimit(travelMode, this)

infix fun TravelLimit.or(travelLimit: TravelLimit) = arrayOf(this, travelLimit)
infix fun Array<TravelLimit>.or(travelLimit: TravelLimit) = this + travelLimit

infix fun TravelLimit.or(minutes: Int) = TravelLimitBuilder(arrayOf(this), minutes)
infix fun Array<TravelLimit>.or(minutes: Int) = TravelLimitBuilder(this, minutes)

class TravelLimitBuilder(private val maxes: Array<TravelLimit>, private val minutes: Int) {
    fun build(travelMode: TravelMode) = maxes or (minutes minutes travelMode)
}

infix fun TravelLimitBuilder.minutes(travelMode: TravelMode) = build(travelMode)