package app.gaborbiro.flathunt

import app.gaborbiro.flathunt.usecase.base.Command
import app.gaborbiro.flathunt.usecase.base.UseCase
import app.gaborbiro.flathunt.usecase.base.command

class CommandSetBuilder(
    private val serviceConfig: String,
    private val useCases: Set<UseCase>
) {

    companion object {
        const val EXIT_COMMAND_CODE = "exit"
        const val HELP_COMMAND_CODE = "help"
    }

    fun buildCommandSet(): CommandSet {
        val allCommands = mutableMapOf<String, Command<*>>()
        useCases.forEach {
            val commands = it.commands.map { it.command to it }.associate { it }
            val intersection = commands.keys.intersect(allCommands.keys)
            if (intersection.isNotEmpty()) {
                throw IllegalArgumentException("Error registering interface provider: '$serviceConfig'. Conflicting commands: ${intersection.joinToString()}")
            }
            allCommands.putAll(commands)
        }
        allCommands.apply {
            put(
                HELP_COMMAND_CODE,
                command(command = HELP_COMMAND_CODE, description = "Prints this menu") {
                    printInfo(serviceConfig, allCommands)
                }
            )
            put(
                EXIT_COMMAND_CODE,
                command(EXIT_COMMAND_CODE, "Exit this app") {} as Command<*>
            )
        }
        return CommandSet(allCommands)
    }

    private fun printInfo(serviceConfig: String, commands: Map<String, Command<*>>) {
        println("\nAvailable commands:")
        println(commands.mapKeys { "- ${it.key}" }.mapValues { it.value.description }.prettyPrint())
        println()
        println("Service:\t\t$serviceConfig")
    }
}