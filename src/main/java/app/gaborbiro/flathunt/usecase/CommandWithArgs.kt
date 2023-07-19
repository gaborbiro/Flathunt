package app.gaborbiro.flathunt.usecase

import app.gaborbiro.flathunt.usecase.base.Command

data class CommandWithArgs(
    val command: Command<*>,
    val args: List<String>
)