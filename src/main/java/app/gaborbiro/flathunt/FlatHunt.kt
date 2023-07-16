package app.gaborbiro.flathunt

import EXP
import app.gaborbiro.flathunt.data.idealista.IdealistaStore
import app.gaborbiro.flathunt.data.rightmove.RightmoveExpStore
import app.gaborbiro.flathunt.data.spareroom.SpareroomStore
import app.gaborbiro.flathunt.data.zoopla.ZooplaExpStore
import app.gaborbiro.flathunt.service.Service
import app.gaborbiro.flathunt.service.idealista.IdealistaService
import app.gaborbiro.flathunt.service.rightmove.RightmoveService
import app.gaborbiro.flathunt.service.spareroom.SpareRoomService
import app.gaborbiro.flathunt.service.spareroom.usecase.InboxUseCase
import app.gaborbiro.flathunt.service.zoopla.ZooplaService
import app.gaborbiro.flathunt.usecase.*
import app.gaborbiro.flathunt.usecase.base.*
import com.google.gson.reflect.TypeToken
import com.jcabi.manifests.Manifests
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.regex.Matcher
import java.util.regex.Pattern


fun main(args: Array<String>) {
    FlatHunt().main(args)
}

class FlatHunt {

    private lateinit var commands: Map<String, Command<*>>

    fun main(args: Array<String>) {
        java.util.logging.LogManager.getLogManager().reset() // disable all logging

        if (Manifests.exists("jar-build-timestamp")) {
            println(
                "\n==========================================================================" +
                        "\nBuilt at:\t" + Manifests.read("jar-build-timestamp")
            )
        }

        val serviceStr = getServiceFromArgs(args)
        val service = getService(serviceStr)
        val strictCommand = getStrictCommand(args)

        if (strictCommand != null) {
            processCommand(strictCommand)?.let { (command, args) ->
                GlobalVariables.strict = true
                executeCommand(command, args)
            } ?: run { println("Invalid command") }
        }

        BufferedReader(InputStreamReader(System.`in`)).use { reader ->
            var input: String?
            if (strictCommand == null) {
                println("Service:\t$serviceStr")
            }
            var hintShown = false
            do {
                println("==========================================================================")
                if (hintShown.not()) {
                    hintShown = true
                    println("Type help for a list of available commands")
                }
                print("> ")
                input = reader.readLine()
                if (input == EXIT_COMMAND_CODE) {
                    return@use
                }
                try {
                    processCommand(input)?.let { (command, args) ->
                        executeCommand(command, reader, args)
                    } ?: run { println("Invalid command") }
                } catch (t: Throwable) {
                    t.printStackTrace()
                }
            } while (true)
        }

        service.cleanup()
    }

