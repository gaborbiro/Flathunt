plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":base"))
    implementation(project(":service"))
    implementation(project(":data:domain"))
    implementation(project(":google"))
}