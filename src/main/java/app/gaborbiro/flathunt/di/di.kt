package app.gaborbiro.flathunt.di

import app.gaborbiro.flathunt.compileTimeConstant.Constants
import app.gaborbiro.flathunt.console.ConsoleWriter
import app.gaborbiro.flathunt.console.ConsoleWriterFactory
import app.gaborbiro.flathunt.console.di.ConsoleModule
import app.gaborbiro.flathunt.criteria.EXP
import app.gaborbiro.flathunt.criteria.TIAGO
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
import org.openqa.selenium.remote.*
import org.openqa.selenium.remote.codec.w3c.W3CHttpCommandCodec
import org.openqa.selenium.remote.codec.w3c.W3CHttpResponseCodec
import java.io.File
import java.io.IOException
import java.lang.reflect.Field
import java.net.URL
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
                        "no-sandbox",
                        "disable-infobars",
                        "disable-dev-shm-usage",
                        "disable-browser-side-navigation",
                        "enable-automation",
                        "ignore-ssl-errors=yes",
                        "ignore-certificate-errors",

                        )
                    setCapability(CapabilityType.UNHANDLED_PROMPT_BEHAVIOUR, UnexpectedAlertBehaviour.DISMISS)
                    addExtensions(File("EditThisCookie.crx"))
                }
            )
//            val executor = (driver.commandExecutor as HttpCommandExecutor)
//            println("Session: ${driver.sessionId} ${executor.addressOfRemoteServer}")
//            val driver2 = createDriverFromSession(driver.sessionId, executor.addressOfRemoteServer)
//            driver2["https://www.idealista.pt/en"]

//            val driver3 = RemoteWebDriver
//                .builder()
//                .address(URL("http://127.0.0.1:9222"))
//                .addAlternative(DesiredCapabilities())
//                .build()
//            driver3["http://www.google.com"]

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

private fun createDriverFromSession(sessionId: SessionId, command_executor: URL?): RemoteWebDriver {
    val executor: CommandExecutor = object : HttpCommandExecutor(command_executor) {
        @Throws(IOException::class)
        override fun execute(command: Command): Response? {
            return if (command.name === "newSession") {
                val response = Response()
                response.sessionId = sessionId.toString()
                response.status = 0
                response.value = emptyMap<String, String>()

                try {
                    val commandCodec: Field = this.javaClass.superclass.getDeclaredField("commandCodec")
                    commandCodec.setAccessible(true)
                    commandCodec.set(this, W3CHttpCommandCodec())
                    val responseCodec: Field = this.javaClass.superclass.getDeclaredField("responseCodec")
                    responseCodec.setAccessible(true)
                    responseCodec.set(this, W3CHttpResponseCodec())
                } catch (e: NoSuchFieldException) {
                    e.printStackTrace()
                } catch (e: IllegalAccessException) {
                    e.printStackTrace()
                }
                response
            } else {
                super.execute(command)
            }
        }
    }
    return RemoteWebDriver(executor, DesiredCapabilities())
}

private fun getValidationCriteria(serviceConfig: String): ValidationCriteria {
    return when {
        serviceConfig.endsWith(Constants.exp) -> EXP
        serviceConfig.endsWith(Constants.tiago) -> TIAGO
        else -> throw IllegalArgumentException("Unknown service configuration $serviceConfig")
    }
}