import dev.gaborbiro.investments.Dependencies

plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(Dependencies.Selenium.java)
    implementation(Dependencies.OkHttp.okhttp)
}
