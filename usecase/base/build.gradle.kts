import dev.gaborbiro.investments.Dependencies

plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":base"))
    implementation(project(":service:domain"))
    implementation(project(":data:domain"))
    implementation(project(":google"))

    implementation(Dependencies.Koin.core)
}