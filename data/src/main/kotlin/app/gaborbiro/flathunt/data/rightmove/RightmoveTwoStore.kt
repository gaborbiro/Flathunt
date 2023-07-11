package app.gaborbiro.flathunt.data.rightmove

import app.gaborbiro.flathunt.data.BaseStore

class RightmoveTwoStore : BaseStore(
    prefPropertiesKey = PREF_PROPERTIES,
    prefIndexKey = PREF_INDEX,
    prefCookiesKey = PREF_COOKIES,
    prefBlacklistKey = PREF_BLACKLIST,
)

private const val PREF_PROPERTIES = "properties_rm_two"
private const val PREF_INDEX = "index_rm_two"
private const val PREF_COOKIES = "cookies_rm_two"
private const val PREF_BLACKLIST = "blacklist_rm_two"