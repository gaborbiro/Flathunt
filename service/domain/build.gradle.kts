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
    implementation(project(":repo:domain"))

    implementation(Dependencies.Selenium.java)
}
