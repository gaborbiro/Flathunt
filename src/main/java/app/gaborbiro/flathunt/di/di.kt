package app.gaborbiro.flathunt.di

import app.gaborbiro.flathunt.EXP
import app.gaborbiro.flathunt.ValidationCriteria
import app.gaborbiro.flathunt.compileTimeConstant.Constants
import app.gaborbiro.flathunt.data.di.DataModule
import app.gaborbiro.flathunt.repo.di.RepoModule
import app.gaborbiro.flathunt.service.di.ServiceModule
import app.gaborbiro.flathunt.service.domain.Service
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.qualifier.StringQualifier
import org.koin.dsl.module
import org.koin.ksp.generated.module


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
            ServiceModule().module
        )
    }
    val serviceModule = module {
        single<Service> {
            val serviceName: String = app.koin.get(StringQualifier("serviceName"))
            app.koin.get(StringQualifier(serviceName))
        }
    }
    app.modules(serviceModule)

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