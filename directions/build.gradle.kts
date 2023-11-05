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
    implementation(project(":base"))
    implementation(project(":request:domain"))

    implementation(Dependencies.Koin.core)
    implementation(Dependencies.Koin.annotations)
    ksp(Dependencies.Koin.ksp)

    implementation(Dependencies.Google.gson)
}
