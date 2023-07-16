plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":data"))
    implementation(project(":service"))
    implementation(project(":base"))

    // https://mvnrepository.com/artifact/org.seleniumhq.selenium/selenium-java
    implementation("org.seleniumhq.selenium:selenium-java:4.10.0")

    implementation("com.google.code.gson:gson:2.8.9")
}
