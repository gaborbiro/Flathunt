package app.gaborbiro.flathunt

object Dependencies {

    object Google {
        const val gson = "com.google.code.gson:gson:2.10.1"
        const val ksp = "com.google.devtools.ksp:1.9.20-1.0.14"
    }

    object Selenium {
        const val java = "org.seleniumhq.selenium:selenium-java:4.26.0"
    }

    object slf4j {
        const val api = "org.slf4j:slf4j-api:1.7.+"
        const val jdk14 = "org.slf4j:slf4j-jdk14:1.7.+"
    }

    object OkHttp {
        const val okhttp = "com.squareup.okhttp3:okhttp:4.11.0"
    }

    object Jcabi {
        const val mainfests = "com.jcabi:jcabi-manifests:1.1"
    }

    object Koin {
        const val core = "io.insert-koin:koin-core:3.1.2"
        const val annotations = "io.insert-koin:koin-annotations:1.2.2"
        const val ksp = "io.insert-koin:koin-ksp-compiler:1.2.2"
    }

    object CompileTimeConstants {
        const val version = "1.0.5"
        const val lib = "com.anatawa12.compile-time-constant"
    }
}