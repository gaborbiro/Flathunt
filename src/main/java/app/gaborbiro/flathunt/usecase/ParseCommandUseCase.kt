package app.gaborbiro.flathunt.usecase

import app.gaborbiro.flathunt.CommandSet
import app.gaborbiro.flathunt.console.ConsoleWriter
import java.util.regex.Matcher
import java.util.regex.Pattern

class ParseCommandUseCase(
    private val commands: CommandSet,
    private val console: ConsoleWriter
) {

    fun execute(input: String): CommandWithArgs? {
        val tokens = input.trim().split(Regex("[\\s]+"))
        var tokenCount = tokens.size
        var candidateCount: Int
        var matcher: Matcher
        var solution: String? = null
        var candidate: String?
        do {
            candidate = tokens.take(tokenCount).joinToString(" ")

            val candidatePattern = Pattern.compile("^${Pattern.quote(candidate)}([^\\s]*)")
            candidateCount = commands.commands.keys.filter { key ->
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
            val exactMatch = commands.commands.keys.firstOrNull { candidate == it }
            if (exactMatch != null) {
                val args =
                    input.removePrefix(exactMatch).trim().split(Regex("[\\s]+")).filter { it.isNotBlank() }
                CommandWithArgs(commands.commands[exactMatch]!!, args)
            } else {
                console.e("No exact match found. Type in more of the command.")
                null
            }
        } else if (candidateCount == 1) {
            val args =
                input.removePrefix(candidate!!).trim().split(Regex("[\\s]+")).filter { it.isNotBlank() }
            CommandWithArgs(commands.commands[solution]!!, args)
        } else {
            null
        }
    }
}