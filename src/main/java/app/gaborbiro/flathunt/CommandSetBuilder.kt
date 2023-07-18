package app.gaborbiro.flathunt

import app.gaborbiro.flathunt.usecase.base.Command
import app.gaborbiro.flathunt.usecase.base.UseCase
import app.gaborbiro.flathunt.usecase.base.command
import app.gaborbiro.flathunt.usecase.base.printInfo

class CommandSetBuilder(
    private val serviceName: String,
    private val useCases: Set<UseCase>
) {

    companion object {
        private const val EXIT_COMMAND_CODE = "exit"
        private const val HELP_COMMAND_CODE = "help"
    }

    fun buildCommandSet(): Map<String, Command<*>> {
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
        return allCommands
    }
}