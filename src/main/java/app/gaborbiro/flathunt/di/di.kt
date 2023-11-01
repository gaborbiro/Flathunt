package app.gaborbiro.flathunt.di

import app.gaborbiro.flathunt.EXP
import app.gaborbiro.flathunt.criteria.ValidationCriteria
import app.gaborbiro.flathunt.compileTimeConstant.Constants
import app.gaborbiro.flathunt.console.ConsoleWriter
import app.gaborbiro.flathunt.console.ConsoleWriterFactory
import app.gaborbiro.flathunt.console.di.ConsoleModule
import app.gaborbiro.flathunt.data.di.DataModule
import app.gaborbiro.flathunt.directions.di.DirectionsModule
import app.gaborbiro.flathunt.repo.di.RepoModule
import app.gaborbiro.flathunt.request.di.RequestModule
import app.gaborbiro.flathunt.service.di.ServiceModule
import app.gaborbiro.flathunt.service.domain.UtilsService
import app.gaborbiro.flathunt.service.domain.WebService
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.qualifier.StringQualifier
import org.koin.dsl.module
import org.koin.ksp.generated.module
import org.openqa.selenium.UnexpectedAlertBehaviour
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.remote.CapabilityType
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
            System.setProperty("webdriver.chrome.driver", Paths.get("chromedriver.exe").toString())
            ChromeDriver(
                ChromeOptions().apply {
                    // https://peter.sh/experiments/chromium-command-line-switches/
                    // start-maximized
                    // window-position=0,0", "window-size=1,1
                    addArguments("start-maximized")
                    setCapability(CapabilityType.UNHANDLED_PROMPT_BEHAVIOUR, UnexpectedAlertBehaviour.DISMISS)
                }
            )
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

    return app
}

private fun getValidationCriteria(serviceConfig: String): ValidationCriteria {
    return when (serviceConfig) {
        Constants.`idealista-exp` -> EXP
        Constants.`spareroom-exp` -> EXP
        Constants.`rightmove-exp` -> EXP
        Constants.`zoopla-exp` -> EXP
        else -> throw IllegalArgumentException("Missing service parameter from Manifest")
    }
}