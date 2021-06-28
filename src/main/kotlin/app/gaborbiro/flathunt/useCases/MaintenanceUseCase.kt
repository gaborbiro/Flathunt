package app.gaborbiro.flathunt.useCases

import app.gaborbiro.flathunt.GlobalVariables
import app.gaborbiro.flathunt.Single
import app.gaborbiro.flathunt.command
import app.gaborbiro.flathunt.data.Store
import java.io.File
import java.io.PrintWriter

class MaintenanceUseCase(private val store: Store) : UseCase {

    override fun getCommands() = listOf(
        command(command = "clear session", description = "Deletes session cookies (will re-login on next launch)") {
            store.clearCookies()
        },
        command(
            command = "export",
            description = "Exports saved properties to the specified path",
            argumentName = "path",
            exec = ::backup
        ),
        command(
            command = "backup",
            description = "Exports saved properties to the specified path",
            argumentName = "path",
            exec = ::backup
        ),
        command<String>(
            command = "import",
            description = "Imports properties from specified path. Warning: this will irrevocably override all your data. " +
                    "Make sure to back your data up first by using the 'export' command.",
            argumentName = "path",
        ) { (path) ->
            try {
                val json = File(path).bufferedReader().use { it.readText() }
                store.saveJsonProperties(json)
                println("Imported ${store.getProperties().size} properties")
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        },
        command(command = "cls", description = "clears console") {
            ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor()
        },
        command(command = "regedit", description = "Opens Windows regedit to Flathunt") {
            Runtime.getRuntime()
                .exec("REG ADD \"HKEY_CURRENT_USER\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Applets\\Regedit\" /v \"LastKey\" /d \"HKEY_CURRENT_USER\\SOFTWARE\\JavaSoft\\Prefs\\app\\gaborbiro\\flathunt\" /f")
                .waitFor()
            Runtime.getRuntime().exec("cmd /c regedit")
        },
        command("safe mode on", "No messages or properties will be altered (resets on app close)") {
            GlobalVariables.safeMode = true
            println(
                "Safe mode enabled. No web content will be altered. Warning: 'search' command might not work with " +
                        "safe mode, because it relies on invalid properties being marked as unsuitable/hidden."
            )
        },
        command("safe mode off (default)", "Messages or properties will be labeled/marked as needed") {
            GlobalVariables.safeMode = false
            println("Safe mode disabled. Messages or properties will be labeled/marked as needed.")
        }
    )

    private fun backup(path: Single<String>) {
        store.getJsonProperties()?.let { json ->
            try {
                PrintWriter(path.first).use { it.print(json) }
                println("${json.length} bytes backed up")
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        } ?: run {
            println("Nothing to back up")
        }
    }
}