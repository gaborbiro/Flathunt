package app.gaborbiro.flathunt.usecase

import app.gaborbiro.flathunt.console.ConsoleWriter
import app.gaborbiro.flathunt.toType
import app.gaborbiro.flathunt.usecase.base.Single
import com.google.gson.reflect.TypeToken
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

class CommandUseCase(private val console: ConsoleWriter) {

    fun execute(command: CommandWithArgs) {
        val (command, args) = command
        val paramTypes: Array<Type> =
            ((command.javaClass.genericSuperclass as ParameterizedType).actualTypeArguments[0] as? ParameterizedType)?.actualTypeArguments
                ?: arrayOf(object : TypeToken<Unit>() {}.type)
        val adaptedArgs = args.mapIndexed { index, item -> toType(item, paramTypes[index]) }
        val finalArgs = when (adaptedArgs.size) {
            0 -> Unit
            1 -> Single(adaptedArgs[0])
            2 -> Pair(adaptedArgs[0], adaptedArgs[1])
            3 -> Triple(adaptedArgs[0], adaptedArgs[1], adaptedArgs[2])
            else -> console.e("Argument count ${adaptedArgs.size} not supported")
        }
        runCatching {
            val exec: (Any) -> Unit = command.exec as ((Any) -> Unit)
            exec.invoke(finalArgs)
        }.exceptionOrNull()?.let {
            console.e(it.message ?: "Unknown error")
        }
    }
}