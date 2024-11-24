package app.gaborbiro.flathunt

@Suppress("ConstPropertyName")
object Dependencies {

    object Google {
        const val gson = "com.google.code.gson:gson:2.11.0"
        const val ksp = "com.google.devtools.ksp:1.9.20-1.0.14"
    }

    object Selenium {
        const val java = "org.seleniumhq.selenium:selenium-java:4.26.0"
    }

    object slf4j {
        const val api = "org.slf4j:slf4j-api:2.0.+"
        const val jdk14 = "org.slf4j:slf4j-jdk14:2.0.+"
    }

    object OkHttp {
        const val okhttp = "com.squareup.okhttp3:okhttp:4.12.0"
    }

    object Jcabi {
        const val mainfests = "com.jcabi:jcabi-manifests:2.1.0"
    }

    object Koin {
        const val core = "io.insert-koin:koin-core:4.0.0"
        const val annotations = "io.insert-koin:koin-annotations:1.4.0"
        const val ksp = "io.insert-koin:koin-ksp-compiler:1.4.0"
    }
}