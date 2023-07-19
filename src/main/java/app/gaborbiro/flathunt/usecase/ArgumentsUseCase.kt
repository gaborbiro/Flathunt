package app.gaborbiro.flathunt.usecase

internal class ArgumentsUseCase(private val getNextInput: () -> String) {

    fun execute(command: CommandWithArgs): CommandWithArgs {
        val (command, args) = command
        val requiredArgCount = command.requiredArguments.size
        val completeArgs = if (args.size < requiredArgCount) {
            (args + (args.size until requiredArgCount).map { i ->
                print(command.requiredArguments[i] + ": ")
                getNextInput()
            })
        } else {
            args
        }
        return CommandWithArgs(command, completeArgs)
    }
}