package app.gaborbiro.flathunt.request

import app.gaborbiro.flathunt.GlobalVariables
import app.gaborbiro.flathunt.console.ConsoleWriter
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.koin.core.annotation.Singleton
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.net.HttpURLConnection
import java.net.URL

@Singleton
class RequestCallerImpl : RequestCaller, KoinComponent {

    private val console: ConsoleWriter by inject()

    override fun post(url: String, payload: String, cookies: String): Boolean {
        val type = "application/json; charset=utf-8".toMediaType()
        val body = payload.toRequestBody(type)
        val request: Request = Request.Builder()
            .addHeader("Cookie", cookies)
            .addHeader("Content-Type", "application/json")
            .url(url)
            .post(body)
            .build()
        if (GlobalVariables.debug) {
            console.i(request.toString() + "\n" + payload)
        }
        val result = OkHttpClient().newCall(request).execute()
        return if (result.code < 200 || result.code > 299) {
            console.e("Error ${result.code}: " + result.message)
            false
        } else {
            true
        }
    }

    @Throws(Exception::class)
    override fun get(url: String): String {
//        console.d()
//        console.d(url, newLine = false)
        val connection = URL(url).openConnection()
        return connection.inputStream.bufferedReader().use { it.readText() }.also {
            (connection as HttpURLConnection).disconnect()
//            console.d(" - ${it.length}")
        }
    }
}