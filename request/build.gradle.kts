import app.gaborbiro.flathunt.Dependencies

plugins {
    kotlin("jvm")
    id("com.google.devtools.ksp")
}

kotlin {
    jvmToolchain(Dependencies.jdk)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(Dependencies.Koin.core)
    implementation(Dependencies.Koin.annotations)
    ksp(Dependencies.Koin.ksp)
    implementation(Dependencies.OkHttp.okhttp)

    implementation(project(":console:domain"))
    implementation(project(":request:domain"))
    implementation(project(":base"))
}
