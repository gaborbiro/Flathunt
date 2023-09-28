plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":base"))
    implementation(project(":request:domain"))

    implementation("com.google.code.gson:gson:2.8.9")
}
