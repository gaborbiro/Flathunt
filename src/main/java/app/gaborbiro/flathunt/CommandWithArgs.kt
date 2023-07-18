package app.gaborbiro.flathunt

import app.gaborbiro.flathunt.usecase.base.Command

data class CommandWithArgs(
    val command: Command<*>,
    val args: List<String>
)