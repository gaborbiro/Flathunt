package app.gaborbiro.flathunt.data.rightmove

import app.gaborbiro.flathunt.data.BaseStore

class RightmoveExpStore : BaseStore(
    prefPropertiesKey = PREF_PROPERTIES,
    prefIndexKey = PREF_INDEX,
    prefCookiesKey = PREF_COOKIES,
    prefBlacklistKey = PREF_BLACKLIST,
)

private const val PREF_PROPERTIES = "properties2"
private const val PREF_INDEX = "index2"
private const val PREF_COOKIES = "cookies2"
private const val PREF_BLACKLIST = "blacklist_rm_sngl"