    private fun getStrictCommand(args: Array<String>): String? {
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

    private fun getServiceFromArgs(args: Array<String>): String {
        return if (Manifests.exists("jar-build-timestamp")) {
            Manifests.read("service")
        } else args[0]
    }

    private fun getService(serviceStr: String): Service {
        return when (serviceStr) {
            "idealista-exp" -> {
                val store = IdealistaStore()
                val service = IdealistaService(store)
                buildCommandSet(
                    serviceName = serviceStr,
                    DemoUseCase(service, store, EXP),
                    SearchUseCase(service, store, EXP),
                )
                service
            }

            "spareroom-exp" -> {
                val store = SpareroomStore()
                val service = SpareRoomService(store)
                buildCommandSet(
                    serviceName = serviceStr,
                    InboxUseCase(service, store, EXP),
                    SearchUseCase(service, store, EXP),
                    AddCheckUseCase(service, store, EXP),
                    RoutesUseCase(service, store, EXP),
                    ListUseCase(service, store, EXP),
                    PropertyUseCase(service, store, EXP),
                    MaintenanceUseCase(store),
                )
                service
            }

            "rightmove-exp" -> {
                val store = RightmoveExpStore()
                val service = RightmoveService(store)
                buildCommandSet(
                    serviceName = serviceStr,
                    SearchUseCase(service, store, EXP),
                    AddCheckUseCase(service, store, EXP),
                    RoutesUseCase(service, store, EXP),
                    ListUseCase(service, store, EXP),
                    PropertyUseCase(service, store, EXP),
                    MaintenanceUseCase(store),
                )
                service
            }

            "zoopla-exp" -> {
                val store = ZooplaExpStore()
                val service = ZooplaService(store)
                buildCommandSet(
                    serviceName = serviceStr,
                    SearchUseCase(service, store, EXP),
                    AddCheckUseCase(service, store, EXP),
                    RoutesUseCase(service, store, EXP),
                    ListUseCase(service, store, EXP),
                    PropertyUseCase(service, store, EXP),
                    MaintenanceUseCase(store),
                )
                service
            }

            else -> throw IllegalArgumentException("Missing service parameter from Manifest")
        }
    }

    private fun processCommand(input: String): Pair<Command<*>, List<String>>? {
        val tokens = input.trim().split(Regex("[\\s]+"))
        var tokenCount = tokens.size
        var candidateCount: Int
        var matcher: Matcher
        var solution: String? = null
        var candidate: String?
        do {
            candidate = tokens.take(tokenCount).joinToString(" ")

            val candidatePattern = Pattern.compile("^${Pattern.quote(candidate)}([^\\s]*)")
            candidateCount = commands.keys.filter { key ->
                matcher = candidatePattern.matcher(key)
                if (matcher.find()) {
                    solution = key
                    true
                } else {
                    false
                }
            }.size
            tokenCount--
        } while (candidateCount != 1 && tokenCount > 0)

        return if (candidateCount > 1) {
            val exactMatch = commands.keys.firstOrNull { candidate == it }
            if (exactMatch != null) {
                val args =
                    input.removePrefix(exactMatch).trim().split(Regex("[\\s]+")).filter { it.isNotBlank() }
                commands[exactMatch]!! to args
            } else {
                println("No exact match found. Type in more of the command.")
                null
            }
        } else if (candidateCount == 1) {
            val args =
                input.removePrefix(candidate!!).trim().split(Regex("[\\s]+")).filter { it.isNotBlank() }
            commands[solution]!! to args
        } else {
            null
        }
    }

    private fun buildCommandSet(serviceName: String, vararg useCases: UseCase) {
        val allCommands = mutableMapOf<String, Command<*>>()
        useCases.forEach {
            val commands = it.commands.map { it.command to it }.associate { it }
            val intersection = commands.keys.intersect(allCommands.keys)
            if (intersection.isNotEmpty()) {
                throw IllegalArgumentException("Error registering interface provider: '$serviceName'. Conflicting commands: ${intersection.joinToString()}")
            }
            allCommands.putAll(commands)
        }
        allCommands.apply {
            put(HELP_COMMAND_CODE, command(command = HELP_COMMAND_CODE, description = "Prints this menu") {
                printInfo(serviceName, allCommands)
            })
            put(EXIT_COMMAND_CODE, command(EXIT_COMMAND_CODE, "Exit this app") {} as Command<*>)
        }
        this.commands = allCommands
    }

    private fun executeCommand(command: Command<*>, reader: BufferedReader, args: List<String>) {
        val requiredArgCount = command.requiredArguments.size
        val completeArgs = if (args.size < requiredArgCount) {
            (args + (args.size until requiredArgCount).map { i ->
                print(command.requiredArguments[i] + ": ")
                reader.readLine()
            })
        } else {
            args
        }
        executeCommand(command, completeArgs)
    }

    private fun executeCommand(command: Command<*>, args: List<String>) {
        val paramTypes: Array<Type> =
            ((command.javaClass.genericSuperclass as ParameterizedType).actualTypeArguments[0] as? ParameterizedType)?.actualTypeArguments
                ?: arrayOf(object : TypeToken<Unit>() {}.type)
        val adaptedArgs = args.mapIndexed { index, item -> toType(item, paramTypes[index]) }
        val finalArgs = when (adaptedArgs.size) {
            0 -> Unit
            1 -> Single(adaptedArgs[0])
            2 -> Pair(adaptedArgs[0], adaptedArgs[1])
            3 -> Triple(adaptedArgs[0], adaptedArgs[1], adaptedArgs[2])
            else -> println("Argument count ${adaptedArgs.size} not supported")
        }
        runCatching {
            val exec: (Any) -> Unit = command.exec as ((Any) -> Unit)
            exec.invoke(finalArgs)
        }.exceptionOrNull()?.let {
            println(it.message)
        }
    }
}

private const val EXIT_COMMAND_CODE = "exit"
private const val HELP_COMMAND_CODE = "help"