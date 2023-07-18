package app.gaborbiro.flathunt

import app.gaborbiro.flathunt.usecase.base.Command

data class CommandSet(
    val commands: Map<String, Command<*>>
)