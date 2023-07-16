package app.gaborbiro.flathunt.data.idealista

import app.gaborbiro.flathunt.data.BaseStore

class IdealistaStore : BaseStore(
    prefPropertiesKey = PREF_PROPERTIES,
    prefIndexKey = PREF_INDEX,
    prefCookiesKey = PREF_COOKIES,
    prefBlacklistKey = PREF_BLACKLIST,
)

private const val PREF_PROPERTIES = "properties"
private const val PREF_INDEX = "index"
private const val PREF_COOKIES = "cookies"
private const val PREF_BLACKLIST = "blacklist_sr"