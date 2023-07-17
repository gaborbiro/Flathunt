package app.gaborbiro.flathunt.service

import app.gaborbiro.flathunt.AVG_WEEKS_IN_MONTH
import app.gaborbiro.flathunt.data.model.Price
import app.gaborbiro.flathunt.matcher

fun ensurePriceIsPerMonth(price: String): Price {
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
        Price(price, pricePerMonth, matcher.group(2).replace(",", "").toInt())
    } else {
        println("Error parsing price $price")
        Price(price, pricePerMonth, -1)
    }
}
