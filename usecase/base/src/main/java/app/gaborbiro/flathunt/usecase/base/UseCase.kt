package app.gaborbiro.flathunt.usecase.base

import app.gaborbiro.flathunt.prettyPrint

interface UseCase {
    val commands: List<Command<*>>
}

abstract class Command<T>(
    /**
     * The thing the user has to type in to make stuff happen
     */
    val command: String,
    /**
     * Print when user runs 'help'
     */
    val description: String,
    /**
     * Print when user runs 'help'
     */
    val requiredArguments: List<String> = emptyList(),

    val exec: (args: T) -> Unit,
)

fun command(
    command: String,
    description: String,
    exec: (Unit) -> Unit
) = object : Command<Unit>(
    command,
    description,
    emptyList(),
    exec
) {}

inline fun <reified A> command(
    command: String,
    description: String,
    argumentName: String,
    noinline exec: (Single<A>) -> Unit,
) = object : Command<Single<A>>(
    command,
    description,
    listOf(argumentName),
    exec
) {}

data class Single<out A>(val first: A)

inline fun <reified A, reified B> command(
    command: String,
    description: String,
    argumentName1: String,
    argumentName2: String,
    noinline exec: (Pair<A, B>) -> Unit,
) = object : Command<Pair<A, B>>(
    command,
    description,
    listOf(argumentName1, argumentName2),
    exec
) {}

inline fun <reified A, reified B, reified C> command(
    command: String,
    description: String,
    argumentName1: String,
    argumentName2: String,
    argumentName3: String,
    noinline exec: (Triple<A, B, C>) -> Unit,
) = object : Command<Triple<A, B, C>>(
    command,
    description,
    listOf(argumentName1, argumentName2, argumentName3),
    exec
) {}

fun printInfo(serviceName: String, commands: Map<String, Command<*>>) {
    println("\nAvailable commands:")
    println(commands.mapKeys { "- ${it.key}" }.mapValues { it.value.description }.prettyPrint())
    println()
    println("Service:\t\t$serviceName")
}
