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
            clearBrowserCookies,
            clearStoredCookies,
            importCookies,
            saveCookies,
            export,
            import,
            cls,
            regedit,
            safeModeOn,
            safeModeOff,
            root
        )

    private val clearBrowserCookies = command(
        command = "clear browser cookies",
        description = "Delete cookies from current browser (if open)",
        exec = { repo.clearBrowserCookies() }
    )

    private val clearStoredCookies = command(
        command = "clear stored cookies",
        description = "Delete cookies from registry (will re-login on next launch)",
        exec = { repo.clearStoredCookies() }
    )

    private val importCookies = command(
        command = "import cookies",
        description = "Add cookies from cookie-override.txt to registry (override) and also browser if available",
        exec = { repo.importCookies("cookie-override.txt") }
    )

    private val saveCookies = command(
        command = "save cookies",
        description = "Override cookies in the registry with the ones from the browser. Requires open browser.",
        exec = { repo.saveCookies() }
    )

    private val applyCookies = command(
        command = "load cookies",
        description = "Add or update browser with cookies saved to the register. Browser must be open to root url",
        exec = { repo.loadCookies() }
    )

    private val export = command<String>(
        command = "export",
        description = "Export saved properties to the specified path",
        argumentName1 = "filepath",
        exec = { (filepath) ->
            repo.backup(filepath)
        }
    )

    private val import = command<String>(
        command = "import",
        description = "Import properties from specified path. Warning: this will irrevocably override all your data. " +
                "Make sure to back your data up first by using the 'export' command.",
        argumentName1 = "filepath",
    ) { (filepath) ->
        try {
            val size = repo.restore(filepath)
            console.d("Imported $size properties")
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    private val cls = command(
        command = "cls",
        description = "clears console"
    ) {
        ProcessBuilder("cmd", "/c", "cls")
            .inheritIO()
            .start()
            .waitFor()
    }

    private val regedit = command(
        command = "regedit",
        description = "Open Windows regedit"
    ) {
        Runtime.getRuntime()
            .exec("REG ADD \"HKEY_CURRENT_USER\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Applets\\Regedit\" /v \"LastKey\" /d \"HKEY_CURRENT_USER\\SOFTWARE\\JavaSoft\\Prefs\\app\\gaborbiro\\flathunt\" /f")
            .waitFor()
        Runtime.getRuntime().exec("cmd /c regedit")
    }

    private val safeModeOn = command(
        command = "safe mode on",
        description = "No messages or properties will be altered (resets on app close)"
    ) {
        GlobalVariables.safeMode = true
        console.d(
            "Safe mode enabled. No web content will be altered. Warning: 'search' command might not work with " +
                    "safe mode, because it relies on invalid properties being marked as unsuitable/hidden."
        )
    }

    private val safeModeOff = command(
        command = "safe mode off (default)",
        description = "Messages or properties will be labeled/marked unsuitable as needed"
    ) {
        GlobalVariables.safeMode = false
        console.d("Safe mode disabled. Messages or properties will be labeled/marked unsuitable as needed.")
    }

    private val root = command(
        command = "root",
        description = "open website"
    ) {
        repo.openRoot()
    }
}