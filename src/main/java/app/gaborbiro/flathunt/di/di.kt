package app.gaborbiro.flathunt.di

import app.gaborbiro.flathunt.compileTimeConstant.Constants
import app.gaborbiro.flathunt.console.ConsoleWriter
import app.gaborbiro.flathunt.console.ConsoleWriterFactory
import app.gaborbiro.flathunt.console.di.ConsoleModule
import app.gaborbiro.flathunt.criteria.EXP
import app.gaborbiro.flathunt.criteria.ValidationCriteria
import app.gaborbiro.flathunt.data.di.DataModule
import app.gaborbiro.flathunt.directions.di.DirectionsModule
import app.gaborbiro.flathunt.repo.di.RepoModule
import app.gaborbiro.flathunt.request.di.RequestModule
import app.gaborbiro.flathunt.service.di.ServiceModule
import app.gaborbiro.flathunt.service.domain.UtilsService
import app.gaborbiro.flathunt.service.domain.WebService
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.logger.PrintLogger
import org.koin.core.qualifier.StringQualifier
import org.koin.dsl.module
import org.koin.ksp.generated.module
import org.openqa.selenium.UnexpectedAlertBehaviour
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.remote.CapabilityType
import java.io.File
import java.nio.file.Paths


fun setupKoin(serviceConfig: String): KoinApplication {

    val (serviceName, criteria) = serviceConfig.split("-")

    val appModule = module {
        single<String>(StringQualifier("serviceName")) { serviceName }
        single<String>(StringQualifier("criteria")) { criteria }

        single<ValidationCriteria> { getValidationCriteria(serviceConfig) }
    }

    val app = startKoin {
        modules(
            appModule,
            DataModule().module,
            RepoModule().module,
            RequestModule().module,
            ConsoleModule().module,
            ServiceModule().module,
            DirectionsModule().module,
        )
    }
    val serviceModule = module {
        single<WebDriver> {
//            RemoteWebDriver((driver.commandExecutor as ChromeDriverCommandExecutor).addressOfRemoteServer, DesiredCapabilities())

            System.setProperty("webdriver.chrome.driver", Paths.get("chromedriver.exe").toString())
            val driver = ChromeDriver(
                ChromeOptions().apply {
                    // https://peter.sh/experiments/chromium-command-line-switches/
                    // start-maximized
                    // window-position=0,0", "window-size=1,1
                    addArguments(
                        "start-maximized",
                        "disable-gpu",
//                        "headless",
                        "debuggerAddress=localhost:9222",
                        "disable-infobars"
                    )
                    setCapability(CapabilityType.UNHANDLED_PROMPT_BEHAVIOUR, UnexpectedAlertBehaviour.DISMISS)
                    addExtensions(File("EditThisCookie.crx"))
                }
            )
            println("Session Id: ${driver.sessionId}")
            driver
        }
    }
    app.modules(serviceModule)
    val serviceNameModule = module {
        val serviceName: String = app.koin.get(StringQualifier("serviceName"))
        single<WebService> {
            app.koin.get(StringQualifier(serviceName + "_web"))
        }
        single<UtilsService> {
            app.koin.get(StringQualifier(serviceName + "_utils"))
        }
    }
    app.modules(serviceNameModule)
    val consoleModule = module {
        single<ConsoleWriter> {
            app.koin.get<ConsoleWriterFactory>().getConsoleWriter()
        }
    }
    app.modules(consoleModule)
    app.logger(PrintLogger())
    return app
}

private fun getValidationCriteria(serviceConfig: String): ValidationCriteria {
    return when (serviceConfig) {
        Constants.`idealista-exp`, Constants.`spareroom-exp`, Constants.`rightmove-exp`, Constants.`zoopla-exp` -> EXP
        else -> throw IllegalArgumentException("Unknown service configuration $serviceConfig")
    }
}