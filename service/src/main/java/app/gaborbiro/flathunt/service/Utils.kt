package app.gaborbiro.flathunt.service

import app.gaborbiro.flathunt.matcher
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver

fun WebDriver.linkExists(linkText: String): Boolean {
    return runCatching { findElement(By.linkText(linkText)) }.getOrNull() != null
}

fun WebDriver.findSimpleText(xpath: String): String? {
    return runCatching { findElement(By.xpath(xpath)).text }.getOrNull()
}

fun WebDriver.findRegex(regex: String): Array<String>? {
    val matcher = pageSource.matcher(regex)
    return if (matcher.find()) {
        (1..matcher.groupCount()).map { matcher.group(it) }.toTypedArray()
    } else {
        null
    }
}