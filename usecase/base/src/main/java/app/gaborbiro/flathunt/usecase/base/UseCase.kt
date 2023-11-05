package app.gaborbiro.flathunt.usecase.base

interface UseCase {
    val commands: List<Command<*>>
}

abstract class Command<T>(
    /**
     * The phrase the user has to enter in order to run this command.
     * The user can type in only part (beginning) of the phrase as long as it doesn't conflict with any other command.
     */
    val command: String,
    /**
     * Printed when user runs 'help'
     */
    val description: String,
    /**
     * Printed when user runs 'help'
     */
    val requiredArguments: List<String> = emptyList(),

    val exec: (args: T) -> Unit,
) {
    abstract fun argumentsDescription(): String
}

fun command(
    command: String,
    description: String,
    exec: (Unit) -> Unit
) = object : Command<Unit>(
    command,
    description,
    emptyList(),
    exec
) {
    override fun argumentsDescription(): String = ""
}

inline fun <reified A> command(
    command: String,
    description: String,
    argumentDescription: String,
    noinline exec: (Single<A>) -> Unit,
): Command<Single<A>> {
    return object : Command<Single<A>>(
        command,
        description,
        listOf(argumentDescription),
        exec
    ) {
        override fun argumentsDescription(): String = "($argumentDescription: ${A::class.simpleName})"
    }
}

data class Single<out A>(val first: A)

inline fun <reified A, reified B> command(
    command: String,
    description: String,
    argumentName1: String,
    argumentName2: String,
    noinline exec: (Pair<A, B>) -> Unit,
): Command<Pair<A, B>> {
    return object : Command<Pair<A, B>>(
        command,
        description,
        listOf(argumentName1, argumentName2),
        exec
    ) {
        override fun argumentsDescription(): String =
            "($argumentName1: ${A::class.simpleName}, " +
                    "$argumentName2: ${B::class.simpleName})"
    }
}

inline fun <reified A, reified B, reified C> command(
    command: String,
    description: String,
    argumentName1: String,
    argumentName2: String,
    argumentName3: String,
    noinline exec: (Triple<A, B, C>) -> Unit,
): Command<Triple<A, B, C>> {
    return object : Command<Triple<A, B, C>>(
        command,
        description,
        listOf(argumentName1, argumentName2, argumentName3),
        exec
    ) {
        override fun argumentsDescription(): String =
            "($argumentName1: ${A::class.simpleName}, " +
                    "$argumentName2: ${B::class.simpleName}, " +
                    "$argumentName3: ${C::class.simpleName}))"
    }
}
