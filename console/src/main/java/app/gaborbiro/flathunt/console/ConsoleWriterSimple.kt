package app.gaborbiro.flathunt.console

class ConsoleWriterSimple : ConsoleWriter {

    private val printStream = System.out

    override fun d(msg: String?) {
        doPrint(msg)
    }

    override fun d(msg: String, newLine: Boolean) {
        doPrint(msg, newLine = newLine)
    }

    override fun i(msg: String) {
        doPrint(msg)
    }

    override fun i(msg: String, newLine: Boolean) {
        doPrint(msg, newLine = newLine)
    }

    override fun w(msg: String) {
        doPrint(msg)
    }

    override fun w(msg: String, newLine: Boolean) {
        doPrint(msg, newLine = newLine)
    }

    override fun e(msg: String) {
        doPrint(msg)
    }

    override fun e(msg: String, newLine: Boolean) {
        doPrint(msg, newLine = newLine)
    }

    private fun doPrint(msg: String?) {
        doPrint(msg, newLine = true)
    }

    private fun doPrint(msg: String?, newLine: Boolean) {
        if (newLine) {
            printStream.println(msg ?: "")
        } else {
            printStream.print(msg ?: "")
        }
    }
}