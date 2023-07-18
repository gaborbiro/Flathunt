import dev.gaborbiro.investments.Dependencies

plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":data:domain"))
    implementation(project(":service"))
    implementation(project(":service:domain"))
    implementation(project(":base"))

    implementation(Dependencies.Selenium.java)
}
