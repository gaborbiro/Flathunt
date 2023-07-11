package app.gaborbiro.flathunt.data.spareroom

import app.gaborbiro.flathunt.data.BaseStore

class SpareroomTwoStore : BaseStore(
    prefPropertiesKey = PREF_PROPERTIES,
    prefIndexKey = PREF_INDEX,
    prefCookiesKey = PREF_COOKIES,
    prefBlacklistKey = PREF_BLACKLIST,
)

private const val PREF_PROPERTIES = "properties_sr_two"
private const val PREF_INDEX = "index_sr_two"
private const val PREF_COOKIES = "cookies_sr_two"
private const val PREF_BLACKLIST = "blacklist_sr_two"