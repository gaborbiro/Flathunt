import app.gaborbiro.flathunt.Dependencies

plugins {
    kotlin("jvm")
    id("com.google.devtools.ksp")
}

apply(from = "${project.rootDir}/service.gradle")

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":data:domain"))
    implementation(project(":service:domain"))
    implementation(project(":request:domain"))
    implementation(project(":repo:domain"))
    implementation(project(":base"))
    implementation(project(":usecase:base"))

    implementation(Dependencies.Selenium.java)
    implementation(Dependencies.Google.gson)
    implementation(Dependencies.Koin.core)
    implementation(Dependencies.Koin.annotations)
    implementation(Dependencies.OkHttp.okhttp)
    ksp(Dependencies.Koin.ksp)
}
