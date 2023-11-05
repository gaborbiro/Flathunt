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
    implementation(project(":data:domain"))
    implementation(project(":repo:domain"))

    implementation(Dependencies.Selenium.java)
}
