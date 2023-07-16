plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":google"))
    implementation(project(":base"))

    implementation("com.google.code.gson:gson:2.8.9")

    // https://mvnrepository.com/artifact/org.seleniumhq.selenium/selenium-java
    implementation("org.seleniumhq.selenium:selenium-java:4.10.0")
}
