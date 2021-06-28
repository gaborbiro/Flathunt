package app.gaborbiro.flathunt.data.zoopla

import app.gaborbiro.flathunt.data.BaseStore

class ZooplaTwoStore : BaseStore(
    prefPropertiesKey = PREF_PROPERTIES,
    prefIndexKey = PREF_INDEX,
    prefCookiesKey = PREF_COOKIES,
    prefBlacklistKey = PREF_BLACKLIST,
)

private const val PREF_PROPERTIES = "properties_zoopla_two"
private const val PREF_INDEX = "index_zoopla_two"
private const val PREF_COOKIES = "cookies_zoopla_two"
private const val PREF_BLACKLIST = "blacklist_zoopla_two"