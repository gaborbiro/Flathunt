package app.gaborbiro.flathunt

import app.gaborbiro.flathunt.compileTimeConstant.Constants
import app.gaborbiro.flathunt.console.ConsoleWriter
import app.gaborbiro.flathunt.di.setupKoin
import app.gaborbiro.flathunt.service.domain.Service
import app.gaborbiro.flathunt.service.spareroom.usecase.InboxUseCase
import app.gaborbiro.flathunt.usecase.*
import app.gaborbiro.flathunt.usecase.base.UseCase
import com.jcabi.manifests.Manifests
import java.io.BufferedReader
import java.io.InputStreamReader


fun main(args: Array<String>) {
    FlatHunt().main(args)
}

class FlatHunt {

    fun main(args: Array<String>) {
        java.util.logging.LogManager.getLogManager().reset() // disable all logging

        val serviceConfig = getServiceConfigFromArgs(args)
        val app = setupKoin(serviceConfig)

        val console = app.koin.get<ConsoleWriter>()

        if (Manifests.exists("jar-build-timestamp")) {
            console.d(
                "\n==========================================================================" +
                        "\nBuilt at:\t" + Manifests.read("jar-build-timestamp")
            )
        }

        val strictCommandStr = getStrictCommandFromArgs(args)
        if (strictCommandStr == null) {
            console.d("Service:\t$serviceConfig")
        }

        val useCases = getUseCases(serviceConfig)
        val commands = CommandSetBuilder(serviceConfig, useCases, console).buildCommandSet()

        BufferedReader(InputStreamReader(System.`in`)).use { reader ->
            val parseCommandUseCase = ParseCommandUseCase(commands, console)
            val commandUseCase = CommandUseCase(console)

            if (strictCommandStr != null) {
                parseCommandUseCase.execute(strictCommandStr)?.let { command ->
                    GlobalVariables.strict = true
                    commandUseCase.execute(command)
                } ?: run { console.d("Invalid command") }
            }

            processInput(reader, parseCommandUseCase, commandUseCase, console)
        }

        app.koin.get<Service>().cleanup()
    }

    private fun processInput(
        reader: BufferedReader,
        parseCommandUseCase: ParseCommandUseCase,
        commandUseCase: CommandUseCase,
        console: ConsoleWriter,
    ) {
        var input: String?
        var hintShown = false
        val argumentsUseCase = ArgumentsUseCase {
            reader.readLine()
        }
        do {
            console.d("==========================================================================")
            if (hintShown.not()) {
                hintShown = true
                console.d("Type 'help' for a list of available commands")
            }
            console.d("> ", newLine = false)
            input = reader.readLine()
            if (input == CommandSetBuilder.EXIT_COMMAND_CODE) {
                return
            }
            try {
                var command = parseCommandUseCase.execute(input)
                command = command?.let(argumentsUseCase::execute)
                command
                    ?.let(commandUseCase::execute)
                    ?: run {
                        console.d("Invalid command")
                    }
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        } while (true)
    }

    private fun getStrictCommandFromArgs(args: Array<String>): String? {
        return if (args.size > 1) {
            when {
                args[0] == "-c" -> {
                    args[1]
                }

                args[1] == "-c" -> {
                    args[2]
                }

                args[1] == "debug" -> {
                    GlobalVariables.debug = true
                    null
                }

                else -> {
                    null
                }
            }
        } else {
            null
        }
    }

    private fun getServiceConfigFromArgs(args: Array<String>): String {
        return if (Manifests.exists("jar-build-timestamp")) {
            Manifests.read("serviceConfig")
        } else args[0]
    }

    private fun getUseCases(serviceConfig: String): Set<UseCase> {
        return when (serviceConfig) {
            Constants.`idealista-exp` -> setOf(
                SearchUseCase(),
                MaintenanceUseCase(),
                ListUseCase()
            )

            Constants.`spareroom-exp` -> setOf(
                InboxUseCase(),
                SearchUseCase(),
                FetchPropertyUseCase(),
                RoutesUseCase(),
                ListUseCase(),
                ManagePropertyUseCase(),
                MaintenanceUseCase(),
            )

            Constants.`rightmove-exp` -> setOf(
                SearchUseCase(),
                FetchPropertyUseCase(),
                RoutesUseCase(),
                ListUseCase(),
                ManagePropertyUseCase(),
                MaintenanceUseCase(),
            )

            Constants.`zoopla-exp` -> setOf(
                SearchUseCase(),
                FetchPropertyUseCase(),
                RoutesUseCase(),
                ListUseCase(),
                ManagePropertyUseCase(),
                MaintenanceUseCase(),
            )

            else -> throw IllegalArgumentException("Missing service parameter from Manifest")
        }
    }
}
