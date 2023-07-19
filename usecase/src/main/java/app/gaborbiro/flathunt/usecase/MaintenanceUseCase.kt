package app.gaborbiro.flathunt.usecase

import app.gaborbiro.flathunt.GlobalVariables
import app.gaborbiro.flathunt.repo.domain.MaintenanceRepository
import app.gaborbiro.flathunt.usecase.base.Command
import app.gaborbiro.flathunt.usecase.base.UseCase
import app.gaborbiro.flathunt.usecase.base.command
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class MaintenanceUseCase : UseCase, KoinComponent {

    private val repo: MaintenanceRepository by inject()

    override val commands: List<Command<*>>
        get() = listOf(
            command(
                command = "clear session",
                description = "Deletes session cookies (will re-login on next launch)",
                exec = { repo.clearCookies() }
            ),
            command<String>(
                command = "export",
                description = "Exports saved properties to the specified path",
                argumentName = "path",
                exec = {
                    repo.backup(it.first)
                }
            ),
            command<String>(
                command = "backup",
                description = "Exports saved properties to the specified path",
                argumentName = "path",
                exec = {
                    repo.backup(it.first)
                }
            ),
            command<String>(
                command = "import",
                description = "Imports properties from specified path. Warning: this will irrevocably override all your data. " +
                        "Make sure to back your data up first by using the 'export' command.",
                argumentName = "path",
            ) { (path) ->
                try {
                    val size = repo.restore(path)
                    println("Imported $size properties")
                } catch (t: Throwable) {
                    t.printStackTrace()
                }
            },
            command(
                command = "cls",
                description = "clears console"
            ) {
                ProcessBuilder("cmd", "/c", "cls")
                    .inheritIO()
                    .start()
                    .waitFor()
            },
            command(
                command = "regedit",
                description = "Opens Windows regedit to Flathunt"
            ) {
                Runtime.getRuntime()
                    .exec("REG ADD \"HKEY_CURRENT_USER\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Applets\\Regedit\" /v \"LastKey\" /d \"HKEY_CURRENT_USER\\SOFTWARE\\JavaSoft\\Prefs\\app\\gaborbiro\\flathunt\" /f")
                    .waitFor()
                Runtime.getRuntime().exec("cmd /c regedit")
            },
            command(
                command = "safe mode on",
                description = "No messages or properties will be altered (resets on app close)"
            ) {
                GlobalVariables.safeMode = true
                println(
                    "Safe mode enabled. No web content will be altered. Warning: 'search' command might not work with " +
                            "safe mode, because it relies on invalid properties being marked as unsuitable/hidden."
                )
            },
            command(
                command = "safe mode off (default)",
                description = "Messages or properties will be labeled/marked as needed"
            ) {
                GlobalVariables.safeMode = false
                println("Safe mode disabled. Messages or properties will be labeled/marked as needed.")
            }
        )
}