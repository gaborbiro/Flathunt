import app.gaborbiro.flathunt.Dependencies

plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

dependencies {
    api(project(":usecase:base"))
    implementation(project(":base"))
    implementation(project(":data:domain"))
    implementation(project(":repo:domain"))
    implementation(project(":google"))

    implementation(Dependencies.Koin.core)
}