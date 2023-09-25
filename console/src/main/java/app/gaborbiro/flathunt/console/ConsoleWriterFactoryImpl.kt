package app.gaborbiro.flathunt.console

import org.koin.core.annotation.Singleton
import org.koin.core.component.KoinComponent

@Singleton
class ConsoleWriterFactoryImpl : ConsoleWriterFactory, KoinComponent {

    override fun getConsoleWriter(): ConsoleWriter {
        return if (System.console() != null || System.console()?.writer() != null) {
            ConsoleWriterFormatted()
        } else {
            ConsoleWriterSimple()
        }
    }
}