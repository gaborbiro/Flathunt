package app.gaborbiro.flathunt.useCases

import app.gaborbiro.flathunt.Command

interface UseCase {
    fun getCommands(): List<Command<*>>
}