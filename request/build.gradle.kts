import app.gaborbiro.flathunt.Dependencies

plugins {
    kotlin("jvm")
    id("com.google.devtools.ksp")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(Dependencies.Koin.core)
    implementation(Dependencies.Koin.annotations)
    implementation(Dependencies.OkHttp.okhttp)
    ksp(Dependencies.Koin.ksp)

    implementation(project(":request:domain"))
    implementation(project(":base"))
}
