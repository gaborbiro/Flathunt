package app.gaborbiro.flathunt.console

import java.io.PrintWriter

class ConsoleWriterFormatted : ConsoleWriter {

    private val infoColor: String = "\u001B[36m"
    private val warningColor: String = "\u001B[93m"
    private val errorColor: String = "\u001b[31m"
    private val resetCode: String = "\u001b[0m"
    private val writer: PrintWriter = System.console().writer()

    override fun d(msg: String?) {
        doPrint(msg)
    }

    override fun d(msg: String, newLine: Boolean) {
        doPrint(msg, newLine = newLine)
    }

    override fun i(msg: String) {
        doPrint(msg, color = infoColor)
    }

    override fun i(msg: String, newLine: Boolean) {
        doPrint(msg, color = infoColor, newLine = newLine)
    }

    override fun w(msg: String) {
        doPrint(msg, color = warningColor)
    }

    override fun w(msg: String, newLine: Boolean) {
        doPrint(msg, color = warningColor, newLine = newLine)
    }

    override fun e(msg: String) {
        doPrint(msg, color = errorColor)
    }

    override fun e(msg: String, newLine: Boolean) {
        doPrint(msg, color = errorColor, newLine = newLine)
    }

    private fun doPrint(msg: String?, color: String = "") {
        doPrint(msg, color, newLine = true)
    }

    private fun doPrint(msg: String?, color: String = "", newLine: Boolean) {
        val finalMsg = if (color.isNotBlank()) "${color}${msg}${resetCode}" else msg
        if (newLine) {
            writer.println(finalMsg ?: "")
        } else {
            writer.print(finalMsg ?: "")
        }
    }
}