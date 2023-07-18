package app.gaborbiro.flathunt

import app.gaborbiro.flathunt.compileTimeConstant.Constants
import app.gaborbiro.flathunt.data.di.DataModule
import app.gaborbiro.flathunt.di.setupKoin
import app.gaborbiro.flathunt.service.di.ServiceModule
import app.gaborbiro.flathunt.service.domain.Service
import app.gaborbiro.flathunt.service.spareroom.usecase.InboxUseCase
import app.gaborbiro.flathunt.usecase.*
import app.gaborbiro.flathunt.usecase.base.UseCase
import app.gaborbiro.flathunt.usecase.base.ValidationCriteria
import com.jcabi.manifests.Manifests
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.qualifier.StringQualifier
import org.koin.dsl.module
import org.koin.ksp.generated.module
import java.io.BufferedReader
import java.io.InputStreamReader


fun main(args: Array<String>) {
    FlatHunt().main(args)
}

class FlatHunt {

    fun main(args: Array<String>) {
        java.util.logging.LogManager.getLogManager().reset() // disable all logging

        if (Manifests.exists("jar-build-timestamp")) {
            println(
                "\n==========================================================================" +
                        "\nBuilt at:\t" + Manifests.read("jar-build-timestamp")
            )
        }

        val serviceConfig = getServiceConfigFromArgs(args)
        val strictCommand = getStrictCommandFromArgs(args)

        if (strictCommand == null) {
            println("Service:\t$serviceConfig")
        }

        val (serviceName, criteria) = serviceConfig.split("-")
        val app = setupKoin(serviceName, criteria)

        val useCases = getUseCases(serviceConfig)
        val commands = CommandSetBuilder(serviceConfig, useCases).buildCommandSet()

        BufferedReader(InputStreamReader(System.`in`)).use { reader ->
            val getCommandUseCase = GetCommandUseCase(commands)
            val commandProcessor = CommandProcessor {
                reader.readLine()
            }

            if (strictCommand != null) {
                getCommandUseCase.execute(strictCommand)?.let { (command, args) ->
                    GlobalVariables.strict = true
                    commandProcessor.doExecute(command, args)
                } ?: run { println("Invalid command") }
            }

            processInput(reader, getCommandUseCase, commandProcessor)
        }

        app.koin.get<Service>().cleanup()
    }

    private fun processInput(
        reader: BufferedReader,
        getCommandUseCase: GetCommandUseCase,
        commandProcessor: CommandProcessor
    ) {
        var input: String?
        var hintShown = false
        do {
            println("==========================================================================")
            if (hintShown.not()) {
                hintShown = true
                println("Type help for a list of available commands")
            }
            print("> ")
            input = reader.readLine()
            if (input == CommandSetBuilder.EXIT_COMMAND_CODE) {
                return
            }
            try {
                getCommandUseCase.execute(input)
                    ?.let {
                        commandProcessor.execute(it)
                    }
                    ?: run {
                        println("Invalid command")
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
            )

            Constants.`spareroom-exp` -> setOf(
                InboxUseCase(),
                SearchUseCase(),
                AddCheckUseCase(),
                RoutesUseCase(),
                ListUseCase(),
                PropertyUseCase(),
                MaintenanceUseCase(),
            )

            Constants.`rightmove-exp` -> setOf(
                SearchUseCase(),
                AddCheckUseCase(),
                RoutesUseCase(),
                ListUseCase(),
                PropertyUseCase(),
                MaintenanceUseCase(),
            )

            Constants.`zoopla-exp` -> setOf(
                SearchUseCase(),
                AddCheckUseCase(),
                RoutesUseCase(),
                ListUseCase(),
                PropertyUseCase(),
                MaintenanceUseCase(),
            )

            else -> throw IllegalArgumentException("Missing service parameter from Manifest")
        }
    }
}
