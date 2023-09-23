package app.gaborbiro.flathunt.request

interface RequestCaller {

    fun post(url: String, payload: String, cookies: String): Boolean

    fun get(url: String): String
}