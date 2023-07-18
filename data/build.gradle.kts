import dev.gaborbiro.investments.Dependencies

plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":base"))
    implementation(project(":data:domain"))

    implementation(Dependencies.Google.gson)
    implementation(Dependencies.Selenium.java)
}
