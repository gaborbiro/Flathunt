import dev.gaborbiro.investments.Dependencies

plugins {
    kotlin("jvm")
    id("com.google.devtools.ksp")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":base"))
    implementation(project(":repo:domain"))
    implementation(project(":service:domain"))
    implementation(project(":data:domain"))
    implementation(project(":google"))

    implementation(Dependencies.Google.gson)
    implementation(Dependencies.Selenium.java)

    implementation(Dependencies.Koin.core)
    implementation(Dependencies.Koin.annotations)
    ksp(Dependencies.Koin.ksp)
}
