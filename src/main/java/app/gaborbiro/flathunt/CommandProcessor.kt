package app.gaborbiro.flathunt

import app.gaborbiro.flathunt.usecase.base.Command
import app.gaborbiro.flathunt.usecase.base.Single
import com.google.gson.reflect.TypeToken
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

class CommandProcessor(private val getNextLine: () -> String) {

    fun execute(command: CommandWithArgs) {
        val (command, args) = command
        val requiredArgCount = command.requiredArguments.size
        val completeArgs = if (args.size < requiredArgCount) {
            (args + (args.size until requiredArgCount).map { i ->
                print(command.requiredArguments[i] + ": ")
                getNextLine()
            })
        } else {
            args
        }
        doExecute(command, completeArgs)
    }

    fun doExecute(command: Command<*>, args: List<String>) {
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