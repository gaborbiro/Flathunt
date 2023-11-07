package app.gaborbiro.flathunt.service

import app.gaborbiro.flathunt.matcher

private const val AVG_WEEKS_IN_MONTH = 365.0 / 7 / 12 // 4.345

fun ensurePriceIsPerMonth(price: String): PriceParseResult {
    val pricePerMonth = if (price.contains("pw")) {
        val matcher = price.matcher("([^\\d])([\\d\\.,]+)[\\s]*pw")
        if (matcher.find()) {
            val perMonth: Double = matcher.group(2).replace(",", "").toFloat() * AVG_WEEKS_IN_MONTH
            "${matcher.group(1)}${perMonth.toInt()} pcm"
        } else {
            price
        }
    } else price
    val matcher = pricePerMonth.matcher("([^\\d])([\\d\\.,]+)[\\s]*pcm")
    return if (matcher.find()) {
        PriceParseResult.Price(pricePerMonth, matcher.group(2).replace(",", "").toInt())
    } else {
        PriceParseResult.ParseError(pricePerMonth)
    }
}

sealed class PriceParseResult(open val pricePerMonth: String) {
    data class Price(
        override val pricePerMonth: String,
        val pricePerMonthInt: Int,
    ) : PriceParseResult(pricePerMonth)

    data class ParseError(override val pricePerMonth: String) : PriceParseResult(pricePerMonth)
}