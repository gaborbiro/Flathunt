package app.gaborbiro.flathunt.console

interface ConsoleWriter {

    fun d(msg: String? = null)
    fun d(msg: String, newLine: Boolean)
    fun i(msg: String)
    fun i(msg: String, newLine: Boolean)
    fun w(msg: String)
    fun w(msg: String, newLine: Boolean)
    fun e(msg: String)
    fun e(msg: String, newLine: Boolean)
}