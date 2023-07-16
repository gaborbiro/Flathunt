plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":base"))

    implementation("com.google.code.gson:gson:2.8.9")
}
