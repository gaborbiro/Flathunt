package app.gaborbiro.flathunt.usecase

import app.gaborbiro.flathunt.GlobalVariables
import app.gaborbiro.flathunt.console.ConsoleWriter
import app.gaborbiro.flathunt.repo.domain.MaintenanceRepository
import app.gaborbiro.flathunt.usecase.base.Command
import app.gaborbiro.flathunt.usecase.base.UseCase
import app.gaborbiro.flathunt.usecase.base.command
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class MaintenanceUseCase : UseCase, KoinComponent {

    private val repo: MaintenanceRepository by inject()
    private val console: ConsoleWriter by inject()

    override val commands: List<Command<*>>
        get() = listOf(
            command(
                command = "clear browser cookies",
                description = "Delete cookies from current browser (if open)",
                exec = { repo.clearBrowserCookies() }
            ),
            command(
                command = "clear stored cookies",
                description = "Delete cookies from database (will re-login on next launch)",
                exec = { repo.clearStoredCookies() }
            ),
            command(
                command = "import cookies",
                description = "Add cookies from cookie-override.txt to registry (override) and also browser if available",
                exec = { repo.importCookies("cookie-override.txt") }
            ),
            command(
                command = "save cookies",
                description = "Override currently stored cookies with the ones from the browser. Requires open browser.",
                exec = { repo.saveCookies() }
            ),
            command<String>(
                command = "export",
                description = "Export saved properties to the specified path",
                argumentDescription = "filepath",
                exec = { (filepath) ->
                    repo.backup(filepath)
                }
            ),
            command<String>(
                command = "import",
                description = "Import properties from specified path. Warning: this will irrevocably override all your data. " +
                        "Make sure to back your data up first by using the 'export' command.",
                argumentDescription = "filepath",
            ) { (filepath) ->
                try {
                    val size = repo.restore(filepath)
                    console.d("Imported $size properties")
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
                description = "Open Windows regedit to Flathunt"
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
                console.d(
                    "Safe mode enabled. No web content will be altered. Warning: 'search' command might not work with " +
                            "safe mode, because it relies on invalid properties being marked as unsuitable/hidden."
                )
            },
            command(
                command = "safe mode off (default)",
                description = "Messages or properties will be labeled/marked unsuitable as needed"
            ) {
                GlobalVariables.safeMode = false
                console.d("Safe mode disabled. Messages or properties will be labeled/marked unsuitable as needed.")
            },
            command(
                command = "root",
                description = "open website"
            ) {
                repo.openRoot()
            }
        )
}