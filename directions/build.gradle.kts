import app.gaborbiro.flathunt.Dependencies

plugins {
    kotlin("jvm")
    id("com.google.devtools.ksp")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":base"))
    implementation(project(":request:domain"))
    implementation(project(":console:domain"))

    implementation(Dependencies.Koin.core)
    implementation(Dependencies.Koin.annotations)
    ksp(Dependencies.Koin.ksp)

    implementation(Dependencies.Google.gson)
}
