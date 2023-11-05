import app.gaborbiro.flathunt.Dependencies

plugins {
    kotlin("jvm")
}

kotlin {
    jvmToolchain(Dependencies.jdk)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":base"))
    implementation(project(":service:domain"))
    implementation(project(":data:domain"))
    implementation(project(":repo:domain"))
    implementation(project(":directions"))

    implementation(Dependencies.Koin.core)
}