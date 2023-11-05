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
    implementation(Dependencies.Selenium.java)
}
